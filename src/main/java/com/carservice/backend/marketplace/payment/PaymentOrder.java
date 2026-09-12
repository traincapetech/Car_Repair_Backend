package com.carservice.backend.marketplace.payment;

import java.math.BigDecimal;

public class PaymentOrder {

    private final String orderId;
    private final BigDecimal amount;
    private final String currency;
    private final String receipt;
    private final String status;

    public PaymentOrder(String orderId, BigDecimal amount, String currency, String receipt, String status) {
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
        this.receipt = receipt;
        this.status = status;
    }

    public String getOrderId() {
        return orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getReceipt() {
        return receipt;
    }

    public String getStatus() {
        return status;
    }
}
