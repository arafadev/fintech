package com.fintech.core.exception;

import com.fintech.core.common.i18n.Messages;
import com.fintech.core.response.Response;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final Messages messages;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Response<?>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (first, second) -> first
                ));

        return ResponseEntity.badRequest()
                .body(Response.fail(
                        HttpStatus.BAD_REQUEST.value(),
                        messages.get("error.validation_failed"),
                        errors
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class) // for Request param validation 
    public ResponseEntity<Response<?>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        violation -> violation.getMessage(),
                        (first, second) -> first
                ));

        return ResponseEntity.badRequest()
                .body(Response.fail(
                        HttpStatus.BAD_REQUEST.value(),
                        messages.get("error.validation_failed"),
                        errors
                ));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<Response<?>> handleDomainException(DomainException ex) {
        HttpStatus status = ex.getStatus();
        String localized = messages.get(ex.getMessageKey(), ex.getArgs());
        return ResponseEntity.status(status)
                .body(Response.fail(status.value(), localized));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Response<?>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("Upload rejected: maximum size exceeded");
        return ResponseEntity.badRequest()
                .body(Response.fail(
                        HttpStatus.BAD_REQUEST.value(),
                        messages.get("error.upload.file_too_large", "5")
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response<?>> handleAllUnknownExceptions(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Response.fail(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        messages.get("error.unexpected")
                ));
    }
}
