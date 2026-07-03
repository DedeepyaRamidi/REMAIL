package com.remail.mail;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@ConditionalOnProperty(prefix = "remail.gmail", name = "enabled", havingValue = "true")
public class GmailApiInboxSource implements GmailInboxSource {

    private static final Logger log = LoggerFactory.getLogger(GmailApiInboxSource.class);
    private static final String API_BASE_URL = "https://gmail.googleapis.com/gmail/v1";

    private final GmailProperties properties;
    private final RestTemplate restTemplate;
    private volatile TokenState cachedToken;

    @Autowired
    public GmailApiInboxSource(GmailProperties properties, RestTemplateBuilder restTemplateBuilder) {
        this(properties, restTemplateBuilder.build());
    }

    GmailApiInboxSource(GmailProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    void validateConfiguration() {
        if (!hasText(properties.getAccessToken()) && !hasText(properties.getRefreshToken())) {
            throw new IllegalStateException("REMAIL_GMAIL_ACCESS_TOKEN or REMAIL_GMAIL_REFRESH_TOKEN is required when Gmail ingestion is enabled");
        }

        if (hasText(properties.getRefreshToken()) && (!hasText(properties.getClientId()) || !hasText(properties.getClientSecret()))) {
            throw new IllegalStateException("REMAIL_GMAIL_CLIENT_ID and REMAIL_GMAIL_CLIENT_SECRET are required when using a refresh token");
        }
    }

    @Override
    public List<MailIntakeDocument> pollRecentMessages() {
        if (!properties.isEnabled()) {
            return List.of();
        }

        try {
            String userId = effectiveUserId(null);
            return loadDocuments(userId, loadMessageIds(userId, properties.getQuery(), properties.getPollMaxResults()));
        } catch (RestClientException exception) {
            log.warn("Unable to poll Gmail inbox", exception);
            return List.of();
        }
    }

    @Override
    public List<MailIntakeDocument> fetchChangedMessages(String emailAddress, String historyId) {
        if (!properties.isEnabled() || historyId == null || historyId.isBlank()) {
            return List.of();
        }

        try {
            String userId = effectiveUserId(emailAddress);
            LinkedHashSet<String> messageIds = new LinkedHashSet<>();
            String pageToken = null;

            do {
                GmailHistoryResponse response = getHistory(userId, historyId, pageToken);
                if (response != null && response.history() != null) {
                    for (GmailHistoryEntry historyEntry : response.history()) {
                        if (historyEntry.messagesAdded() == null) {
                            continue;
                        }
                        for (GmailMessageAdded added : historyEntry.messagesAdded()) {
                            if (added != null && added.message() != null && hasText(added.message().id())) {
                                messageIds.add(added.message().id());
                            }
                        }
                    }
                }
                pageToken = response == null ? null : response.nextPageToken();
            } while (hasText(pageToken));

            return loadDocuments(userId, messageIds);
        } catch (RestClientException exception) {
            log.warn("Unable to fetch Gmail history for {}", emailAddress, exception);
            return List.of();
        }
    }

    private List<String> loadMessageIds(String userId, String query, int maxResults) {
        LinkedHashSet<String> messageIds = new LinkedHashSet<>();
        String currentPageToken = null;

        do {
            GmailListResponse response = getMessages(userId, query, currentPageToken, maxResults);
            if (response != null && response.messages() != null) {
                response.messages().stream()
                        .filter(Objects::nonNull)
                        .map(GmailMessageSummary::id)
                        .filter(GmailApiInboxSource::hasText)
                        .forEach(messageIds::add);
            }
            currentPageToken = response == null ? null : response.nextPageToken();
        } while (hasText(currentPageToken));

        return new ArrayList<>(messageIds);
    }

    private List<MailIntakeDocument> loadDocuments(String userId, Collection<String> messageIds) {
        List<MailIntakeDocument> documents = new ArrayList<>();
        for (String messageId : messageIds) {
            fetchMessage(userId, messageId).ifPresent(documents::add);
        }
        return documents;
    }

    private Optional<MailIntakeDocument> fetchMessage(String userId, String messageId) {
        try {
            GmailMessageResponse response = get(messageUrl(userId, messageId), GmailMessageResponse.class);
            if (response == null) {
                return Optional.empty();
            }
            return Optional.of(mapDocument(response));
        } catch (RestClientException exception) {
            log.warn("Unable to fetch Gmail message {} for {}", messageId, userId, exception);
            return Optional.empty();
        }
    }

    private GmailListResponse getMessages(String userId, String query, String pageToken, int maxResults) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(API_BASE_URL)
                .path("/users/{userId}/messages")
                .queryParam("labelIds", "INBOX");
        if (hasText(query)) {
            builder.queryParam("q", query);
        }
        if (maxResults > 0) {
            builder.queryParam("maxResults", maxResults);
        }
        if (hasText(pageToken)) {
            builder.queryParam("pageToken", pageToken);
        }
        return get(builder.buildAndExpand(Map.of("userId", userId)).toUriString(), GmailListResponse.class);
    }

