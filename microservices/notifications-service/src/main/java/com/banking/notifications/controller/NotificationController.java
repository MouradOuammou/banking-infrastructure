package com.banking.notifications.controller;

import com.banking.notifications.dto.NotificationDto;
import com.banking.notifications.dto.SendNotificationRequest;
import com.banking.notifications.entity.NotificationChannel;
import com.banking.notifications.entity.NotificationStatus;
import com.banking.notifications.entity.NotificationType;
import com.banking.notifications.service.NotificationService;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*", maxAge = 3600)
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('ADMIN')")
    @Timed(value = "notifications.send", description = "Time taken to send notification")
    public ResponseEntity<NotificationDto> sendNotification(@Valid @RequestBody SendNotificationRequest request) {
        NotificationDto notification = notificationService.sendNotification(request);
        return new ResponseEntity<>(notification, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('EMPLOYEE') or hasRole('ADMIN')")
    @Timed(value = "notifications.get", description = "Time taken to get notification")
    public ResponseEntity<NotificationDto> getNotification(@PathVariable Long id) {
        return notificationService.getNotificationById(id)
                .map(notification -> ResponseEntity.ok(notification))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('EMPLOYEE') or hasRole('ADMIN')")
    @Timed(value = "notifications.getByUser", description = "Time taken to get notifications by user")
    public ResponseEntity<Page<NotificationDto>> getNotificationsByUser(
            @PathVariable Long userId, 
            Pageable pageable) {
        Page<NotificationDto> notifications = notificationService.getNotificationsByUserId(userId, pageable);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/user/{userId}/status/{status}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('EMPLOYEE') or hasRole('ADMIN')")
    public ResponseEntity<Page<NotificationDto>> getNotificationsByUserAndStatus(
            @PathVariable Long userId,
            @PathVariable NotificationStatus status,
            Pageable pageable) {
        Page<NotificationDto> notifications = notificationService.getNotificationsByUserIdAndStatus(userId, status, pageable);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/user/{userId}/type/{type}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('EMPLOYEE') or hasRole('ADMIN')")
    public ResponseEntity<Page<NotificationDto>> getNotificationsByUserAndType(
            @PathVariable Long userId,
            @PathVariable NotificationType type,
            Pageable pageable) {
        Page<NotificationDto> notifications = notificationService.getNotificationsByUserIdAndType(userId, type, pageable);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/user/{userId}/channel/{channel}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('EMPLOYEE') or hasRole('ADMIN')")
    public ResponseEntity<Page<NotificationDto>> getNotificationsByUserAndChannel(
            @PathVariable Long userId,
            @PathVariable NotificationChannel channel,
            Pageable pageable) {
        Page<NotificationDto> notifications = notificationService.getNotificationsByUserIdAndChannel(userId, channel, pageable);
        return ResponseEntity.ok(notifications);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('ADMIN')")
    @Timed(value = "notifications.updateStatus", description = "Time taken to update notification status")
    public ResponseEntity<NotificationDto> updateNotificationStatus(
            @PathVariable Long id,
            @RequestParam NotificationStatus status,
            @RequestParam(required = false) String errorMessage) {
        NotificationDto notification = notificationService.updateNotificationStatus(id, status, errorMessage);
        return ResponseEntity.ok(notification);
    }

    @PostMapping("/process-scheduled")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> processScheduledNotifications() {
        notificationService.processScheduledNotifications();
        return ResponseEntity.ok("Scheduled notifications processed");
    }

    @PostMapping("/retry-failed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> retryFailedNotifications() {
        notificationService.retryFailedNotifications();
        return ResponseEntity.ok("Failed notifications retried");
    }
}
