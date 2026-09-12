package com.carservice.backend.marketplace.payment;

import java.math.BigDecimal;

public class PaymentRefund {

    private final String refundId;
    private final String paymentId;
    private final BigDecimal amount;
    private final String currency;
    private final String status;

    public PaymentRefund(String refundId, String paymentId, BigDecimal amount, String currency, String status) {
        this.refundId = refundId;
        this.paymentId = paymentId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
    }

    public String getRefundId() {
        return refundId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getStatus() {
        return status;
    }
}
