package com.remail.task;

public class NoopCalendarSyncService implements CalendarSyncService {

    @Override
    public void upsertTask(TaskEntity task) {
        // Calendar API integration will be wired here.
    }

    @Override
    public void syncTaskState(TaskEntity task) {
        // Calendar API integration will be wired here.
    }
}