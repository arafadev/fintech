package com.fintech.fintech.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;


@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Response<T> {

    private boolean success;
    private int statusCode;
    private String message;
    private T data;
    private Map<String, Serializable> meta;
    private Instant timestamp;

    public static <T> Response<T> ok(String message) {
        return ok(message, null, null);
    }

    public static <T> Response<T> ok(String message, T data) {
        return ok(message, data, null);
    }

    public static <T> Response<T> ok(String message, T data, Map<String, Serializable> meta) {
        return Response.<T>builder()
                .success(true)
                .statusCode(200)
                .message(message)
                .data(data)
                .meta(meta)
                .timestamp(Instant.now())
                .build();
    }

    public static <T> Response<T> fail(int statusCode, String message) {
        return fail(statusCode, message, null);
    }

    public static <T> Response<T> fail(int statusCode, String message, T data) {
        return Response.<T>builder()
                .success(false)
                .statusCode(statusCode)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }
}
