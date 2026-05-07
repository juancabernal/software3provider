package com.co.eatupapi.messaging.commercial.discount;

import com.co.eatupapi.dto.commercial.discount.DiscountDTO;
import java.util.UUID;

public interface DiscountEventPublisher {
    void publishDiscountCreated(DiscountDTO discount);
    void publishDiscountUpdated(DiscountDTO discount);
    void publishDiscountStatusUpdated(DiscountDTO discount);
    void publishDiscountDeleted(UUID discountId);
}