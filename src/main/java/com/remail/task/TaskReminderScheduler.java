package com.remail.task;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class TaskReminderScheduler {

    private final TaskService taskService;
    private final ReminderDeliveryService reminderDeliveryService;

    public TaskReminderScheduler(TaskService taskService, ReminderDeliveryService reminderDeliveryService) {
        this.taskService = taskService;
        this.reminderDeliveryService = reminderDeliveryService;
    }

    @Scheduled(fixedDelayString = "${remail.task.reminder-delay-ms:60000}")
    public void reactivateDueReminders() {
        var dueTasks = taskService.findDueReminders();
        if (dueTasks.isEmpty()) {
            return;
        }

        taskService.reactivateDueReminders();
        reminderDeliveryService.sendReminderDigest(dueTasks);
    }
}