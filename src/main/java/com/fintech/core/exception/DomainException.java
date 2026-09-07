package com.fintech.core.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;


@Getter
public abstract class DomainException extends RuntimeException {

    private final HttpStatus status;
    private final String messageKey;
    private final Object[] args;

    protected DomainException(String messageKey, HttpStatus status, Object... args) {
        super(messageKey);
        this.messageKey = messageKey;
        this.status = status;
        this.args = args != null ? args : new Object[0];
    }
}
