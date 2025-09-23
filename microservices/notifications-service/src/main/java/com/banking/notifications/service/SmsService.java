package com.banking.notifications.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SmsService {
    private static final Logger logger = LoggerFactory.getLogger(SmsService.class);

    @CircuitBreaker(name = "sms-service", fallbackMethod = "sendSmsFallback")
    @Retry(name = "sms-service")
    public String sendSms(String phoneNumber, String message) {
        try {
            // Simulate SMS sending (in real implementation, integrate with SMS provider like Twilio, AWS SNS, etc.)
            logger.info("Sending SMS to {}: {}", phoneNumber, message);
            
            // Simulate processing time
            Thread.sleep(100);
            
            String messageId = UUID.randomUUID().toString();
            logger.info("SMS sent successfully to {} with message ID: {}", phoneNumber, messageId);
            return messageId;

        } catch (Exception e) {
            logger.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage());
            throw new RuntimeException("Failed to send SMS", e);
        }
    }

    public String sendSmsFallback(String phoneNumber, String message, Exception ex) {
        logger.error("SMS service fallback triggered for {}: {}", phoneNumber, ex.getMessage());
        return null;
    }
}
