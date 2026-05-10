package com.co.eatupapi.utils.commercial.discount.exceptions;

public class ValidationException extends ApiException {

    public ValidationException(String message) {
        super(DiscountErrorCode.VALIDATION_ERROR, message);
    }
}