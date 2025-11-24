package com.example.financeapp.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {

    private final ApiErrorCode code;
    private final HttpStatus status;

    public ApiException(ApiErrorCode code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public ApiException(ApiErrorCode code, String message) {
        this(code, message, HttpStatus.BAD_REQUEST);
    }
}

