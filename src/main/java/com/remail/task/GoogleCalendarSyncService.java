package com.remail.task;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleCalendarSyncService implements CalendarSyncService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarSyncService.class);
    private static final String CALENDAR_API_BASE = "https://www.googleapis.com/calendar/v3/calendars";
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    private final GoogleCalendarProperties properties;

    public GoogleCalendarSyncService(GoogleCalendarProperties properties) {
        this.properties = properties;
    }

    @Override
    public void upsertTask(TaskEntity task) {
        if (!properties.isEnabled() || properties.getAccessToken() == null 
                || properties.getAccessToken().isBlank()) {
            logger.debug("Google Calendar sync disabled or no access token available");
            return;
        }

        try {
            String calendarId = getCalendarId();
            String eventJson = buildEventJson(task);

            if (task.getCalendarEventId() != null) {
                updateEvent(calendarId, task.getCalendarEventId(), eventJson);
            } else {
                String eventId = createEvent(calendarId, eventJson);
                task.setCalendarEventId(eventId);
            }
        } catch (Exception e) {
            logger.error("Failed to upsert calendar event for task {}", task.getId(), e);
        }
    }

    @Override
    public void syncTaskState(TaskEntity task) {
        if (!properties.isEnabled() || properties.getAccessToken() == null 
                || properties.getAccessToken().isBlank() || task.getCalendarEventId() == null) {
            logger.debug("Cannot sync task state: calendar disabled or event not found");
            return;
        }

        try {
            String calendarId = getCalendarId();
            String eventId = task.getCalendarEventId();
            String eventJson = buildEventJson(task);
            updateEvent(calendarId, eventId, eventJson);
            logger.info("Synced calendar event {} state for task {}", eventId, task.getId());
        } catch (Exception e) {
            logger.error("Failed to sync calendar event state for task {}", task.getId(), e);
        }
    }

    private String createEvent(String calendarId, String eventJson) throws IOException, InterruptedException {
        String url = CALENDAR_API_BASE + "/" + calendarId + "/events";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + properties.getAccessToken())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(eventJson))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            String eventId = extractEventId(response.body());
            logger.info("Created calendar event {} with JSON: {}", eventId, eventJson);
            return eventId;
        } else {
            logger.error("Failed to create calendar event. Status: {}, Response: {}", response.statusCode(), response.body());
            return null;
        }
    }

    private void updateEvent(String calendarId, String eventId, String eventJson) throws IOException, InterruptedException {
        String url = CALENDAR_API_BASE + "/" + calendarId + "/events/" + eventId;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + properties.getAccessToken())
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(eventJson))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            logger.info("Updated calendar event {} with JSON: {}", eventId, eventJson);
        } else {
            logger.error("Failed to update calendar event {}. Status: {}, Response: {}", 
                    eventId, response.statusCode(), response.body());
        }
    }

    private String buildEventJson(TaskEntity task) {
        StringBuilder json = new StringBuilder("{");
        json.append("\"summary\":\"").append(escapeJson(task.getTitle())).append("\",");
        json.append("\"description\":\"").append(escapeJson(buildEventDescription(task))).append("\",");

        LocalDateTime eventTime = task.getSnoozeUntil() != null ? task.getSnoozeUntil() : task.getCreatedAt();
        if (eventTime != null) {
            String dateTime = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(eventTime);
            json.append("\"start\":{\"dateTime\":\"").append(dateTime).append("Z\",\"timeZone\":\"UTC\"},");
            json.append("\"end\":{\"dateTime\":\"").append(dateTime).append("Z\",\"timeZone\":\"UTC\"},");
        }

        if (task.getStatus() == TaskStatus.COMPLETED) {
            json.append("\"transparency\":\"transparent\",");
            json.append("\"status\":\"cancelled\"");
        } else {
            json.append("\"transparency\":\"opaque\"");
        }

        json.append("}");
        return json.toString();
    }

    private String buildEventDescription(TaskEntity task) {
        StringBuilder description = new StringBuilder();
        description.append("Company: ").append(task.getCompanyName()).append("\\n");
        if (task.getDesignation() != null) {
            description.append("Position: ").append(task.getDesignation()).append("\\n");
        }
        if (task.getLocation() != null) {
            description.append("Location: ").append(task.getLocation()).append("\\n");
        }
        if (task.getWebsite() != null) {
            description.append("Website: ").append(task.getWebsite()).append("\\n");
        }
        description.append("Deadline: ").append(task.getDeadlineText()).append("\\n");
        description.append("Status: ").append(task.getStatus());
        return description.toString();
    }

    private String extractEventId(String response) {
        int idIndex = response.indexOf("\"id\":\"");
        if (idIndex >= 0) {
            int startIndex = idIndex + 6;
            int endIndex = response.indexOf("\"", startIndex);
            return response.substring(startIndex, endIndex);
        }
        return null;
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String getCalendarId() {
        String calendarId = properties.getCalendarId();
        return calendarId != null && !calendarId.isBlank() ? calendarId : "primary";
    }
}

