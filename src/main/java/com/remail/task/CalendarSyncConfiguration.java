package com.remail.task;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CalendarSyncConfiguration {

    @Bean
    @ConditionalOnProperty(name = "remail.calendar.enabled", havingValue = "true")
    public CalendarSyncService googleCalendarSyncService(GoogleCalendarProperties properties) {
        return new GoogleCalendarSyncService(properties);
    }

    @Bean
    @ConditionalOnProperty(name = "remail.calendar.enabled", havingValue = "false", matchIfMissing = true)
    public CalendarSyncService noopCalendarSyncService() {
        return new NoopCalendarSyncService();
    }
}
