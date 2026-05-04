package com.co.eatupapi.services.commercial.sales;

import com.co.eatupapi.dto.commercial.sales.SaleAsyncResponseDTO;
import com.co.eatupapi.dto.commercial.sales.SalePatchDTO;
import com.co.eatupapi.dto.commercial.sales.SaleRequestDTO;
import com.co.eatupapi.dto.commercial.sales.SaleResponseDTO;
import java.util.List;
import java.util.UUID;

public interface SaleService {
    SaleAsyncResponseDTO createSale(SaleRequestDTO request);
    SaleResponseDTO getSaleById(UUID id);
    List<SaleResponseDTO> getAllSales();
    SaleAsyncResponseDTO updateSale(UUID id, SaleRequestDTO request);
    SaleAsyncResponseDTO patchSale(UUID id, SalePatchDTO request);
    SaleAsyncResponseDTO deleteSale(UUID id);
}
