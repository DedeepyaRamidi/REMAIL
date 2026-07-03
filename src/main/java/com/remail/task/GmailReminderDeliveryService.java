package com.remail.task;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.remail.mail.GmailProperties;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class GmailReminderDeliveryService implements ReminderDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(GmailReminderDeliveryService.class);
    private static final String API_BASE_URL = "https://gmail.googleapis.com/gmail/v1";

    private final GmailProperties properties;
    private final RestTemplate restTemplate;
    private volatile TokenState cachedToken;

    public GmailReminderDeliveryService(GmailProperties properties, RestTemplateBuilder restTemplateBuilder) {
        this(properties, restTemplateBuilder.build());
    }

    GmailReminderDeliveryService(GmailProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    @Override
    public void sendReminderDigest(List<TaskEntity> tasks) {
        if (tasks == null || tasks.isEmpty() || !properties.isEnabled() || !hasText(properties.getUserId())) {
            return;
        }

        String recipient = properties.getUserId().trim();
        String subject = "REMAIL reminder: action needed on " + tasks.size() + " mail" + (tasks.size() == 1 ? "" : "s");
        String body = buildBody(tasks);

        try {
            sendMessage(recipient, subject, body);
        } catch (RestClientException exception) {
            log.warn("Unable to send REMAIL reminder digest to {}", recipient, exception);
        }
    }

    private void sendMessage(String recipient, String subject, String body) {
        String rawMessage = buildRawMessage(recipient, subject, body);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(resolveAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        String userId = effectiveUserId();
        String url = UriComponentsBuilder.fromHttpUrl(API_BASE_URL)
                .path("/users/{userId}/messages/send")
                .buildAndExpand(Map.of("userId", userId))
                .toUriString();

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(Map.of("raw", rawMessage), headers),
                String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.warn("Reminder digest send returned status {}", response.getStatusCode());
        }
    }

    private String buildBody(List<TaskEntity> tasks) {
        StringBuilder builder = new StringBuilder();
        builder.append("You still have urgent mail to handle.\n\n");
        for (TaskEntity task : tasks) {
            builder.append("- ").append(nullSafe(task.getCompanyName())).append(" | ")
                    .append(nullSafe(task.getTitle())).append(" | ")
                    .append(nullSafe(task.getDeadlineText())).append(" | ")
                    .append(nullSafe(task.getStatus().name())).append("\n");
        }
        builder.append("\nOpen REMAIL and tap Submitted, Not needed, or Remind later so nothing is missed.");
        return builder.toString();
    }

    private String buildRawMessage(String recipient, String subject, String body) {
        StringBuilder raw = new StringBuilder();
        raw.append("To: ").append(recipient).append("\r\n");
        raw.append("Subject: ").append(subject).append("\r\n");
        raw.append("Content-Type: text/plain; charset=UTF-8\r\n");
        raw.append("\r\n");
        raw.append(body);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.toString().getBytes(StandardCharsets.UTF_8));
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
            throw new IllegalStateException("Unable to obtain Gmail access token for reminders");
        }

        long expiresAt = System.currentTimeMillis() / 1000L + Math.max(60, response.expiresIn());
        return new TokenState(response.accessToken(), expiresAt);
    }

    private String effectiveUserId() {
        if (hasText(properties.getUserId())) {
            return properties.getUserId().trim();
        }
        return "me";
    }

    private String nullSafe(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record TokenState(String accessToken, long expiresAtEpochSecond) {

        private boolean isValid() {
            return hasText(accessToken) && (System.currentTimeMillis() / 1000L) < expiresAtEpochSecond - 30;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GmailTokenResponse(@JsonProperty("access_token") String accessToken,
                                      @JsonProperty("expires_in") long expiresIn) {
    }
}