package com.remail.task;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "remail.gmail.enabled", havingValue = "false", matchIfMissing = true)
public class NoopReminderDeliveryService implements ReminderDeliveryService {

    @Override
    public void sendReminderDigest(List<TaskEntity> tasks) {
        // no-op
    }
}