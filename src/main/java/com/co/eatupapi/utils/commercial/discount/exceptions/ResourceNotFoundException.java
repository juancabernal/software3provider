package com.co.eatupapi.utils.commercial.discount.exceptions;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(DiscountErrorCode.RESOURCE_NOT_FOUND, message);
    }
}