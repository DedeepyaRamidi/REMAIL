package com.remail.task;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Tasks", description = "Task management endpoints for REMAIL")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/active")
    @Operation(summary = "Get active task", description = "Retrieve the currently active task (next in queue)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Active task found",
                    content = @Content(schema = @Schema(implementation = TaskView.class))),
            @ApiResponse(responseCode = "204", description = "No active task available")
    })
    public ResponseEntity<TaskView> getActiveTask() {
        return taskService.findActiveTask()
                .map(task -> ResponseEntity.ok(toView(task)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

        @GetMapping
        @Operation(summary = "List tasks", description = "List tasks with optional status, company, and query filters")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tasks returned successfully")
        })
        public List<TaskView> getTasks(@RequestParam(required = false) String status,
                                       @RequestParam(required = false) String company,
                                       @RequestParam(required = false) String query) {
        return taskService.findTasks(status, company, query).stream()
            .map(this::toView)
            .toList();
        }

    @PostMapping("/{id}/action")
    @Operation(summary = "Apply action to task", description = "Perform an action on a task (SNOOZE, DISMISS, or FILLED)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Action applied successfully",
                    content = @Content(schema = @Schema(implementation = TaskView.class))),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<TaskView> applyAction(@PathVariable long id,
                                                @RequestParam TaskActionType actionType) {
        try {
            return ResponseEntity.ok(toView(taskService.applyAction(id, actionType)));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
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