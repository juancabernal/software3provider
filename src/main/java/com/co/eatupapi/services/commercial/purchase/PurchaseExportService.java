package com.co.eatupapi.services.commercial.purchase;

import com.co.eatupapi.dto.commercial.purchase.PurchaseExportFilter;
import java.util.UUID;

public interface PurchaseExportService {
    byte[] exportToPdf(UUID locationId, PurchaseExportFilter filter);
}