package com.remail.task;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
public class TaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 512)
    private String sourceMessageId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false)
    private String deadlineText;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private String website;

    @Column(nullable = false)
    private String designation;

    @Column(nullable = false, length = 10_000)
    private String normalizedBody;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    private LocalDateTime snoozeUntil;

    @Column(length = 512)
    private String calendarEventId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected TaskEntity() {
    }

    public TaskEntity(String sourceMessageId,
                      String title,
                      String companyName,
                      String deadlineText,
                      String location,
                      String website,
                      String designation,
                      String normalizedBody) {
        this.sourceMessageId = sourceMessageId;
        this.title = title;
        this.companyName = companyName;
        this.deadlineText = deadlineText;
        this.location = location;
        this.website = website;
        this.designation = designation;
        this.normalizedBody = normalizedBody;
        this.status = TaskStatus.PENDING;
    }

    public Long getId() {
        return id;
    }

    public String getSourceMessageId() {
        return sourceMessageId;
    }

    public String getTitle() {
        return title;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getDeadlineText() {
        return deadlineText;
    }

    public String getLocation() {
        return location;
    }

    public String getWebsite() {
        return website;
    }

    public String getDesignation() {
        return designation;
    }

    public String getNormalizedBody() {
        return normalizedBody;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public LocalDateTime getSnoozeUntil() {
        return snoozeUntil;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getCalendarEventId() {
        return calendarEventId;
    }

    public void setCalendarEventId(String calendarEventId) {
        this.calendarEventId = calendarEventId;
    }

    public boolean isActive() {
        return status != TaskStatus.COMPLETED;
    }

    public void snooze(LocalDateTime until) {
        this.status = TaskStatus.SNOOZED;
        this.snoozeUntil = until;
        this.updatedAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = TaskStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (status == null) {
            status = TaskStatus.PENDING;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}