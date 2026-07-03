package com.remail.task;

import java.util.List;

public interface ReminderDeliveryService {

    void sendReminderDigest(List<TaskEntity> tasks);
}