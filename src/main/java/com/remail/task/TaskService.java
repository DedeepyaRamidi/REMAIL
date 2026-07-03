package com.remail.task;

import com.remail.mail.RegistrationMailSnapshot;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final CalendarSyncService calendarSyncService;

    public TaskService(TaskRepository taskRepository, CalendarSyncService calendarSyncService) {
        this.taskRepository = taskRepository;
        this.calendarSyncService = calendarSyncService;
    }

    public TaskEntity createFromSnapshot(RegistrationMailSnapshot snapshot, String sourceMessageId) {
        TaskEntity task = taskRepository.findBySourceMessageId(sourceMessageId)
                .orElseGet(() -> taskRepository.save(new TaskEntity(
                sourceMessageId,
                snapshot.title(),
                snapshot.companyName(),
                snapshot.registrationDeadlineText(),
                snapshot.location(),
                snapshot.website(),
                snapshot.designation(),
                snapshot.normalizedBody())));
        calendarSyncService.upsertTask(task);
        return task;
    }

    public Optional<TaskEntity> findActiveTask() {
        return taskRepository.findFirstByStatusNotOrderByUpdatedAtDesc(TaskStatus.COMPLETED);
    }

    public List<TaskEntity> findAll() {
        return taskRepository.findAllByOrderByUpdatedAtDesc();
    }

    public TaskEntity applyAction(long taskId, TaskActionType actionType) {
        TaskEntity task = getRequiredTask(taskId);
        switch (actionType) {
            case SNOOZE -> task.snooze(LocalDateTime.now().plusMinutes(15));
            case DISMISS, FILLED -> task.complete();
        }
        TaskEntity savedTask = taskRepository.save(task);
        calendarSyncService.syncTaskState(savedTask);
        return savedTask;
    }

    private TaskEntity getRequiredTask(long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
    }
}