package com.banking.notifications.service;

import com.banking.notifications.dto.NotificationDto;
import com.banking.notifications.dto.SendNotificationRequest;
import com.banking.notifications.entity.Notification;
import com.banking.notifications.entity.NotificationChannel;
import com.banking.notifications.entity.NotificationStatus;
import com.banking.notifications.entity.NotificationType;
import com.banking.notifications.mapper.NotificationMapper;
import com.banking.notifications.repository.NotificationRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SmsService smsService;

    @Autowired
    private PushNotificationService pushNotificationService;

    @Autowired
    private TemplateService templateService;

    @CircuitBreaker(name = "notification-service", fallbackMethod = "sendNotificationFallback")
    @Retry(name = "notification-service")
    public NotificationDto sendNotification(SendNotificationRequest request) {
        // Create notification entity
        Notification notification = new Notification();
        notification.setUserId(request.getUserId());
        notification.setType(request.getType());
        notification.setChannel(request.getChannel());
        notification.setRecipient(request.getRecipient());
        notification.setSubject(request.getSubject());
        notification.setContent(request.getContent());
        notification.setHtmlContent(request.getHtmlContent());
        notification.setPriority(request.getPriority());
        notification.setTemplateName(request.getTemplateName());
        notification.setTemplateParameters(request.getTemplateParameters());
        notification.setScheduledAt(request.getScheduledAt());

        // Process template if specified
        if (request.getTemplateName() != null) {
            processTemplate(notification);
        }

        // Save notification
        Notification savedNotification = notificationRepository.save(notification);

        // Send immediately if not scheduled
        if (request.getScheduledAt() == null || request.getScheduledAt().isBefore(LocalDateTime.now())) {
            sendNotificationAsync(savedNotification);
        }

        return notificationMapper.toDto(savedNotification);
    }

    public NotificationDto sendNotificationFallback(SendNotificationRequest request, Exception ex) {
        logger.error("Failed to send notification: {}", ex.getMessage());
        
        // Create a failed notification record
        Notification notification = new Notification();
        notification.setUserId(request.getUserId());
        notification.setType(request.getType());
        notification.setChannel(request.getChannel());
        notification.setRecipient(request.getRecipient());
        notification.setSubject(request.getSubject());
        notification.setContent(request.getContent());
        notification.setStatus(NotificationStatus.FAILED);
        notification.setErrorMessage("Service unavailable: " + ex.getMessage());

        Notification savedNotification = notificationRepository.save(notification);
        return notificationMapper.toDto(savedNotification);
    }

    @Async
    public void sendNotificationAsync(Notification notification) {
        try {
            notification.setStatus(NotificationStatus.PROCESSING);
            notificationRepository.save(notification);

            boolean success = false;
            String externalId = null;

            switch (notification.getChannel()) {
                case EMAIL:
                    externalId = emailService.sendEmail(
                        notification.getRecipient(),
                        notification.getSubject(),
                        notification.getContent(),
                        notification.getHtmlContent()
                    );
                    success = externalId != null;
                    break;

                case SMS:
                    externalId = smsService.sendSms(
                        notification.getRecipient(),
                        notification.getContent()
                    );
                    success = externalId != null;
                    break;

                case PUSH:
                    externalId = pushNotificationService.sendPushNotification(
                        notification.getRecipient(),
                        notification.getSubject(),
                        notification.getContent()
                    );
                    success = externalId != null;
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported notification channel: " + notification.getChannel());
            }

            if (success) {
                notification.setStatus(NotificationStatus.SENT);
                notification.setExternalId(externalId);
                notification.setSentAt(LocalDateTime.now());
                logger.info("Notification sent successfully: {}", notification.getId());
            } else {
                notification.setStatus(NotificationStatus.FAILED);
                notification.setErrorMessage("Failed to send notification via " + notification.getChannel());
                logger.error("Failed to send notification: {}", notification.getId());
            }

        } catch (Exception e) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            notification.incrementRetryCount();
            logger.error("Error sending notification {}: {}", notification.getId(), e.getMessage());
        } finally {
            notificationRepository.save(notification);
        }
    }

    @Transactional(readOnly = true)
    public Optional<NotificationDto> getNotificationById(Long id) {
        return notificationRepository.findById(id)
                .map(notificationMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotificationsByUserId(Long userId, Pageable pageable) {
        return notificationRepository.findByUserId(userId, pageable)
                .map(notificationMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotificationsByUserIdAndStatus(Long userId, NotificationStatus status, Pageable pageable) {
        return notificationRepository.findByUserIdAndStatus(userId, status, pageable)
                .map(notificationMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotificationsByUserIdAndType(Long userId, NotificationType type, Pageable pageable) {
        return notificationRepository.findByUserIdAndType(userId, type, pageable)
                .map(notificationMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotificationsByUserIdAndChannel(Long userId, NotificationChannel channel, Pageable pageable) {
        return notificationRepository.findByUserIdAndChannel(userId, channel, pageable)
                .map(notificationMapper::toDto);
    }

    public void processScheduledNotifications() {
        List<Notification> scheduledNotifications = notificationRepository
                .findByStatusAndScheduledAtBefore(NotificationStatus.PENDING, LocalDateTime.now());

        for (Notification notification : scheduledNotifications) {
            sendNotificationAsync(notification);
        }

        logger.info("Processed {} scheduled notifications", scheduledNotifications.size());
    }

    public void retryFailedNotifications() {
        List<Notification> failedNotifications = notificationRepository.findFailedNotificationsForRetry();

        for (Notification notification : failedNotifications) {
            if (notification.canRetry()) {
                sendNotificationAsync(notification);
            }
        }

        logger.info("Retried {} failed notifications", failedNotifications.size());
    }

    public NotificationDto updateNotificationStatus(Long notificationId, NotificationStatus status, String errorMessage) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found with id: " + notificationId));

        notification.setStatus(status);
        if (errorMessage != null) {
            notification.setErrorMessage(errorMessage);
        }

        if (status == NotificationStatus.SENT) {
            notification.setSentAt(LocalDateTime.now());
        }

        Notification updatedNotification = notificationRepository.save(notification);
        return notificationMapper.toDto(updatedNotification);
    }

    @Transactional(readOnly = true)
    public Long getNotificationCountByUserAndStatus(Long userId, NotificationStatus status, LocalDateTime since) {
        return notificationRepository.countSentNotificationsByUserSince(userId, since);
    }

    private void processTemplate(Notification notification) {
        if (notification.getTemplateName() != null && notification.getTemplateParameters() != null) {
            try {
                String processedContent = templateService.processTemplate(
                    notification.getTemplateName(), 
                    notification.getTemplateParameters()
                );
                notification.setContent(processedContent);

                // Process HTML template if it's an email
                if (notification.getChannel() == NotificationChannel.EMAIL) {
                    String processedHtmlContent = templateService.processHtmlTemplate(
                        notification.getTemplateName(), 
                        notification.getTemplateParameters()
                    );
                    notification.setHtmlContent(processedHtmlContent);
                }
            } catch (Exception e) {
                logger.error("Failed to process template {}: {}", notification.getTemplateName(), e.getMessage());
            }
        }
    }
}
