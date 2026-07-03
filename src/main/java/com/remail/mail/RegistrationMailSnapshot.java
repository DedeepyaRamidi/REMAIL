package com.remail.mail;

public record RegistrationMailSnapshot(
        String sourceSubject,
        String title,
        String companyName,
        String category,
        String registrationDeadlineText,
        String location,
        String website,
        String designation,
        boolean urgencyDetected,
        String normalizedBody
) {
}