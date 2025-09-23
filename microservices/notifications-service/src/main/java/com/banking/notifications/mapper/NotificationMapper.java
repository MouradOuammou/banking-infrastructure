package com.banking.notifications.mapper;

import com.banking.notifications.dto.NotificationDto;
import com.banking.notifications.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationDto toDto(Notification notification) {
        if (notification == null) {
            return null;
        }

        NotificationDto dto = new NotificationDto();
        dto.setId(notification.getId());
        dto.setUserId(notification.getUserId());
        dto.setType(notification.getType());
        dto.setChannel(notification.getChannel());
        dto.setRecipient(notification.getRecipient());
        dto.setSubject(notification.getSubject());
        dto.setContent(notification.getContent());
        dto.setHtmlContent(notification.getHtmlContent());
        dto.setStatus(notification.getStatus());
        dto.setPriority(notification.getPriority());
        dto.setTemplateName(notification.getTemplateName());
        dto.setTemplateParameters(notification.getTemplateParameters());
        dto.setExternalId(notification.getExternalId());
        dto.setErrorMessage(notification.getErrorMessage());
        dto.setRetryCount(notification.getRetryCount());
        dto.setScheduledAt(notification.getScheduledAt());
        dto.setSentAt(notification.getSentAt());
        dto.setCreatedAt(notification.getCreatedAt());
        dto.setUpdatedAt(notification.getUpdatedAt());

        return dto;
    }

    public Notification toEntity(NotificationDto dto) {
        if (dto == null) {
            return null;
        }

        Notification notification = new Notification();
        notification.setId(dto.getId());
        notification.setUserId(dto.getUserId());
        notification.setType(dto.getType());
        notification.setChannel(dto.getChannel());
        notification.setRecipient(dto.getRecipient());
        notification.setSubject(dto.getSubject());
        notification.setContent(dto.getContent());
        notification.setHtmlContent(dto.getHtmlContent());
        notification.setStatus(dto.getStatus());
        notification.setPriority(dto.getPriority());
        notification.setTemplateName(dto.getTemplateName());
        notification.setTemplateParameters(dto.getTemplateParameters());
        notification.setExternalId(dto.getExternalId());
        notification.setErrorMessage(dto.getErrorMessage());
        notification.setRetryCount(dto.getRetryCount());
        notification.setScheduledAt(dto.getScheduledAt());
        notification.setSentAt(dto.getSentAt());

        return notification;
    }
}
