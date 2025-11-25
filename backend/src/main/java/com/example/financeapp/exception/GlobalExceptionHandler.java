package com.example.financeapp.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Template response error
    private Map<String, Object> buildErrorBody(ApiErrorCode code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("code", code.name());
        body.put("message", message);
        body.put("timestamp", new Date());
        return body;
    }

    // 1️⃣ Lỗi tùy chỉnh của hệ thống (ApiException)
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> handleApiException(ApiException ex) {
        String message = (ex.getMessage() == null || ex.getMessage().isBlank())
                ? "Lỗi không xác định"
                : ex.getMessage();

        Map<String, Object> body = buildErrorBody(ex.getCode(), message);
        HttpStatus status = ex.getStatus() != null ? ex.getStatus() : HttpStatus.BAD_REQUEST;

        return ResponseEntity.status(status).body(body);
    }

    // 2️⃣ Lỗi validate @Valid trong DTO
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> body = buildErrorBody(ApiErrorCode.VALIDATION_ERROR, "Dữ liệu không hợp lệ");

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err ->
                fieldErrors.put(err.getField(), err.getDefaultMessage())
        );

        body.put("errors", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    // 3️⃣ Lỗi validate @PathVariable, @RequestParam
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleConstraint(ConstraintViolationException ex) {
        Map<String, Object> body = buildErrorBody(ApiErrorCode.VALIDATION_ERROR, "Dữ liệu không hợp lệ");

        List<String> details = new ArrayList<>();
        ex.getConstraintViolations().forEach(v -> details.add(v.getMessage()));
        body.put("errors", details);

        return ResponseEntity.badRequest().body(body);
    }

    // 4️⃣ Lỗi thường gặp khác
    @ExceptionHandler({
            IllegalArgumentException.class,
            IllegalStateException.class
    })
    public ResponseEntity<?> handleIllegal(RuntimeException ex) {
        ex.printStackTrace();

        Map<String, Object> body = buildErrorBody(
                ApiErrorCode.INTERNAL_ERROR,
                ex.getMessage() != null ? ex.getMessage() : "Lỗi xử lý dữ liệu"
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    // 5️⃣ Lỗi chung chung (fallback)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleOther(Exception ex) {
        ex.printStackTrace();

        Map<String, Object> body = buildErrorBody(
                ApiErrorCode.INTERNAL_ERROR,
                "Lỗi hệ thống, vui lòng thử lại sau"
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}

