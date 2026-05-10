package com.co.eatupapi.utils.commercial.discount.exceptions;

public class BusinessException extends ApiException {

    public BusinessException(String message) {
        super(DiscountErrorCode.BUSINESS_ERROR, message);
    }
}