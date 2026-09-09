package com.carservice.backend.common.exception;

public class ServiceCatalogAlreadyExistsException extends RuntimeException {

    public ServiceCatalogAlreadyExistsException(String message) {
        super(message);
    }
}
