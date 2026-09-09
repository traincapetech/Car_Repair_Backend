package com.carservice.backend.common.exception;

public class ServiceCatalogNotFoundException extends ResourceNotFoundException {

    public ServiceCatalogNotFoundException(String message) {
        super(message);
    }
}
