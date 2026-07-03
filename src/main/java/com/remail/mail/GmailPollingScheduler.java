package com.remail.mail;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class GmailPollingScheduler {

    private final GmailIngestionService gmailIngestionService;

    public GmailPollingScheduler(GmailIngestionService gmailIngestionService) {
        this.gmailIngestionService = gmailIngestionService;
    }

    @Scheduled(fixedDelayString = "${remail.gmail.poll-delay-ms:300000}")
    public void pollInbox() {
        gmailIngestionService.pollInbox();
    }
}