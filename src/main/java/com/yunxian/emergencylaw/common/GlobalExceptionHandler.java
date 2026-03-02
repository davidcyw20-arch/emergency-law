package com.yunxian.emergencylaw.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ApiResponse<String> handleDataIntegrity(DataIntegrityViolationException e) {
        log.error("DB error", e);
        String msg = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage();
        return ApiResponse.fail(msg == null ? "db error" : msg);
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<String> handleAny(Exception e) {
        log.error("Unhandled error", e);
        return ApiResponse.fail(e.getMessage() == null ? "internal error" : e.getMessage());
    }
}
