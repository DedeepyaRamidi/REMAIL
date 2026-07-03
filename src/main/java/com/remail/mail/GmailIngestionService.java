package com.remail.mail;

import com.remail.task.TaskEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class GmailIngestionService {

    private final GmailInboxSource gmailInboxSource;
    private final MailProcessingService mailProcessingService;

    public GmailIngestionService(GmailInboxSource gmailInboxSource, MailProcessingService mailProcessingService) {
        this.gmailInboxSource = gmailInboxSource;
        this.mailProcessingService = mailProcessingService;
    }

    public int pollInbox() {
        return processDocuments(gmailInboxSource.pollRecentMessages());
    }

    public int handleNotification(String emailAddress, String historyId) {
        return processDocuments(gmailInboxSource.fetchChangedMessages(emailAddress, historyId));
    }

    private int processDocuments(List<MailIntakeDocument> documents) {
        int created = 0;
        for (MailIntakeDocument document : documents) {
            Optional<TaskEntity> task = mailProcessingService.process(document);
            if (task.isPresent()) {
                created++;
            }
        }
        return created;
    }
}