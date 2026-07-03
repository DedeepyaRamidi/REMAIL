package com.remail.task;

import com.remail.mail.RegistrationMailSnapshot;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class TaskService {

    private static final int DEFAULT_REMINDER_MINUTES = 15;

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
        LocalDateTime now = LocalDateTime.now();
        return taskRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(task -> isActiveNow(task, now))
                .findFirst();
    }

    public List<TaskEntity> findAll() {
        return taskRepository.findAllByOrderByUpdatedAtDesc();
    }

    public List<TaskEntity> findTasks(String statusFilter, String companyFilter, String queryFilter) {
        Predicate<TaskEntity> matchesStatus = task -> !hasText(statusFilter)
            || task.getStatus().name().equalsIgnoreCase(statusFilter.trim());
        Predicate<TaskEntity> matchesCompany = task -> !hasText(companyFilter)
            || containsIgnoreCase(task.getCompanyName(), companyFilter);
        Predicate<TaskEntity> matchesQuery = task -> !hasText(queryFilter)
            || containsIgnoreCase(task.getTitle(), queryFilter)
            || containsIgnoreCase(task.getCompanyName(), queryFilter)
            || containsIgnoreCase(task.getDesignation(), queryFilter)
            || containsIgnoreCase(task.getLocation(), queryFilter)
            || containsIgnoreCase(task.getWebsite(), queryFilter)
            || containsIgnoreCase(task.getDeadlineText(), queryFilter)
            || containsIgnoreCase(task.getNormalizedBody(), queryFilter);

        return findAll().stream()
            .filter(matchesStatus)
            .filter(matchesCompany)
            .filter(matchesQuery)
            .collect(Collectors.toList());
    }

    public TaskEntity applyAction(long taskId, TaskActionType actionType) {
        TaskEntity task = getRequiredTask(taskId);
        switch (actionType) {
            case SNOOZE, REMIND_LATER -> task.remindLater(LocalDateTime.now().plusMinutes(DEFAULT_REMINDER_MINUTES));
            case DISMISS, FILLED -> task.complete();
        }
        TaskEntity savedTask = taskRepository.save(task);
        calendarSyncService.syncTaskState(savedTask);
        return savedTask;
    }

    public int reactivateDueReminders() {
        List<TaskEntity> dueTasks = findDueReminders();

        dueTasks.forEach(TaskEntity::reopen);
        if (!dueTasks.isEmpty()) {
            taskRepository.saveAll(dueTasks);
        }
        return dueTasks.size();
    }

    public List<TaskEntity> findDueReminders() {
        LocalDateTime now = LocalDateTime.now();
        return taskRepository.findAll().stream()
                .filter(task -> task.getStatus() == TaskStatus.SNOOZED)
                .filter(task -> task.getSnoozeUntil() != null && !task.getSnoozeUntil().isAfter(now))
                .collect(Collectors.toList());
    }

    private TaskEntity getRequiredTask(long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean containsIgnoreCase(String value, String searchTerm) {
        return value != null && searchTerm != null
                && value.toLowerCase(Locale.ROOT).contains(searchTerm.trim().toLowerCase(Locale.ROOT));
    }

    private boolean isActiveNow(TaskEntity task, LocalDateTime now) {
        if (task.getStatus() == TaskStatus.COMPLETED) {
            return false;
        }
        return task.getStatus() != TaskStatus.SNOOZED
                || task.getSnoozeUntil() == null
                || !task.getSnoozeUntil().isAfter(now);
    }
}