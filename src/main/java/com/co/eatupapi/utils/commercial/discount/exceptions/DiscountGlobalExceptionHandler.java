package com.co.eatupapi.utils.commercial.discount.exceptions;

import com.co.eatupapi.controllers.commercial.discount.DiscountController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice(assignableTypes = DiscountController.class)
public class DiscountGlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<DiscountApiErrorResponse> handleValidation(ValidationException ex) {
        return build(ex.getMessage(), ex.getErrorCode().name(), ex.getTimestamp(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<DiscountApiErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return build(ex.getMessage(), ex.getErrorCode().name(), ex.getTimestamp(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<DiscountApiErrorResponse> handleBusiness(BusinessException ex) {
        return build(ex.getMessage(), ex.getErrorCode().name(), ex.getTimestamp(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<DiscountApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().isEmpty()
                ? "Error de validacion"
                : ex.getBindingResult().getFieldErrors().getFirst().getDefaultMessage();
        return build(message, DiscountErrorCode.VALIDATION_ERROR.name(), LocalDateTime.now(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<DiscountApiErrorResponse> handleUnexpected(Exception ex) {
        return build("Error interno del servidor", DiscountErrorCode.INTERNAL_SERVER_ERROR.name(),
                LocalDateTime.now(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<DiscountApiErrorResponse> build(String message, String errorCode,
                                                           LocalDateTime timestamp, HttpStatus status) {
        return ResponseEntity.status(status)
                .body(new DiscountApiErrorResponse(message, errorCode, timestamp, status.value()));
    }
}