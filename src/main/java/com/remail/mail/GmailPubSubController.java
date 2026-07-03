package com.remail.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gmail")
public class GmailPubSubController {

    private final GmailIngestionService gmailIngestionService;
    private final ObjectMapper objectMapper;

    public GmailPubSubController(GmailIngestionService gmailIngestionService, ObjectMapper objectMapper) {
        this.gmailIngestionService = gmailIngestionService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/pubsub")
    public ResponseEntity<Void> receive(@RequestBody GmailPubSubEnvelope envelope) {
        GmailPushNotification notification = decodeNotification(envelope);
        gmailIngestionService.handleNotification(notification.emailAddress(), notification.historyId());
        return ResponseEntity.accepted().build();
    }

    private GmailPushNotification decodeNotification(GmailPubSubEnvelope envelope) {
        if (envelope == null || envelope.message() == null || envelope.message().data() == null || envelope.message().data().isBlank()) {
            throw new IllegalArgumentException("Pub/Sub envelope data is required");
        }

        byte[] decoded = Base64.getDecoder().decode(envelope.message().data());
        try {
            return objectMapper.readValue(new String(decoded, StandardCharsets.UTF_8), GmailPushNotification.class);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to decode Gmail push notification", exception);
        }
    }
}