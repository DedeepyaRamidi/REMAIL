package com.remail.task;

public interface CalendarSyncService {

    void upsertTask(TaskEntity task);

    void syncTaskState(TaskEntity task);
}