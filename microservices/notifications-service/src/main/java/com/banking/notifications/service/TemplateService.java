package com.banking.notifications.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
public class TemplateService {
    private static final Logger logger = LoggerFactory.getLogger(TemplateService.class);

    @Autowired
    private TemplateEngine templateEngine;

    public String processTemplate(String templateName, Map<String, String> parameters) {
        try {
            Context context = new Context();
            parameters.forEach(context::setVariable);
            
            return templateEngine.process(templateName, context);
        } catch (Exception e) {
            logger.error("Failed to process template {}: {}", templateName, e.getMessage());
            return "Template processing failed";
        }
    }

    public String processHtmlTemplate(String templateName, Map<String, String> parameters) {
        try {
            Context context = new Context();
            parameters.forEach(context::setVariable);
            
            String htmlTemplateName = templateName + "-html";
            return templateEngine.process(htmlTemplateName, context);
        } catch (Exception e) {
            logger.error("Failed to process HTML template {}: {}", templateName, e.getMessage());
            return processTemplate(templateName, parameters); // Fallback to text template
        }
    }
}
