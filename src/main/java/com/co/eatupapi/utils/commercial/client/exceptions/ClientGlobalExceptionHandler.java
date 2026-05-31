package com.co.eatupapi.utils.commercial.client.exceptions;

import com.co.eatupapi.controllers.commercial.client.ClientController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = ClientController.class)
public class ClientGlobalExceptionHandler {

    @ExceptionHandler(ClientNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleClientNotFound(ClientNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), ex.getCode());
    }

    @ExceptionHandler(ClientValidationException.class)
    public ResponseEntity<Map<String, Object>> handleClientValidation(ClientValidationException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.getCode());
    }

    @ExceptionHandler(ClientBusinessException.class)
    public ResponseEntity<Map<String, Object>> handleClientBusinessException(ClientBusinessException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.getCode());
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApiException(ApiException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.getCode());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid request body. Verify JSON format and field data types",
                "CLIENT_INVALID_BODY"
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid value for parameter '" + ex.getName() + "': " + ex.getValue(),
                "CLIENT_INVALID_FORMAT"
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String rawMessage = ex.getMostSpecificCause().getMessage() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        String normalized = rawMessage.toLowerCase();

        if (normalized.contains("email")) {
            return buildResponse(HttpStatus.BAD_REQUEST, "A client with the same email already exists", "CLIENT_DUPLICATE_EMAIL");
        }
        if (normalized.contains("document_number")) {
            return buildResponse(HttpStatus.BAD_REQUEST, "A client with the same document number already exists", "CLIENT_DUPLICATE_DOCUMENT");
        }
        if (normalized.contains("document_type")) {
            return buildResponse(HttpStatus.BAD_REQUEST, "The provided documentTypeId does not exist or is not valid", "CLIENT_INVALID_DOCUMENT_TYPE");
        }
        if (normalized.contains("city")) {
            return buildResponse(HttpStatus.BAD_REQUEST, "The provided cityId does not exist or is not valid", "CLIENT_INVALID_CITY");
        }
        if (normalized.contains("tax_regime")) {
            return buildResponse(HttpStatus.BAD_REQUEST, "The provided taxRegimeId does not exist or is not valid", "CLIENT_INVALID_TAX_REGIME");
        }
        if (normalized.contains("assigned_seller")) {
            return buildResponse(HttpStatus.BAD_REQUEST, "The provided assignedSellerId does not exist or is not valid", "CLIENT_INVALID_SELLER");
        }

        return buildResponse(HttpStatus.BAD_REQUEST, "The request violates a database data integrity rule", "CLIENT_DATA_INTEGRITY_ERROR");
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message, String code) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("code", code);
        return new ResponseEntity<>(body, status);
    }
}
