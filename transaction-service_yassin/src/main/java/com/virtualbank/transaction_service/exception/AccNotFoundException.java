package com.virtualbank.transaction_service.exception;

public class AccNotFoundException extends RuntimeException {
    public AccNotFoundException(String message) {
        super(message);
    }
}
