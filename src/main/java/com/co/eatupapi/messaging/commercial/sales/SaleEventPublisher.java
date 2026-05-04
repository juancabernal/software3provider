package com.co.eatupapi.messaging.commercial.sales;

import com.co.eatupapi.dto.commercial.sales.SaleRequestDTO;

public interface SaleEventPublisher {
    void publishCreateRequested(SaleRequestDTO request);
    void publishUpdateRequested(SaleUpdateRequestedMessage message);
    void publishPatchRequested(SalePatchRequestedMessage message);
    void publishDeleteRequested(SaleDeleteRequestedMessage message);
}
