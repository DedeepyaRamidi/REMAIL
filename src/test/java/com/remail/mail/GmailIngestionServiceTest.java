package com.remail.mail;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class GmailIngestionServiceTest {

    @Test
    void pollsInboxAndProcessesAvailableMessages() {
        MailIntakeDocument document = new MailIntakeDocument("message-1", "subject", "from", "body");
        RecordingGmailInboxSource gmailInboxSource = new RecordingGmailInboxSource(List.of(document));
        RecordingMailProcessingService mailProcessingService = new RecordingMailProcessingService();
        GmailIngestionService service = new GmailIngestionService(gmailInboxSource, mailProcessingService);

        assertThat(service.pollInbox()).isZero();
        assertThat(mailProcessingService.processedCount.get()).isEqualTo(1);
        assertThat(mailProcessingService.lastDocument).isSameAs(document);
    }

    static class RecordingGmailInboxSource implements GmailInboxSource {

        private final List<MailIntakeDocument> documents;

        RecordingGmailInboxSource(List<MailIntakeDocument> documents) {
            this.documents = documents;
        }

        @Override
        public List<MailIntakeDocument> pollRecentMessages() {
            return documents;
        }

        @Override
        public List<MailIntakeDocument> fetchChangedMessages(String emailAddress, String historyId) {
            return documents;
        }
    }

    static class RecordingMailProcessingService extends MailProcessingService {

        private final AtomicInteger processedCount = new AtomicInteger();
        private MailIntakeDocument lastDocument;

        RecordingMailProcessingService() {
            super(null, null);
        }

        @Override
        public Optional<com.remail.task.TaskEntity> process(MailIntakeDocument document) {
            lastDocument = document;
            processedCount.incrementAndGet();
            return Optional.empty();
        }
    }
}