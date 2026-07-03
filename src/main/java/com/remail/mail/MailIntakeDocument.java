package com.remail.mail;

public record MailIntakeDocument(String messageId, String subject, String from, String body) {
}