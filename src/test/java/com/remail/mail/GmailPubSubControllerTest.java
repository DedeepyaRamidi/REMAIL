package com.remail.mail;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class GmailPubSubControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecordingGmailIngestionService gmailIngestionService;

    @Test
    void decodesPubSubEnvelopeAndDispatchesNotification() throws Exception {
        String notificationJson = "{" +
                "\"emailAddress\":\"students.cdc2027@vitap.ac.in\"," +
                "\"historyId\":\"18192\"}";
        String encoded = Base64.getEncoder().encodeToString(notificationJson.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(post("/api/gmail/pubsub")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"message\":{\"data\":\"" + encoded + "\",\"messageId\":\"1\",\"publishTime\":\"now\"},\"subscription\":\"projects/test/subscriptions/gmail\"}"))
                .andExpect(status().isAccepted());

        assert gmailIngestionService.lastEmailAddress.get().equals("students.cdc2027@vitap.ac.in");
        assert gmailIngestionService.lastHistoryId.get().equals("18192");
    }

    @TestConfiguration
    static class TestConfig {

        @Bean(name = "recordingGmailIngestionService")
        @Primary
        RecordingGmailIngestionService gmailIngestionService() {
            return new RecordingGmailIngestionService();
        }
    }

    static class RecordingGmailIngestionService extends GmailIngestionService {

        private final AtomicReference<String> lastEmailAddress = new AtomicReference<>();
        private final AtomicReference<String> lastHistoryId = new AtomicReference<>();

        RecordingGmailIngestionService() {
            super(null, null);
        }

        @Override
        public int pollInbox() {
            return 0;
        }

        @Override
        public int handleNotification(String emailAddress, String historyId) {
            lastEmailAddress.set(emailAddress);
            lastHistoryId.set(historyId);
            return 1;
        }
    }
}