package com.co.eatupapi.messaging.commercial.sales;

import com.co.eatupapi.dto.commercial.sales.SaleResponseDTO;

public record SaleDeleteRequestedMessage(SaleResponseDTO sale) {
}
