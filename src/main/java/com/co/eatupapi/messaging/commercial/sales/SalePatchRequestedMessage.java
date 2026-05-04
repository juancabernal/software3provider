package com.co.eatupapi.messaging.commercial.sales;

import com.co.eatupapi.dto.commercial.sales.SalePatchDTO;
import java.util.UUID;

public record SalePatchRequestedMessage(UUID saleId, SalePatchDTO request) {
}
