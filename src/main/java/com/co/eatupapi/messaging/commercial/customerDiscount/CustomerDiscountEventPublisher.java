package com.co.eatupapi.messaging.commercial.customerDiscount;

import com.co.eatupapi.dto.commercial.customerDiscount.CustomerDiscountDTO;
import java.util.UUID;

public interface CustomerDiscountEventPublisher {
    void publishCustomerDiscountCreated(CustomerDiscountDTO customerDiscount);
    void publishCustomerDiscountUpdated(CustomerDiscountDTO customerDiscount);
    void publishCustomerDiscountDeleted(UUID customerDiscountId);
}