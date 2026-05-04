package com.co.eatupapi.messaging.commercial.sales;

import com.co.eatupapi.dto.commercial.sales.SaleRequestDTO;
import com.co.eatupapi.dto.commercial.sales.SaleResponseDTO;

public record SaleUpdateRequestedMessage(SaleResponseDTO oldSale, SaleRequestDTO newSale) {
}
