package com.remail.task;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(name = "Task", description = "A registration deadline task extracted from email")
public record TaskView(
        @Schema(description = "Unique task identifier")
        long id,
        @Schema(description = "Job title or position name")
        String title,
        @Schema(description = "Company name offering the registration")
        String companyName,
        @Schema(description = "Human-readable deadline text")
        String deadlineText,
        @Schema(description = "Job location or office address")
        String location,
        @Schema(description = "Company website or registration link")
        String website,
        @Schema(description = "Job designation or role")
        String designation,
        @Schema(description = "Current status of the task")
        TaskStatus status,
        @Schema(description = "When the task will be snoozed until (if snoozed)")
        LocalDateTime snoozeUntil,
        @Schema(description = "Task creation timestamp")
        LocalDateTime createdAt,
        @Schema(description = "Last update timestamp")
        LocalDateTime updatedAt
) {
}