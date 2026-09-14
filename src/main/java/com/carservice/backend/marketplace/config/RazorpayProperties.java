package com.carservice.backend.marketplace.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "razorpay")
public class RazorpayProperties {

    /**
     * Whether Razorpay payment gateway is enabled.
     */
    private boolean enabled = false;

    /**
     * Razorpay Key ID (public / publishable identifier).
     */
    private String keyId = "";

    /**
     * Razorpay Key Secret (private server-side secret).
     */
    private String keySecret = "";

    /**
     * Razorpay Webhook Secret for validating incoming webhook signatures.
     */
    private String webhookSecret = "";

    /**
     * Whether to use mock / simulated gateway responses (useful for automated testing without live network).
     */
    private boolean mockGateway = false;

    public RazorpayProperties() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getKeySecret() {
        return keySecret;
    }

    public void setKeySecret(String keySecret) {
        this.keySecret = keySecret;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public boolean isMockGateway() {
        return mockGateway;
    }

    public void setMockGateway(boolean mockGateway) {
        this.mockGateway = mockGateway;
    }

    /**
     * Checks whether Razorpay credentials are fully configured and ready for transactions.
     * Razorpay is considered unavailable when:
     * - enabled = false
     * - keyId is null or blank
     * - keySecret is null or blank
     */
    public boolean isConfigured() {
        return enabled
                && keyId != null && !keyId.trim().isEmpty()
                && keySecret != null && !keySecret.trim().isEmpty();
    }

    /**
     * Checks whether webhook secret is configured.
     */
    public boolean isWebhookConfigured() {
        return webhookSecret != null && !webhookSecret.trim().isEmpty();
    }
}
