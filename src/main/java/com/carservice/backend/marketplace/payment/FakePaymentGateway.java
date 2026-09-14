package com.carservice.backend.marketplace.payment;

import com.carservice.backend.marketplace.exception.PaymentGatewayException;
import com.carservice.backend.marketplace.exception.PaymentGatewayUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

@Component("fakePaymentGateway")
@ConditionalOnProperty(name = "razorpay.mock-gateway", havingValue = "true")
public class FakePaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(FakePaymentGateway.class);

    private boolean available = true;
    private String keyId = "rzp_test_fake123";
    private String keySecret = "secret_fake_key_123";
    private String webhookSecret = "webhook_secret_fake_123";

    private boolean simulateOrderFailure = false;
    private boolean simulateSignatureFailure = false;
    private boolean simulateRefundFailure = false;
    private String simulatedRefundStatus = "processed";

    public FakePaymentGateway() {
    }

    public FakePaymentGateway(String keyId, String keySecret) {
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.available = true;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public String getKeyId() {
        return keyId;
    }

    @Override
    public PaymentOrder createOrder(BigDecimal amount, String currency, String receiptId) {
        if (!available) {
            throw new PaymentGatewayUnavailableException("Razorpay payment gateway is currently unavailable.");
        }
        if (simulateOrderFailure) {
            throw new PaymentGatewayException("Simulated Razorpay order creation failure");
        }
        log.info("[FakeGateway] Creating simulated order for receipt: {}, amount: {} {}", receiptId, amount, currency);
        String orderId = "order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        return new PaymentOrder(orderId, amount, currency != null ? currency : "INR", receiptId, "created");
    }

    @Override
    public boolean verifySignature(String orderId, String paymentId, String signature) {
        if (!available) {
            return false;
        }
        if (simulateSignatureFailure) {
            return false;
        }
        if (orderId == null || orderId.isBlank()
                || paymentId == null || paymentId.isBlank()
                || signature == null || signature.isBlank()) {
            return false;
        }

        try {
            String data = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = HexFormat.of().formatHex(rawHmac);

            boolean matches = MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    signature.trim().getBytes(StandardCharsets.UTF_8)
            );

            // In fake mode, also accept explicit test signatures if secret hasn't been explicitly configured for a strict test
            if (!matches && (signature.startsWith("sig_test_valid") || signature.equals("valid_test_signature"))) {
                return true;
            }

            return matches;
        } catch (Exception e) {
            log.error("[FakeGateway] Error verifying payment signature: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        if (payload == null || signature == null || webhookSecret == null || webhookSecret.isBlank()) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] rawHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = HexFormat.of().formatHex(rawHmac);

            boolean matches = MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    signature.trim().getBytes(StandardCharsets.UTF_8)
            );

            if (!matches && (signature.startsWith("whsec_valid") || signature.equals("valid_webhook_sig"))) {
                return true;
            }

            return matches;
        } catch (Exception e) {
            log.error("[FakeGateway] Error verifying webhook signature: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public PaymentRefund initiateRefund(String paymentId, BigDecimal amount, String reason) {
        if (!available) {
            throw new PaymentGatewayUnavailableException("Razorpay payment gateway is currently unavailable.");
        }
        if (simulateRefundFailure) {
            throw new PaymentGatewayException("Simulated Razorpay refund initiation failure");
        }
        log.info("[FakeGateway] Initiating simulated refund for payment: {}, amount: {}, reason: {}", paymentId, amount, reason);
        String refundId = "rfnd_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        return new PaymentRefund(refundId, paymentId, amount, "INR", simulatedRefundStatus);
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getKeySecret( ) {
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

    public void setSimulateOrderFailure(boolean simulateOrderFailure) {
        this.simulateOrderFailure = simulateOrderFailure;
    }

    public void setSimulateSignatureFailure(boolean simulateSignatureFailure) {
        this.simulateSignatureFailure = simulateSignatureFailure;
    }

    public void setSimulateRefundFailure(boolean simulateRefundFailure) {
        this.simulateRefundFailure = simulateRefundFailure;
    }

    public void setSimulatedRefundStatus(String simulatedRefundStatus) {
        this.simulatedRefundStatus = simulatedRefundStatus;
    }
}
