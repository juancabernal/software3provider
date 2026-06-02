package com.co.eatupapi.services.commercial.purchase;

import com.co.eatupapi.dto.commercial.purchase.PurchaseExportFilter;

import java.util.UUID;

public interface PurchaseExportService {
    byte[] exportToPdf(UUID locationId, PurchaseExportFilter filter);
    byte[] exportToExcel(UUID locationId, PurchaseExportFilter filter);
    String exportToHtmlPreview(UUID locationId, PurchaseExportFilter filter);
    void sendReportByEmail(UUID locationId, PurchaseExportFilter filter, String recipientEmail);
}