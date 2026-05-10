package com.co.eatupapi.utils.commercial.customerDiscount.exceptions;

import java.time.LocalDateTime;

public abstract class ApiException extends RuntimeException {

    private final CustomerDiscountErrorCode errorCode;
    private final LocalDateTime timestamp;

    protected ApiException(CustomerDiscountErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.timestamp = LocalDateTime.now();
    }

    public CustomerDiscountErrorCode getErrorCode() { return errorCode; }
    public LocalDateTime getTimestamp() { return timestamp; }
}