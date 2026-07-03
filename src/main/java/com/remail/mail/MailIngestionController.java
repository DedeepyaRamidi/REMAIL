package com.remail.mail;

import com.remail.task.TaskEntity;
import com.remail.task.TaskView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mail")
@Tag(name = "Mail Ingestion", description = "Email ingestion and processing endpoints")
public class MailIngestionController {

    private final MailProcessingService mailProcessingService;

    public MailIngestionController(MailProcessingService mailProcessingService) {
        this.mailProcessingService = mailProcessingService;
    }

    @PostMapping("/intake")
    @Operation(summary = "Ingest email", description = "Process and ingest an email to extract registration deadlines")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Email processed and task created",
                    content = @Content(schema = @Schema(implementation = TaskView.class))),
            @ApiResponse(responseCode = "204", description = "Email processed but no urgent deadline detected")
    })
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