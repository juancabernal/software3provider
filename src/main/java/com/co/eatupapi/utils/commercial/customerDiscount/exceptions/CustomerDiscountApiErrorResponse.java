package com.co.eatupapi.utils.commercial.customerDiscount.exceptions;

import java.time.LocalDateTime;

public class CustomerDiscountApiErrorResponse {

    private final String message;
    private final String errorCode;
    private final LocalDateTime timestamp;
    private final int status;

    public CustomerDiscountApiErrorResponse(String message, String errorCode, LocalDateTime timestamp, int status) {
        this.message = message;
        this.errorCode = errorCode;
        this.timestamp = timestamp;
        this.status = status;
    }

    public String getMessage() { return message; }
    public String getErrorCode() { return errorCode; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
}