    private GmailHistoryResponse getHistory(String userId, String historyId, String pageToken) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(API_BASE_URL)
                .path("/users/{userId}/history")
                .queryParam("startHistoryId", historyId)
                .queryParam("historyTypes", "messageAdded")
                .queryParam("labelId", "INBOX");
        if (hasText(pageToken)) {
            builder.queryParam("pageToken", pageToken);
        }
        if (properties.getHistoryMaxResults() > 0) {
            builder.queryParam("maxResults", properties.getHistoryMaxResults());
        }
        return get(builder.buildAndExpand(Map.of("userId", userId)).toUriString(), GmailHistoryResponse.class);
    }

    private MailIntakeDocument mapDocument(GmailMessageResponse response) {
        GmailMessagePayload payload = response.payload();
        String subject = headerValue(payload, "Subject");
        String from = headerValue(payload, "From");
        String body = extractBody(payload);
        if (!hasText(body)) {
            body = response.snippet() == null ? "" : response.snippet().trim();
        }
        return new MailIntakeDocument(response.id(), subject, from, body);
    }

    private String headerValue(GmailMessagePayload payload, String headerName) {
        if (payload == null || payload.headers() == null) {
            return "";
        }
        for (GmailMessageHeader header : payload.headers()) {
            if (header != null && headerName.equalsIgnoreCase(header.name()) && hasText(header.value())) {
                return header.value().trim();
            }
        }
        return "";
    }

    private String extractBody(GmailMessagePayload payload) {
        if (payload == null) {
            return "";
        }

        String plainText = extractPreferredPart(payload, "text/plain");
        if (hasText(plainText)) {
            return plainText;
        }

        String htmlText = extractPreferredPart(payload, "text/html");
        if (hasText(htmlText)) {
            return htmlText;
        }

        return decodeBody(payload.body());
    }

    private String extractPreferredPart(GmailMessagePayload payload, String mimeType) {
        if (payload == null) {
            return "";
        }

        if (mimeType.equalsIgnoreCase(payload.mimeType())) {
            String direct = decodeBody(payload.body());
            if (hasText(direct)) {
                return direct;
            }
        }

        if (payload.parts() != null) {
            for (GmailMessagePayload part : payload.parts()) {
                String candidate = extractPreferredPart(part, mimeType);
                if (hasText(candidate)) {
                    return candidate;
                }
            }
        }

        return "";
    }

    private String decodeBody(GmailMessageBody body) {
        if (body == null || !hasText(body.data())) {
            return "";
        }

        byte[] decoded = Base64.getUrlDecoder().decode(body.data());
        return new String(decoded, StandardCharsets.UTF_8).trim();
    }

    private String effectiveUserId(String emailAddress) {
        if (hasText(emailAddress)) {
            return emailAddress.trim();
        }
        if (hasText(properties.getUserId())) {
            return properties.getUserId().trim();
        }
        return "me";
    }

    private String messageUrl(String userId, String messageId) {
        return UriComponentsBuilder.fromHttpUrl(API_BASE_URL)
                .path("/users/{userId}/messages/{messageId}")
                .queryParam("format", "full")
                .buildAndExpand(Map.of("userId", userId, "messageId", messageId))
                .toUriString();
    }

    private <T> T get(String uri, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(resolveAccessToken());
        ResponseEntity<T> response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), responseType);
        return response.getBody();
    }

    private String resolveAccessToken() {
        if (hasText(properties.getAccessToken())) {
            return properties.getAccessToken().trim();
        }

        TokenState tokenState = cachedToken;
        if (tokenState != null && tokenState.isValid()) {
            return tokenState.accessToken();
        }

        synchronized (this) {
            tokenState = cachedToken;
            if (tokenState != null && tokenState.isValid()) {
                return tokenState.accessToken();
            }
            TokenState refreshed = refreshAccessToken();
            cachedToken = refreshed;
            return refreshed.accessToken();
        }
    }

    private TokenState refreshAccessToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        form.add("refresh_token", properties.getRefreshToken());
        form.add("grant_type", "refresh_token");

        GmailTokenResponse response = restTemplate.postForObject(properties.getTokenEndpoint(), new HttpEntity<>(form, headers), GmailTokenResponse.class);
        if (response == null || !hasText(response.accessToken())) {
            throw new IllegalStateException("Unable to obtain Gmail access token");
        }

        long expiresAt = Instant.now().plusSeconds(Math.max(60, response.expiresIn())).getEpochSecond();
        return new TokenState(response.accessToken(), expiresAt);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record TokenState(String accessToken, long expiresAtEpochSecond) {

        private boolean isValid() {
            return hasText(accessToken) && Instant.now().getEpochSecond() < expiresAtEpochSecond - 30;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GmailTokenResponse(@JsonProperty("access_token") String accessToken,
                                      @JsonProperty("expires_in") long expiresIn) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GmailListResponse(List<GmailMessageSummary> messages, String nextPageToken) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GmailHistoryResponse(List<GmailHistoryEntry> history, String nextPageToken) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GmailHistoryEntry(List<GmailMessageAdded> messagesAdded) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GmailMessageAdded(GmailMessageSummary message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GmailMessageSummary(String id, String threadId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GmailMessageResponse(String id, String snippet, GmailMessagePayload payload) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GmailMessagePayload(String mimeType, List<GmailMessageHeader> headers, GmailMessageBody body,
                                       List<GmailMessagePayload> parts) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GmailMessageHeader(String name, String value) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GmailMessageBody(String data, Integer size) {
    }
}