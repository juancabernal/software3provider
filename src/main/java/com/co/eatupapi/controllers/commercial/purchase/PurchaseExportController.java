package com.co.eatupapi.controllers.commercial.purchase;

import com.co.eatupapi.dto.commercial.purchase.PurchaseExportFilter;
import com.co.eatupapi.services.commercial.purchase.PurchaseExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/locations/{locationId}/purchases")
@Tag(name = "Compras", description = "Gestión de compras de productos a proveedores")
public class PurchaseExportController {

    private final PurchaseExportService purchaseExportService;

    public PurchaseExportController(PurchaseExportService purchaseExportService) {
        this.purchaseExportService = purchaseExportService;
    }

    @Operation(
            summary = "Exportar compras a PDF",
            description = "Genera y descarga un PDF con las compras filtradas."
    )
    @GetMapping(value = "/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportPdf(
            @PathVariable UUID locationId,
            @ModelAttribute PurchaseExportFilter filter) {

        byte[] pdf = purchaseExportService.exportToPdf(locationId, filter);

        String filename = "compras-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }
}