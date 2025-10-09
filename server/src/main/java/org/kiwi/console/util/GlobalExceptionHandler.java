package org.kiwi.console.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {


    // Handler for your specific BusinessException
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<?>> handleBusinessException(BusinessException ex) {
        log.warn("Business exception: {}", ex.getMessage()); // Log as warning
        
        Result<?> failureResult = Result.failure(ex.getErrorCode(), ex.getArgs());
        HttpStatus status = HttpStatus.BAD_REQUEST; // Default to 400

        if (ex.getErrorCode() == ErrorCode.AUTHENTICATION_FAILED) {
            status = HttpStatus.UNAUTHORIZED; // 401
        }
        
        return new ResponseEntity<>(failureResult, status);
    }

    // Handler for all other unexpected exceptions
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // Ensures a 500 status code
    public ResponseEntity<Result<?>> handleGenericException(Exception ex) {
        log.error("An unhandled exception occurred", ex); // Log as error with stack trace
        
        // Create a generic error response for the client
        Result<?> failureResult = Result.failure(ErrorCode.INTERNAL_SERVER_ERROR);
        
        return new ResponseEntity<>(failureResult, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}