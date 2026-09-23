package com.fintech.core.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends DomainException {

    public ConflictException(String messageKey, Object... args) {
        super(messageKey, HttpStatus.CONFLICT, args);
    }
}
