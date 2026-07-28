package com.fintech.fintech.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends DomainException {

    public NotFoundException(String messageKey, Object... args) {
        super(messageKey, HttpStatus.NOT_FOUND, args);
    }
}
