package com.fintech.fintech.exception;

import org.springframework.http.HttpStatus;

public class InvalidTransactionException extends DomainException {

    public InvalidTransactionException(String messageKey, Object... args) {
        super(messageKey, HttpStatus.BAD_REQUEST, args);
    }
}
