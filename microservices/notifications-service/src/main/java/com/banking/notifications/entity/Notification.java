package com.banking.notifications.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_user_id", columnList = "userId"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_type", columnList = "type"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
@EntityListeners(AuditingEntityListener.class)
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private Long userId;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    private NotificationChannel channel;

    @NotBlank
    private String recipient; // email, phone number, or device token

    @NotBlank
    @Column(length = 200)
    private String subject;

    @Column(length = 2000)
    private String content;

    @Column(length = 5000)
    private String htmlContent;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private NotificationPriority priority = NotificationPriority.NORMAL;

    private String templateName;

    @ElementCollection
    @CollectionTable(name = "notification_parameters", 
                    joinColumns = @JoinColumn(name = "notification_id"))
    @MapKeyColumn(name = "parameter_key")
    @Column(name = "parameter_value")
    private Map<String, String> templateParameters;

    private String externalId; // ID from external service (email provider, SMS provider, etc.)

    private String errorMessage;

    private Integer retryCount = 0;

    private LocalDateTime scheduledAt;

    private LocalDateTime sentAt;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Constructors
    public Notification() {}

    public Notification(Long userId, NotificationType type, NotificationChannel channel, 
                       String recipient, String subject, String content) {
        this.userId = userId;
        this.type = type;
        this.channel = channel;
        this.recipient = recipient;
        this.subject = subject;
        this.content = content;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public NotificationChannel getChannel() { return channel; }
    public void setChannel(NotificationChannel channel) { this.channel = channel; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getHtmlContent() { return htmlContent; }
    public void setHtmlContent(String htmlContent) { this.htmlContent = htmlContent; }

    public NotificationStatus getStatus() { return status; }
    public void setStatus(NotificationStatus status) { this.status = status; }

    public NotificationPriority getPriority() { return priority; }
    public void setPriority(NotificationPriority priority) { this.priority = priority; }

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }

    public Map<String, String> getTemplateParameters() { return templateParameters; }
    public void setTemplateParameters(Map<String, String> templateParameters) { this.templateParameters = templateParameters; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Business methods
    public boolean isPending() {
        return status == NotificationStatus.PENDING;
    }

    public boolean isSent() {
        return status == NotificationStatus.SENT;
    }

    public boolean isFailed() {
        return status == NotificationStatus.FAILED;
    }

    public boolean canRetry() {
        return retryCount < 3 && (status == NotificationStatus.FAILED || status == NotificationStatus.PENDING);
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }
}
