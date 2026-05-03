package com.co.eatupapi.messaging.commercial.sales;

import com.co.eatupapi.dto.commercial.sales.SaleRequestDTO;

public interface SaleEventPublisher {
    void publishCreateRequested(SaleRequestDTO request);
}
