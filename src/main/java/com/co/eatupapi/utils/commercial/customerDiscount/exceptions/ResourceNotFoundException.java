package com.co.eatupapi.utils.commercial.customerDiscount.exceptions;

public class ResourceNotFoundException extends ApiException {
    public ResourceNotFoundException(String message) {
        super(CustomerDiscountErrorCode.RESOURCE_NOT_FOUND, message);
    }
}