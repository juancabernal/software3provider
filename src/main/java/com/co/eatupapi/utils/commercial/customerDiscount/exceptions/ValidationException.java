package com.co.eatupapi.utils.commercial.customerDiscount.exceptions;

public class ValidationException extends ApiException {
    public ValidationException(String message) {
        super(CustomerDiscountErrorCode.VALIDATION_ERROR, message);
    }
}