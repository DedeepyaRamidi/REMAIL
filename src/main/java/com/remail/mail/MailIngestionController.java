package com.remail.mail;

import com.remail.task.TaskEntity;
import com.remail.task.TaskView;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mail")
public class MailIngestionController {

    private final MailProcessingService mailProcessingService;

    public MailIngestionController(MailProcessingService mailProcessingService) {
        this.mailProcessingService = mailProcessingService;
    }

    @PostMapping("/intake")
    public ResponseEntity<TaskView> ingest(@RequestBody MailIntakeDocument document) {
        Optional<TaskEntity> task = mailProcessingService.process(document);
        return task.map(entity -> ResponseEntity.status(HttpStatus.CREATED).body(toView(entity)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    private TaskView toView(TaskEntity task) {
        return new TaskView(
                task.getId(),
                task.getTitle(),
                task.getCompanyName(),
                task.getDeadlineText(),
                task.getLocation(),
                task.getWebsite(),
                task.getDesignation(),
                task.getStatus(),
                task.getSnoozeUntil(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }
}