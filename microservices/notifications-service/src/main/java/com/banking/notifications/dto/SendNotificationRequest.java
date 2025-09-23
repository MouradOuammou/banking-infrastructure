package com.banking.notifications.dto;

import com.banking.notifications.entity.NotificationChannel;
import com.banking.notifications.entity.NotificationPriority;
import com.banking.notifications.entity.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Map;

public class SendNotificationRequest {
    @NotNull
    private Long userId;

    @NotNull
    private NotificationType type;

    @NotNull
    private NotificationChannel channel;

    @NotBlank
    private String recipient;

    @NotBlank
    private String subject;

    private String content;

    private String htmlContent;

    private NotificationPriority priority = NotificationPriority.NORMAL;

    private String templateName;

    private Map<String, String> templateParameters;

    private LocalDateTime scheduledAt;

    // Getters and Setters
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

    public NotificationPriority getPriority() { return priority; }
    public void setPriority(NotificationPriority priority) { this.priority = priority; }

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }

    public Map<String, String> getTemplateParameters() { return templateParameters; }
    public void setTemplateParameters(Map<String, String> templateParameters) { this.templateParameters = templateParameters; }

    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
}
