package com.remail.mail;

public record GmailPubSubEnvelope(PubSubMessage message, String subscription) {

    public record PubSubMessage(String data, String messageId, String publishTime) {
    }
}