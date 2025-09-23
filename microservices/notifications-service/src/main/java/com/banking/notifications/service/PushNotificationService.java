package com.banking.notifications.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PushNotificationService {
    private static final Logger logger = LoggerFactory.getLogger(PushNotificationService.class);

    @CircuitBreaker(name = "push-service", fallbackMethod = "sendPushNotificationFallback")
    @Retry(name = "push-service")
    public String sendPushNotification(String deviceToken, String title, String body) {
        try {
            // Simulate push notification sending (in real implementation, integrate with FCM, APNS, etc.)
            logger.info("Sending push notification to device {}: {} - {}", deviceToken, title, body);
            
            // Simulate processing time
            Thread.sleep(50);
            
            String messageId = UUID.randomUUID().toString();
            logger.info("Push notification sent successfully to {} with message ID: {}", deviceToken, messageId);
            return messageId;

        } catch (Exception e) {
            logger.error("Failed to send push notification to {}: {}", deviceToken, e.getMessage());
            throw new RuntimeException("Failed to send push notification", e);
        }
    }

    public String sendPushNotificationFallback(String deviceToken, String title, String body, Exception ex) {
        logger.error("Push notification service fallback triggered for {}: {}", deviceToken, ex.getMessage());
        return null;
    }
}
