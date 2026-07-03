package com.remail.mail;

import java.util.List;

public interface GmailInboxSource {

    List<MailIntakeDocument> pollRecentMessages();

    List<MailIntakeDocument> fetchChangedMessages(String emailAddress, String historyId);
}