package com.remail.mail;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "remail.gmail", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoopGmailInboxSource implements GmailInboxSource {

    @Override
    public List<MailIntakeDocument> pollRecentMessages() {
        return List.of();
    }

    @Override
    public List<MailIntakeDocument> fetchChangedMessages(String emailAddress, String historyId) {
        return List.of();
    }
}