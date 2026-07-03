package com.remail.task;

import java.time.LocalDateTime;

public record TaskView(
        long id,
        String title,
        String companyName,
        String deadlineText,
        String location,
        String website,
        String designation,
        TaskStatus status,
        LocalDateTime snoozeUntil,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}