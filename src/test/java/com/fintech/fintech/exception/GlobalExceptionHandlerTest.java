package com.fintech.fintech.exception;

import com.fintech.fintech.common.i18n.Messages;
import com.fintech.fintech.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    Messages messages;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler(messages);
        when(messages.get(anyString(), any(Object[].class))).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            if ("error.validation_failed".equals(key)) {
                return "Validation failed";
            }
            return key;
        });
    }

    @Test
    void handleValidation_shouldReturn400_not500() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "amount", "must not be null"));

        Method method = DummyController.class.getDeclaredMethod("create", Object.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<Response<?>> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatusCode());
        assertEquals("Validation failed", response.getBody().getMessage());
        assertFalse(response.getBody().isSuccess());
        assertNotEquals(500, response.getBody().getStatusCode());
    }

    static class DummyController {
        @SuppressWarnings("unused")
        public void create(Object body) {
        }
    }
}
