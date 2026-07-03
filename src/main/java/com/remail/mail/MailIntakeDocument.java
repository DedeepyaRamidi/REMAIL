package com.remail.mail;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "MailIntakeDocument", description = "Email document for ingestion and processing")
public record MailIntakeDocument(
        @Schema(description = "Unique message identifier from email source", example = "msg123@gmail.com")
        String messageId,
        @Schema(description = "Email subject line", example = "Registration for Senior Developer Position - Deadline Dec 31")
        String subject,
        @Schema(description = "Sender's email address", example = "recruiter@company.com")
        String from,
        @Schema(description = "Email body content in plain text", example = "Dear Candidate,\nPlease register for our job opening...")
        String body
) {
}