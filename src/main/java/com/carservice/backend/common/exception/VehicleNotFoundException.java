package com.carservice.backend.common.exception;

public class VehicleNotFoundException extends ResourceNotFoundException {

    public VehicleNotFoundException(String message) {
        super(message);
    }
}
