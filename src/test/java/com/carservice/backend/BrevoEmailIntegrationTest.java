package com.carservice.backend;

import com.carservice.backend.common.email.BrevoEmailService;
import com.carservice.backend.common.email.EmailTemplateBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class BrevoEmailIntegrationTest {

    @Autowired
    private BrevoEmailService brevoEmailService;

    @Autowired
    private EmailTemplateBuilder templateBuilder;

    @Test
    @DisplayName("BrevoEmailService loads default configuration without throwing")
    void testConfigurationLoading() {
        assertNotNull(brevoEmailService);
        assertNotNull(brevoEmailService.getSenderEmail());
        assertNotNull(brevoEmailService.getSenderName());
    }

    @Test
    @DisplayName("BrevoEmailService handles missing API key gracefully without error")
    void testMissingApiKeyHandling() {
        // In test profile, BREVO_API_KEY is empty
        boolean sent = brevoEmailService.sendEmail("customer@test.com", "Test Customer", "Test Subject", "<p>Hello</p>");
        assertTrue(sent, "Missing API key in dev/test should simulate successful send without throwing exception");
    }

    @Test
    @DisplayName("Successful Brevo API response returns true")
    void testSuccessfulEmailRequest() {
        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        when(mockRestTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"messageId\":\"<msg-123>\"}", HttpStatus.OK));

        BrevoEmailService testService = new BrevoEmailService(
                templateBuilder,
                mockRestTemplate,
                "xkeysib-dummy-test-key-12345",
                "no-reply@carservice.com",
                "Addior Mechanics",
                "https://api.brevo.com/v3/smtp/email",
                "admin@carservice.com"
        );

        boolean result = testService.sendBookingConfirmationEmail(
                "customer@test.com",
                "John Doe",
                "BK-2026-9999",
                "2026-10-01",
                "10:00 - 11:00",
                new BigDecimal("1499.00")
        );

        assertTrue(result);
        verify(mockRestTemplate, times(1)).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    @DisplayName("Brevo 5xx server error is handled non-blocking without throwing")
    void testBrevoServerErrorHandling() {
        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        when(mockRestTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Brevo gateway down"));

        BrevoEmailService testService = new BrevoEmailService(
                templateBuilder,
                mockRestTemplate,
                "xkeysib-dummy-test-key-12345",
                "no-reply@carservice.com",
                "Addior Mechanics",
                "https://api.brevo.com/v3/smtp/email",
                "admin@carservice.com"
        );

        // Crucial: Must return false and NEVER throw an exception
        assertDoesNotThrow(() -> {
            boolean result = testService.sendEmail("customer@test.com", "John Doe", "Test", "<p>Content</p>");
            assertFalse(result, "When Brevo is down, sendEmail must return false");
        });
    }

    @Test
    @DisplayName("Brevo 401/403 unauthorized client error is handled non-blocking")
    void testBrevoClientErrorHandling() {
        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        when(mockRestTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Invalid API Key"));

        BrevoEmailService testService = new BrevoEmailService(
                templateBuilder,
                mockRestTemplate,
                "xkeysib-invalid-key",
                "no-reply@carservice.com",
                "Addior Mechanics",
                "https://api.brevo.com/v3/smtp/email",
                "admin@carservice.com"
        );

        assertDoesNotThrow(() -> {
            boolean result = testService.sendWorkshopApprovalEmail("workshop@test.com", "Elite Auto Hub");
            assertFalse(result);
        });
    }

    @Test
    @DisplayName("EmailTemplateBuilder produces valid HTML containing all required fields")
    void testEmailTemplateGeneration() {
        String welcomeHtml = templateBuilder.buildWelcomeEmail("Alice Smith");
        assertTrue(welcomeHtml.contains("Alice Smith"));
        assertTrue(welcomeHtml.contains("Addior"));

        String resetHtml = templateBuilder.buildPasswordResetEmail("Bob", "https://carservice.com/reset?token=xyz");
        assertTrue(resetHtml.contains("https://carservice.com/reset?token=xyz"));

        String bookingHtml = templateBuilder.buildBookingConfirmationEmail("Charlie", "BK-123", "2026-09-25", "11:00 - 12:00", new BigDecimal("2999.00"));
        assertTrue(bookingHtml.contains("BK-123"));
        assertTrue(bookingHtml.contains("2999.00"));

        String rejectionHtml = templateBuilder.buildWorkshopRejectionEmail("FastFix Garage", "Missing trade license");
        assertTrue(rejectionHtml.contains("FastFix Garage"));
        assertTrue(rejectionHtml.contains("Missing trade license"));
    }
}
