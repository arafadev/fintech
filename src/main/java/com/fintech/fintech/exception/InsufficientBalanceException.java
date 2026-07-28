package com.fintech.fintech.exception;

import org.springframework.http.HttpStatus;

public class InsufficientBalanceException extends DomainException {

    public InsufficientBalanceException(String messageKey, Object... args) {
        super(messageKey, HttpStatus.BAD_REQUEST, args);
    }
}
