package com.banking.notifications.listener;

import com.banking.notifications.dto.SendNotificationRequest;
import com.banking.notifications.entity.NotificationChannel;
import com.banking.notifications.entity.NotificationPriority;
import com.banking.notifications.entity.NotificationType;
import com.banking.notifications.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class BankingEventListener {
    private static final Logger logger = LoggerFactory.getLogger(BankingEventListener.class);

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = "user-events", groupId = "notifications-service")
    public void handleUserEvents(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            String eventType = determineEventType(event);

            switch (eventType) {
                case "USER_CREATED":
                    handleUserCreatedEvent(event);
                    break;
                default:
                    logger.debug("Unhandled user event type: {}", eventType);
            }
        } catch (Exception e) {
            logger.error("Error processing user event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "account-events", groupId = "notifications-service")
    public void handleAccountEvents(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            String eventType = determineEventType(event);

            switch (eventType) {
                case "ACCOUNT_CREATED":
                    handleAccountCreatedEvent(event);
                    break;
                case "BALANCE_UPDATED":
                    handleBalanceUpdatedEvent(event);
                    break;
                case "ACCOUNT_STATUS_UPDATED":
                    handleAccountStatusUpdatedEvent(event);
                    break;
                default:
                    logger.debug("Unhandled account event type: {}", eventType);
            }
        } catch (Exception e) {
            logger.error("Error processing account event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "transaction-events", groupId = "notifications-service")
    public void handleTransactionEvents(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            String eventType = determineEventType(event);

            switch (eventType) {
                case "TRANSACTION_COMPLETED":
                    handleTransactionCompletedEvent(event);
                    break;
                case "TRANSACTION_FAILED":
                    handleTransactionFailedEvent(event);
                    break;
                default:
                    logger.debug("Unhandled transaction event type: {}", eventType);
            }
        } catch (Exception e) {
            logger.error("Error processing transaction event: {}", e.getMessage());
        }
    }

    private void handleUserCreatedEvent(Map<String, Object> event) {
        Long userId = getLongValue(event, "userId");
        String email = getStringValue(event, "email");
        String firstName = getStringValue(event, "firstName");

        if (userId != null && email != null) {
            SendNotificationRequest request = new SendNotificationRequest();
            request.setUserId(userId);
            request.setType(NotificationType.ACCOUNT_CREATED);
            request.setChannel(NotificationChannel.EMAIL);
            request.setRecipient(email);
            request.setSubject("Welcome to Banking Platform");
            request.setPriority(NotificationPriority.HIGH);
            request.setTemplateName("welcome-email");

            Map<String, String> parameters = new HashMap<>();
            parameters.put("firstName", firstName != null ? firstName : "Customer");
            request.setTemplateParameters(parameters);

            notificationService.sendNotification(request);
            logger.info("Welcome notification sent for user: {}", userId);
        }
    }

    private void handleAccountCreatedEvent(Map<String, Object> event) {
        Long userId = getLongValue(event, "userId");
        String accountNumber = getStringValue(event, "accountNumber");
        String accountType = getStringValue(event, "accountType");

        if (userId != null && accountNumber != null) {
            // This would typically fetch user email from user service
            // For now, we'll create a placeholder notification
            SendNotificationRequest request = new SendNotificationRequest();
            request.setUserId(userId);
            request.setType(NotificationType.ACCOUNT_CREATED);
            request.setChannel(NotificationChannel.EMAIL);
            request.setRecipient("user@example.com"); // Would be fetched from user service
            request.setSubject("New Account Created");
            request.setPriority(NotificationPriority.NORMAL);
            request.setTemplateName("account-created");

            Map<String, String> parameters = new HashMap<>();
            parameters.put("accountNumber", accountNumber);
            parameters.put("accountType", accountType);
            request.setTemplateParameters(parameters);

            notificationService.sendNotification(request);
            logger.info("Account creation notification sent for user: {}", userId);
        }
    }

    private void handleBalanceUpdatedEvent(Map<String, Object> event) {
        String accountNumber = getStringValue(event, "accountNumber");
        String operation = getStringValue(event, "operation");
        Double amount = getDoubleValue(event, "amount");

        if (accountNumber != null && "DEBIT".equals(operation) && amount != null && amount > 1000) {
            // Send high-value transaction alert
            // This would typically fetch user details from account service
            logger.info("High-value transaction alert for account: {} amount: {}", accountNumber, amount);
        }
    }

    private void handleAccountStatusUpdatedEvent(Map<String, Object> event) {
        String accountNumber = getStringValue(event, "accountNumber");
        String status = getStringValue(event, "status");

        if ("SUSPENDED".equals(status) || "CLOSED".equals(status)) {
            logger.info("Account status change notification for account: {} status: {}", accountNumber, status);
        }
    }

    private void handleTransactionCompletedEvent(Map<String, Object> event) {
        String transactionReference = getStringValue(event, "transactionReference");
        String type = getStringValue(event, "type");
        Double amount = getDoubleValue(event, "amount");

        if (transactionReference != null && amount != null && amount > 5000) {
            // Send high-value transaction completion notification
            logger.info("High-value transaction completed: {} amount: {}", transactionReference, amount);
        }
    }

    private void handleTransactionFailedEvent(Map<String, Object> event) {
        String transactionReference = getStringValue(event, "transactionReference");
        String reason = getStringValue(event, "reason");

        if (transactionReference != null) {
            logger.info("Transaction failed notification: {} reason: {}", transactionReference, reason);
        }
    }

    private String determineEventType(Map<String, Object> event) {
        // Simple event type determination based on event structure
        if (event.containsKey("username") && event.containsKey("email")) {
            return "USER_CREATED";
        } else if (event.containsKey("accountNumber") && event.containsKey("accountType")) {
            return "ACCOUNT_CREATED";
        } else if (event.containsKey("accountNumber") && event.containsKey("newBalance")) {
            return "BALANCE_UPDATED";
        } else if (event.containsKey("accountNumber") && event.containsKey("status")) {
            return "ACCOUNT_STATUS_UPDATED";
        } else if (event.containsKey("transactionReference") && event.containsKey("completedAt")) {
            return "TRANSACTION_COMPLETED";
        } else if (event.containsKey("transactionReference") && event.containsKey("reason")) {
            return "TRANSACTION_FAILED";
        }
        return "UNKNOWN";
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }
}
