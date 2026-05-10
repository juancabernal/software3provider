package com.co.eatupapi.utils.commercial.discount.exceptions;

import java.time.LocalDateTime;

public abstract class ApiException extends RuntimeException {

    private final DiscountErrorCode errorCode;
    private final LocalDateTime timestamp;

    protected ApiException(DiscountErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.timestamp = LocalDateTime.now();
    }

    public DiscountErrorCode getErrorCode() { return errorCode; }
    public LocalDateTime getTimestamp() { return timestamp; }
}