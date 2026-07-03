package com.remail.task;

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
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/active")
    public ResponseEntity<TaskView> getActiveTask() {
        return taskService.findActiveTask()
                .map(task -> ResponseEntity.ok(toView(task)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/{id}/action")
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