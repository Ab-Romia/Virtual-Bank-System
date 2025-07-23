package com.virtualbank.account.exception;

public class InvalidCreationException extends RuntimeException {
    public InvalidCreationException(String message) {
        super(message);
    }
}
