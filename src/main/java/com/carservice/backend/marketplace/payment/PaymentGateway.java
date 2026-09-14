package com.carservice.backend.marketplace.payment;

import java.math.BigDecimal;

public interface PaymentGateway {

    PaymentOrder createOrder(BigDecimal amount, String currency, String receiptId);

    boolean verifySignature(String orderId, String paymentId, String signature);

    PaymentRefund initiateRefund(String paymentId, BigDecimal amount, String reason);

    boolean verifyWebhookSignature(String payload, String signature);

    boolean isAvailable();

    String getKeyId();
}
