package com.co.eatupapi.utils.commercial.customerDiscount.exceptions;

import com.co.eatupapi.controllers.commercial.customerDiscount.CustomerDiscountController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;

@RestControllerAdvice(assignableTypes = CustomerDiscountController.class)
public class CustomerDiscountGlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<CustomerDiscountApiErrorResponse> handleValidation(ValidationException ex) {
        return build(ex.getMessage(), ex.getErrorCode().name(), ex.getTimestamp(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<CustomerDiscountApiErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return build(ex.getMessage(), ex.getErrorCode().name(), ex.getTimestamp(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<CustomerDiscountApiErrorResponse> handleBusiness(BusinessException ex) {
        return build(ex.getMessage(), ex.getErrorCode().name(), ex.getTimestamp(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CustomerDiscountApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().isEmpty()
                ? "Error de validación"
                : ex.getBindingResult().getFieldErrors().getFirst().getDefaultMessage();
        return build(message, CustomerDiscountErrorCode.VALIDATION_ERROR.name(), LocalDateTime.now(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<CustomerDiscountApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "El id '" + ex.getValue() + "' no es un UUID válido.";
        return build(message, CustomerDiscountErrorCode.VALIDATION_ERROR.name(), LocalDateTime.now(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CustomerDiscountApiErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        return build("El cuerpo de la solicitud es inválido o está mal formado.",
                CustomerDiscountErrorCode.VALIDATION_ERROR.name(), LocalDateTime.now(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CustomerDiscountApiErrorResponse> handleUnexpected(Exception ex) {
        return build("Error interno del servidor", CustomerDiscountErrorCode.INTERNAL_SERVER_ERROR.name(),
                LocalDateTime.now(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<CustomerDiscountApiErrorResponse> build(String message, String errorCode,
                                                                   LocalDateTime timestamp, HttpStatus status) {
        return ResponseEntity.status(status)
                .body(new CustomerDiscountApiErrorResponse(message, errorCode, timestamp, status.value()));
    }
}