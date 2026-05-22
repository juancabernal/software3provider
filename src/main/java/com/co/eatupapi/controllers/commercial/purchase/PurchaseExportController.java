package com.co.eatupapi.controllers.commercial.purchase;

import com.co.eatupapi.dto.commercial.purchase.PurchaseEmailRequest;
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
@Tag(name = "Compras - Exportación", description = "Exportación de reportes de compras en PDF, Excel y HTML")
public class PurchaseExportController {

    private final PurchaseExportService purchaseExportService;

    public PurchaseExportController(PurchaseExportService purchaseExportService) {
        this.purchaseExportService = purchaseExportService;
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    @Operation(summary = "Exportar compras a PDF",
            description = "Genera y descarga un PDF con las compras filtradas.")
    @GetMapping(value = "/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportPdf(
            @PathVariable UUID locationId,
            @ModelAttribute PurchaseExportFilter filter) {

        byte[] pdf      = purchaseExportService.exportToPdf(locationId, filter);
        String filename = "compras-" + today() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }

    // ── Excel ─────────────────────────────────────────────────────────────────

    @Operation(summary = "Exportar compras a Excel",
            description = "Genera y descarga un .xlsx con las compras filtradas.")
    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            @PathVariable UUID locationId,
            @ModelAttribute PurchaseExportFilter filter) {

        byte[] xlsx     = purchaseExportService.exportToExcel(locationId, filter);
        String filename = "compras-" + today() + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(xlsx.length)
                .body(xlsx);
    }

    // ── HTML Preview ──────────────────────────────────────────────────────────

    @Operation(summary = "Previsualización del reporte en HTML",
            description = "Devuelve el reporte como HTML para mostrar en pantalla antes de exportar.")
    @GetMapping(value = "/export/preview", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> preview(
            @PathVariable UUID locationId,
            @ModelAttribute PurchaseExportFilter filter) {

        String html = purchaseExportService.exportToHtmlPreview(locationId, filter);

        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", java.nio.charset.StandardCharsets.UTF_8))
                .body(html);
    }

    // ── Enviar por correo ─────────────────────────────────────────────────────

    @Operation(summary = "Enviar reporte por correo",
            description = "Genera el PDF y lo envía como adjunto al correo indicado.")
    @PostMapping("/export/email")
    public ResponseEntity<Void> sendEmail(
            @PathVariable UUID locationId,
            @ModelAttribute PurchaseExportFilter filter,
            @RequestBody PurchaseEmailRequest emailRequest) {

        purchaseExportService.sendReportByEmail(locationId, filter, emailRequest.getRecipientEmail());
        return ResponseEntity.ok().build();
    }

    // ── Util ──────────────────────────────────────────────────────────────────

    private String today() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
}