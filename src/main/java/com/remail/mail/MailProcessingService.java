package com.remail.mail;

import com.remail.task.TaskEntity;
import com.remail.task.TaskService;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class MailProcessingService {

    private final RegistrationMailParser parser;
    private final TaskService taskService;

    public MailProcessingService(RegistrationMailParser parser, TaskService taskService) {
        this.parser = parser;
        this.taskService = taskService;
    }

    public Optional<TaskEntity> process(MailIntakeDocument document) {
        RegistrationMailSnapshot snapshot = parser.parse(document);
        if (!snapshot.urgencyDetected()) {
            return Optional.empty();
        }

        String sourceMessageId = buildSourceMessageId(document);
        return Optional.of(taskService.createFromSnapshot(snapshot, sourceMessageId));
    }

    private String buildSourceMessageId(MailIntakeDocument document) {
        if (document.messageId() != null && !document.messageId().isBlank()) {
            return document.messageId();
        }
        return (document.from() == null ? "unknown" : document.from()) + "|"
                + (document.subject() == null ? "unknown" : document.subject());
    }
}