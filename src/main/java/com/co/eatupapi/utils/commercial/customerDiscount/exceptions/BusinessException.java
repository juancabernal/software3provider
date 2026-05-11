package com.co.eatupapi.utils.commercial.customerDiscount.exceptions;

public class BusinessException extends ApiException {
    public BusinessException(String message) {
        super(CustomerDiscountErrorCode.BUSINESS_ERROR, message);
    }
}