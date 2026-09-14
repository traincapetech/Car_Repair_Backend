package com.carservice.backend.marketplace.exception;

public class PaymentGatewayUnavailableException extends RuntimeException {

    public PaymentGatewayUnavailableException(String message) {
        super(message);
    }
}
