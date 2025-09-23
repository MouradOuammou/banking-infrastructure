package com.banking.notifications.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${banking.mail.from}")
    private String fromEmail;

    @CircuitBreaker(name = "email-service", fallbackMethod = "sendEmailFallback")
    @Retry(name = "email-service")
    public String sendEmail(String to, String subject, String textContent, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);

            if (htmlContent != null && !htmlContent.isEmpty()) {
                helper.setText(textContent, htmlContent);
            } else {
                helper.setText(textContent);
            }

            mailSender.send(message);

            String messageId = UUID.randomUUID().toString();
            logger.info("Email sent successfully to {} with message ID: {}", to, messageId);
            return messageId;

        } catch (MessagingException e) {
            logger.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    public String sendEmailFallback(String to, String subject, String textContent, String htmlContent, Exception ex) {
        logger.error("Email service fallback triggered for {}: {}", to, ex.getMessage());
        return null;
    }
}
