package com.carservice.backend.common.exception;

public class BookingNotFoundException extends ResourceNotFoundException {

    public BookingNotFoundException(String message) {
        super(message);
    }
}
