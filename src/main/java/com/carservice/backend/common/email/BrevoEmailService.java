package com.carservice.backend.common.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

@Service
public class BrevoEmailService {

    private static final Logger log = LoggerFactory.getLogger(BrevoEmailService.class);

    @Value("${brevo.api-key:}")
    private String apiKey;

    @Value("${brevo.sender.email:no-reply@carservice.com}")
    private String senderEmail;

    @Value("${brevo.sender.name:Addior Mechanics}")
    private String senderName;

    @Value("${brevo.api-url:https://api.brevo.com/v3/smtp/email}")
    private String apiUrl;

    @Value("${admin.default.email:admin@carservice.com}")
    private String adminEmail;

    private final EmailTemplateBuilder templateBuilder;
    private final RestTemplate restTemplate;

    @org.springframework.beans.factory.annotation.Autowired
    public BrevoEmailService(EmailTemplateBuilder templateBuilder) {
        this.templateBuilder = templateBuilder;
        this.restTemplate = new RestTemplate();
    }

    // Constructor for testing with mock RestTemplate
    public BrevoEmailService(EmailTemplateBuilder templateBuilder, RestTemplate restTemplate,
                             String apiKey, String senderEmail, String senderName, String apiUrl, String adminEmail) {
        this.templateBuilder = templateBuilder;
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.apiUrl = apiUrl;
        this.adminEmail = adminEmail;
    }

    /**
     * Core transactional email sender using Brevo's REST API.
     * Guaranteed non-blocking: Outages or errors will NEVER throw an uncaught exception
     * to the calling business method.
     */
    public boolean sendEmail(String toEmail, String toName, String subject, String htmlContent) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            log.warn("[BrevoEmailService] Cannot send email: recipient address is empty");
            return false;
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.info("[BrevoEmailService] Brevo API key unconfigured. Email simulated: to='{}', subject='{}'", toEmail, subject);
            return true;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey.trim());
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            Map<String, Object> payload = new HashMap<>();
            payload.put("sender", Map.of(
                    "name", senderName != null && !senderName.isBlank() ? senderName : "Addior Mechanics",
                    "email", senderEmail != null && !senderEmail.isBlank() ? senderEmail : "no-reply@carservice.com"
            ));

            Map<String, String> recipient = new HashMap<>();
            recipient.put("email", toEmail.trim());
            if (toName != null && !toName.trim().isEmpty()) {
                recipient.put("name", toName.trim());
            }
            payload.put("to", Collections.singletonList(recipient));
            payload.put("subject", subject);
            payload.put("htmlContent", htmlContent);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            log.info("[BrevoEmailService] Dispatching transactional email: to='{}', subject='{}'", toEmail, subject);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("[BrevoEmailService] Successfully sent email to '{}', status={}", toEmail, response.getStatusCode());
                return true;
            } else {
                log.warn("[BrevoEmailService] Provider returned non-2xx status: {} body={}", response.getStatusCode(), response.getBody());
                return false;
            }
        } catch (RestClientResponseException ex) {
            log.error("[BrevoEmailService] Provider error dispatching email to '{}': status={} body={}",
                    toEmail, ex.getStatusCode(), ex.getResponseBodyAsString());
            return false;
        } catch (Exception ex) {
            log.error("[BrevoEmailService] Network/system failure sending email to '{}': {}", toEmail, ex.getMessage());
            return false;
        }
    }

    /**
     * Send email using a predefined Brevo template ID.
     */
    public boolean sendTemplateEmail(String toEmail, String toName, Long templateId, Map<String, Object> params) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            return false;
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.info("[BrevoEmailService] Brevo API key unconfigured. Template simulated: to='{}', templateId={}", toEmail, templateId);
            return true;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey.trim());
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            Map<String, Object> payload = new HashMap<>();
            payload.put("sender", Map.of(
                    "name", senderName,
                    "email", senderEmail
            ));
            payload.put("to", Collections.singletonList(Map.of("email", toEmail.trim(), "name", toName != null ? toName.trim() : "")));
            payload.put("templateId", templateId);
            if (params != null) {
                payload.put("params", params);
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception ex) {
            log.error("[BrevoEmailService] Failed template email to '{}': {}", toEmail, ex.getMessage());
            return false;
        }
    }

    public boolean sendWelcomeEmail(String toEmail, String toName) {
        String html = templateBuilder.buildWelcomeEmail(toName != null ? toName : "Customer");
        return sendEmail(toEmail, toName, "Welcome to Addior Mechanics Pro!", html);
    }

    public boolean sendVerificationEmail(String toEmail, String toName, String verificationUrl) {
        String html = templateBuilder.buildVerificationEmail(toName != null ? toName : "Customer", verificationUrl);
        return sendEmail(toEmail, toName, "Verify Your Email - Addior Mechanics Pro", html);
    }

    public boolean sendPasswordResetEmail(String toEmail, String toName, String resetUrl) {
        String html = templateBuilder.buildPasswordResetEmail(toName != null ? toName : "User", resetUrl);
        return sendEmail(toEmail, toName, "Password Reset Request - Addior Mechanics Pro", html);
    }

    public boolean sendBookingConfirmationEmail(String toEmail, String toName, String bookingRef, String date, String timeSlot, BigDecimal amount) {
        String html = templateBuilder.buildBookingConfirmationEmail(toName, bookingRef, date, timeSlot, amount);
        return sendEmail(toEmail, toName, "Booking Confirmation #" + bookingRef + " - Addior Mechanics Pro", html);
    }

    public boolean sendBookingStatusEmail(String toEmail, String toName, String bookingRef, String status, String details) {
        String html = templateBuilder.buildBookingStatusEmail(toName, bookingRef, status, details);
        return sendEmail(toEmail, toName, "Booking Status Update [" + status + "] #" + bookingRef, html);
    }

    public boolean sendWorkshopApprovalEmail(String toEmail, String businessName) {
        String html = templateBuilder.buildWorkshopApprovalEmail(businessName);
        return sendEmail(toEmail, businessName, "Workshop Application Approved - Addior Mechanics Partner Network", html);
    }

    public boolean sendWorkshopRejectionEmail(String toEmail, String businessName, String reason) {
        String html = templateBuilder.buildWorkshopRejectionEmail(businessName, reason);
        return sendEmail(toEmail, businessName, "Workshop Application Update - Addior Mechanics Partner Network", html);
    }

    public boolean sendAdminNotificationEmail(String subject, String message) {
        String recipient = adminEmail != null && !adminEmail.isBlank() ? adminEmail : "admin@carservice.com";
        String html = templateBuilder.buildAdminNotificationEmail(subject, message);
        return sendEmail(recipient, "System Administrator", subject, html);
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public String getSenderName() {
        return senderName;
    }
}
