package com.co.eatupapi.messaging.commercial.sales;

import com.co.eatupapi.dto.commercial.sales.SaleDeleteRequestedMessage;
import com.co.eatupapi.dto.commercial.sales.SalePatchRequestedMessage;
import com.co.eatupapi.dto.commercial.sales.SaleRequestDTO;
import com.co.eatupapi.dto.commercial.sales.SaleUpdateRequestedMessage;

public interface SaleEventPublisher {
    void publishCreateRequested(SaleRequestDTO request);
    void publishUpdateRequested(SaleUpdateRequestedMessage message);
    void publishPatchRequested(SalePatchRequestedMessage message);
    void publishDeleteRequested(SaleDeleteRequestedMessage message);
}
