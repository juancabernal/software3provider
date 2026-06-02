package com.co.eatupapi.services.commercial.purchase.impl;

import com.co.eatupapi.domain.commercial.purchase.PurchaseDomain;
import com.co.eatupapi.domain.commercial.purchase.PurchaseItemDomain;
import com.co.eatupapi.domain.commercial.purchase.PurchaseStatus;
import com.co.eatupapi.domain.commercial.provider.ProviderDomain;
import com.co.eatupapi.dto.commercial.purchase.PurchaseExportFilter;
import com.co.eatupapi.repositories.commercial.provider.ProviderRepository;
import com.co.eatupapi.repositories.commercial.purchase.PurchaseRepository;
import com.co.eatupapi.services.commercial.purchase.ProductNameResolver;
import com.co.eatupapi.services.commercial.purchase.PurchaseExportService;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import jakarta.persistence.criteria.Predicate;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PurchaseExportServiceImpl implements PurchaseExportService {

    private static final String CREATED_DATE_FIELD = "createdDate";
    private static final DateTimeFormatter DATE_FMT      = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_ONLY_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Paleta PDF ───────────────────────────────────────────────────────────
    private static final DeviceRgb BRAND_ORANGE  = new DeviceRgb(255, 107,  53);
    private static final DeviceRgb ORANGE_LIGHT  = new DeviceRgb(255, 237, 213);
    private static final DeviceRgb ORANGE_PALE   = new DeviceRgb(255, 248, 242);
    private static final DeviceRgb ITEM_BG       = new DeviceRgb(250, 250, 248);
    private static final DeviceRgb BORDER        = new DeviceRgb(234, 223, 212);
    private static final DeviceRgb BORDER_LIGHT  = new DeviceRgb(243, 236, 228);
    private static final DeviceRgb WHITE         = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb TEXT_MAIN     = new DeviceRgb( 30,  30,  30);
    private static final DeviceRgb TEXT_MUTED    = new DeviceRgb(107, 114, 128);
    private static final DeviceRgb TEXT_ITEM     = new DeviceRgb( 80,  80,  80);
    private static final DeviceRgb STATUS_CREATED_BG   = new DeviceRgb(219, 234, 254);
    private static final DeviceRgb STATUS_CREATED_FG   = new DeviceRgb( 29,  78, 216);
    private static final DeviceRgb STATUS_APPROVED_BG  = new DeviceRgb(220, 252, 231);
    private static final DeviceRgb STATUS_APPROVED_FG  = new DeviceRgb( 21, 128,  61);
    private static final DeviceRgb STATUS_RECEIVED_BG  = new DeviceRgb(209, 250, 229);
    private static final DeviceRgb STATUS_RECEIVED_FG  = new DeviceRgb(  6,  95,  70);
    private static final DeviceRgb STATUS_CANCELLED_BG = new DeviceRgb(254, 226, 226);
    private static final DeviceRgb STATUS_CANCELLED_FG = new DeviceRgb(185,  28,  28);
    private static final DeviceRgb SUMMARY_BG     = new DeviceRgb(255, 247, 237);
    private static final DeviceRgb SUMMARY_BORDER = new DeviceRgb(253, 186, 116);
    private static final DeviceRgb ORDER_SEPARATOR = new DeviceRgb(255, 107, 53);

    private final PurchaseRepository  purchaseRepository;
    private final ProviderRepository  providerRepository;
    private final ProductNameResolver productNameResolver;
    private final JavaMailSender      mailSender;

    @Value("${spring.mail.username}")
    private String mailFrom;

    public PurchaseExportServiceImpl(PurchaseRepository purchaseRepository,
                                     ProviderRepository providerRepository,
                                     ProductNameResolver productNameResolver,
                                     JavaMailSender mailSender) {
        this.purchaseRepository  = purchaseRepository;
        this.providerRepository  = providerRepository;
        this.productNameResolver = productNameResolver;
        this.mailSender          = mailSender;
    }

    // ── Specification ────────────────────────────────────────────────────────

    private Specification<PurchaseDomain> buildSpec(UUID locationId, PurchaseExportFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("locationId"), locationId));
            predicates.add(cb.isFalse(root.get("deleted")));
            if (filter.getStatus()     != null)
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            if (filter.getProviderId() != null)
                predicates.add(cb.equal(root.get("providerId"), filter.getProviderId()));
            if (filter.getStartDate()  != null)
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get(CREATED_DATE_FIELD), filter.getStartDate().atStartOfDay()));
            if (filter.getEndDate()    != null)
                predicates.add(cb.lessThanOrEqualTo(
                        root.get(CREATED_DATE_FIELD), filter.getEndDate().atTime(23, 59, 59)));
            query.orderBy(cb.desc(root.get(CREATED_DATE_FIELD)));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // ── Resolver datos comunes ───────────────────────────────────────────────

    private record ReportData(
            List<PurchaseDomain> purchases,
            Map<UUID, String> providerNames,
            Map<UUID, String> productNames
    ) {}

    @Transactional(readOnly = true)
    protected ReportData loadData(UUID locationId, PurchaseExportFilter filter) {
        List<PurchaseDomain> purchases = purchaseRepository.findAll(buildSpec(locationId, filter));

        List<UUID> providerIds = purchases.stream()
                .map(PurchaseDomain::getProviderId).distinct().toList();
        Map<UUID, String> providerNames = providerRepository.findAllById(providerIds)
                .stream()
                .collect(Collectors.toMap(ProviderDomain::getId, ProviderDomain::getBusinessName));

        List<UUID> productIds = purchases.stream()
                .filter(p -> p.getItems() != null)
                .flatMap(p -> p.getItems().stream())
                .map(PurchaseItemDomain::getProductId)
                .distinct().toList();
        Map<UUID, String> productNames = productNameResolver.resolveNames(productIds);

        return new ReportData(purchases, providerNames, productNames);
    }

    // ════════════════════════════════════════════════════════════════════════
    // PDF
    // ════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public byte[] exportToPdf(UUID locationId, PurchaseExportFilter filter) {
        ReportData data = loadData(locationId, filter);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont bold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfDocument pdf      = new PdfDocument(new PdfWriter(out));
            Document    document = new Document(pdf, PageSize.A4.rotate());
            document.setMargins(40, 30, 50, 30);
            document.setFont(regular);

            document.add(new Paragraph("Reporte de Compras")
                    .setFont(bold).setFontSize(22).setFontColor(BRAND_ORANGE)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(6));

            document.add(buildFilterBlock(filter, regular));

            if (data.purchases().isEmpty()) {
                document.add(emptyMsg(regular));
                document.close();
                return out.toByteArray();
            }

            document.add(buildSummaryBlock(data, regular, bold));
            document.add(buildTable(data, regular, bold));
            addPageNumbers(pdf, regular);
            document.close();

        } catch (IOException e) {
            throw new PdfGenerationException("Error generando PDF", e);
        }
        return out.toByteArray();
    }

    // ════════════════════════════════════════════════════════════════════════
    // EXCEL (Apache POI)
    // ════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public byte[] exportToExcel(UUID locationId, PurchaseExportFilter filter) {
        ReportData data = loadData(locationId, filter);

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Compras");

            // ── Estilos ───────────────────────────────────────────────────
            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);

            CellStyle totalStyle = wb.createCellStyle();
            totalStyle.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
            totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font totalFont = wb.createFont();
            totalFont.setBold(true);
            totalStyle.setFont(totalFont);

            CellStyle moneyStyle = wb.createCellStyle();
            DataFormat df = wb.createDataFormat();
            moneyStyle.setDataFormat(df.getFormat("$#,##0.00"));

            CellStyle itemStyle = wb.createCellStyle();
            itemStyle.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
            itemStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // ── Título ────────────────────────────────────────────────────
            Row titleRow = sheet.createRow(0);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Reporte de Compras");
            CellStyle titleStyle = wb.createCellStyle();
            Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setColor(IndexedColors.ORANGE.getIndex());
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

            // ── Subtítulo filtros ─────────────────────────────────────────
            Row subRow = sheet.createRow(1);
            subRow.createCell(0).setCellValue(buildSubtitleText(filter));
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));

            // ── Métricas resumen ──────────────────────────────────────────
            BigDecimal totalGastado = data.purchases().stream()
                    .map(p -> p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal promedio = data.purchases().isEmpty() ? BigDecimal.ZERO :
                    totalGastado.divide(BigDecimal.valueOf(data.purchases().size()), 2, RoundingMode.HALF_UP);
            String topProvider = data.purchases().stream()
                    .collect(Collectors.groupingBy(PurchaseDomain::getProviderId, Collectors.counting()))
                    .entrySet().stream().max(Map.Entry.comparingByValue())
                    .map(e -> data.providerNames().getOrDefault(e.getKey(), "—")).orElse("—");

            Row metaRow = sheet.createRow(2);
            metaRow.createCell(0).setCellValue("Total compras: " + data.purchases().size());
            metaRow.createCell(2).setCellValue("Total gastado: " + formatMoney(totalGastado));
            metaRow.createCell(4).setCellValue("Proveedor top: " + topProvider);
            metaRow.createCell(6).setCellValue("Promedio: " + formatMoney(promedio));

            sheet.createRow(3); // espacio

            // ── Cabecera tabla ────────────────────────────────────────────
            int rowIdx = 4;
            Row header = sheet.createRow(rowIdx++);
            String[] cols = {"N° Orden", "Proveedor", "Estado", "Fecha", "Producto", "Cantidad", "Precio Unit.", "Subtotal", "Total Orden"};
            for (int i = 0; i < cols.length; i++) {
                org.apache.poi.ss.usermodel.Cell c = header.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(headerStyle);
            }

            // ── Filas de datos ────────────────────────────────────────────
            BigDecimal grandTotal = BigDecimal.ZERO;
            for (PurchaseDomain p : data.purchases()) {
                String providerName = data.providerNames().getOrDefault(p.getProviderId(), "—");
                BigDecimal pTotal   = p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO;
                grandTotal = grandTotal.add(pTotal);

                if (p.getItems() == null || p.getItems().isEmpty()) {
                    Row r = sheet.createRow(rowIdx++);
                    r.createCell(0).setCellValue(p.getOrderNumber());
                    r.createCell(1).setCellValue(providerName);
                    r.createCell(2).setCellValue(translateStatus(p.getStatus()));
                    r.createCell(3).setCellValue(p.getCreatedDate() != null ? p.getCreatedDate().format(DATE_FMT) : "-");
                    r.createCell(4).setCellValue("—");
                    r.createCell(8).setCellValue(pTotal.doubleValue());
                    r.getCell(8).setCellStyle(moneyStyle);
                } else {
                    boolean firstItem = true;
                    for (PurchaseItemDomain item : p.getItems()) {
                        Row r = sheet.createRow(rowIdx++);
                        if (firstItem) {
                            r.createCell(0).setCellValue(p.getOrderNumber());
                            r.createCell(1).setCellValue(providerName);
                            r.createCell(2).setCellValue(translateStatus(p.getStatus()));
                            r.createCell(3).setCellValue(p.getCreatedDate() != null ? p.getCreatedDate().format(DATE_FMT) : "-");
                            org.apache.poi.ss.usermodel.Cell totalCell = r.createCell(8);
                            totalCell.setCellValue(pTotal.doubleValue());
                            totalCell.setCellStyle(moneyStyle);
                            firstItem = false;
                        } else {
                            // filas de ítem: columnas 0-3 vacías, estilo claro
                            for (int i = 0; i <= 3; i++) r.createCell(i).setCellStyle(itemStyle);
                        }
                        String productName = data.productNames().getOrDefault(item.getProductId(), "Desconocido");
                        r.createCell(4).setCellValue(productName);
                        r.createCell(5).setCellValue(item.getQuantity().doubleValue());
                        org.apache.poi.ss.usermodel.Cell upCell = r.createCell(6);
                        upCell.setCellValue(item.getUnitPrice().doubleValue());
                        upCell.setCellStyle(moneyStyle);
                        org.apache.poi.ss.usermodel.Cell subCell = r.createCell(7);
                        subCell.setCellValue(item.getSubtotal().doubleValue());
                        subCell.setCellStyle(moneyStyle);
                    }
                }
            }

            // ── Fila total general ────────────────────────────────────────
            Row totalRow = sheet.createRow(rowIdx);
            org.apache.poi.ss.usermodel.Cell labelCell = totalRow.createCell(0);
            labelCell.setCellValue("TOTAL GENERAL (" + data.purchases().size() + " compras)");
            labelCell.setCellStyle(totalStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 7));
            org.apache.poi.ss.usermodel.Cell grandCell = totalRow.createCell(8);
            grandCell.setCellValue(grandTotal.doubleValue());
            CellStyle grandStyle = wb.createCellStyle();
            grandStyle.cloneStyleFrom(totalStyle);
            grandStyle.setDataFormat(df.getFormat("$#,##0.00"));
            grandCell.setCellStyle(grandStyle);

            // Auto-size columnas
            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);

            wb.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new PdfGenerationException("Error generando Excel", e);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // HTML PREVIEW
    // ════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public String exportToHtmlPreview(UUID locationId, PurchaseExportFilter filter) {
        ReportData data = loadData(locationId, filter);
        List<PurchaseDomain> purchases = data.purchases();

        BigDecimal totalGastado = purchases.stream()
                .map(p -> p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal promedio = purchases.isEmpty() ? BigDecimal.ZERO :
                totalGastado.divide(BigDecimal.valueOf(purchases.size()), 2, RoundingMode.HALF_UP);
        String topProvider = purchases.stream()
                .collect(Collectors.groupingBy(PurchaseDomain::getProviderId, Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue())
                .map(e -> data.providerNames().getOrDefault(e.getKey(), "—")).orElse("—");

        StringBuilder sb = new StringBuilder();

        sb.append("""
            <!DOCTYPE html>
            <!DOCTYPE html>
            <html lang="es">
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
              <style>
                *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
                body { font-family: 'Segoe UI', Arial, sans-serif; font-size: 13px;
                       color: #1e1e1e; background: #fff; padding: 20px; }
                h1 { color: #ff6b35; font-size: 20px; font-weight: 700;
                     text-align: center; margin-bottom: 4px; }
                .subtitle { text-align: center; color: #6b7280; font-size: 11px; margin-bottom: 16px; }
                /* flex en lugar de grid — máxima compatibilidad en srcdoc de iframe */
                .summary { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 20px; }
                .metric { flex: 1 1 120px; min-width: 100px; background: #fff7ed;
                          border: 1px solid #fdba74; border-radius: 8px;
                          padding: 12px 8px; text-align: center; }
                .metric-value { font-size: 15px; font-weight: 700; color: #ff6b35;
                                margin-bottom: 4px; word-break: break-word; }
                .metric-label { font-size: 10px; color: #6b7280; }
                table { width: 100%; border-collapse: collapse; margin-top: 4px; }
                thead tr { background: #ff6b35; }
                thead th { padding: 9px 8px; text-align: left; font-size: 11px;
                           color: #fff; font-weight: 700; white-space: nowrap; }
                thead th.right { text-align: right; }
                tbody td { padding: 8px; border-bottom: 1px solid #eadfcf;
                           font-size: 12px; vertical-align: middle; }
                tr.order-sep td { border-top: 2px solid #ff6b35; }
                tr.alt td { background: #fff8f2; }
                tr.item-row td { background: #fafaf8; color: #505050;
                                 font-size: 11px; padding: 4px 8px; }
                tr.item-row td:first-child { padding-left: 24px; }
                .status { display: inline-block; padding: 2px 8px; border-radius: 9999px;
                          font-size: 10px; font-weight: 700; white-space: nowrap; }
                .s-CREATED   { background:#dbeafe; color:#1d4ed8; }
                .s-APPROVED  { background:#dcfce7; color:#15803d; }
                .s-RECEIVED  { background:#d1fae5; color:#065f46; }
                .s-CANCELLED { background:#fee2e2; color:#b91c1c; }
                tr.subtotal-row td { background: #fff8f2; font-weight: 700;
                                     font-size: 11px; padding: 6px 8px;
                                     border-top: 1px solid #eadfcf; text-align: right; }
                tr.subtotal-row td.sub-amount { color: #ff6b35; }
                tr.total-row td { background: #ffedd5; font-weight: 700; font-size: 13px;
                                  padding: 10px 8px; border-top: 2px solid #fdba74;
                                  text-align: right; }
                tr.total-row td.grand { color: #ff6b35; }
                .text-right { text-align: right; }
                .empty { text-align: center; color: #9ca3af; padding: 40px 0; font-size: 14px; }
              </style>
            </head>
            <body>
            """);

        sb.append("<h1>Reporte de Compras</h1>");
        sb.append("<div class=\"subtitle\">").append(buildSubtitleText(filter)).append("</div>");

        // Resumen
        sb.append("<div class=\"summary\">");
        sb.append(metricCard(String.valueOf(purchases.size()), "Total compras"));
        sb.append(metricCard(formatMoney(totalGastado), "Total gastado"));
        sb.append(metricCard(topProvider, "Proveedor top"));
        sb.append(metricCard(formatMoney(promedio), "Promedio / compra"));
        sb.append("</div>");

        if (purchases.isEmpty()) {
            sb.append("<div class=\"empty\">No se encontraron compras con los filtros seleccionados.</div>");
        } else {
            sb.append("<table><thead><tr>")
                    .append("<th>N° Orden</th><th>Proveedor</th><th>Estado</th>")
                    .append("<th>Fecha</th><th class=\"right\">Ítems</th><th class=\"right\">Total</th>")
                    .append("</tr></thead><tbody>");

            // Iteramos en orden cronologico DESC sin reagrupar
            BigDecimal grandTotal = BigDecimal.ZERO;
            boolean alt = false;
            boolean firstRow = true;
            Map<UUID, BigDecimal> htmlProviderTotals = new LinkedHashMap<>();
            Map<UUID, Integer>    htmlProviderCounts = new LinkedHashMap<>();

            for (PurchaseDomain p : purchases) {
                String providerName = data.providerNames().getOrDefault(p.getProviderId(), "—");
                int itemCount       = p.getItems() != null ? p.getItems().size() : 0;
                BigDecimal pTotal   = p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO;
                grandTotal          = grandTotal.add(pTotal);
                htmlProviderTotals.merge(p.getProviderId(), pTotal, BigDecimal::add);
                htmlProviderCounts.merge(p.getProviderId(), 1, Integer::sum);

                String rowCls = (firstRow ? "order-row" : "order-row order-sep") + (alt ? " alt" : "");
                firstRow = false;
                alt = !alt;

                sb.append("<tr class=\"").append(rowCls).append("\">")
                        .append("<td><strong>").append(p.getOrderNumber()).append("</strong></td>")
                        .append("<td>").append(providerName).append("</td>")
                        .append("<td><span class=\"status s-").append(p.getStatus()).append("\">")
                        .append(translateStatus(p.getStatus())).append("</span></td>")
                        .append("<td>").append(p.getCreatedDate() != null ? p.getCreatedDate().format(DATE_FMT) : "-").append("</td>")
                        .append("<td class=\"text-right\">").append(itemCount).append("</td>")
                        .append("<td class=\"text-right\"><strong>").append(formatMoney(pTotal)).append("</strong></td>")
                        .append("</tr>");

                if (p.getItems() != null) {
                    for (PurchaseItemDomain item : p.getItems()) {
                        String productName = data.productNames().getOrDefault(item.getProductId(), "Desconocido");
                        sb.append("<tr class=\"item-row\">")
                                .append("<td colspan=\"2\">↳ ").append(productName).append("</td>")
                                .append("<td>").append(formatMoney(item.getUnitPrice())).append(" / u</td>")
                                .append("<td>").append(item.getQuantity().setScale(2, RoundingMode.HALF_UP).toPlainString()).append(" u</td>")
                                .append("<td></td>")
                                .append("<td class=\"text-right\">").append(formatMoney(item.getSubtotal())).append("</td>")
                                .append("</tr>");
                    }
                }
            }

            // Subtotales por proveedor
            for (Map.Entry<UUID, BigDecimal> entry : htmlProviderTotals.entrySet()) {
                String pName = data.providerNames().getOrDefault(entry.getKey(), "—");
                int cnt = htmlProviderCounts.get(entry.getKey());
                sb.append("<tr class=\"subtotal-row\">")
                        .append("<td colspan=\"5\">Subtotal ").append(pName)
                        .append(" (").append(cnt).append(cnt == 1 ? " compra)" : " compras)").append("</td>")
                        .append("<td class=\"sub-amount\">").append(formatMoney(entry.getValue())).append("</td></tr>");
            }

            // Total general
            sb.append("<tr class=\"total-row\"><td colspan=\"5\">Total general (")
                    .append(purchases.size()).append(purchases.size() == 1 ? " compra)" : " compras)").append("</td>")
                    .append("<td class=\"grand\">").append(formatMoney(grandTotal)).append("</td></tr>");

            sb.append("</tbody></table>");
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    // ════════════════════════════════════════════════════════════════════════
    // EMAIL
    // ════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public void sendReportByEmail(UUID locationId, PurchaseExportFilter filter, String recipientEmail) {
        byte[] pdf = exportToPdf(locationId, filter);
        String filename = "compras-" + java.time.LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(recipientEmail);
            helper.setSubject("Reporte de Compras — " + java.time.LocalDate.now().format(DATE_ONLY_FMT));
            helper.setText(buildEmailBody(filter), true);
            helper.addAttachment(filename,
                    new org.springframework.core.io.ByteArrayResource(pdf),
                    "application/pdf");

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new PdfGenerationException("Error enviando el reporte por correo", e);
        }
    }

    private String buildEmailBody(PurchaseExportFilter filter) {
        return """
            <html><body style="font-family:sans-serif;color:#1e1e1e;">
              <h2 style="color:#ff6b35;">Reporte de Compras</h2>
              <p>Adjunto encontrará el reporte de compras generado con los siguientes filtros:</p>
              <ul>
                <li><strong>Generado el:</strong> %s</li>
                %s
              </ul>
              <p style="color:#6b7280;font-size:12px;margin-top:24px;">
                Este correo fue generado automáticamente por EatUp.
              </p>
            </body></html>
            """.formatted(
                java.time.LocalDateTime.now().format(DATE_FMT),
                buildFilterListItems(filter)
        );
    }

    private String buildFilterListItems(PurchaseExportFilter filter) {
        StringBuilder sb = new StringBuilder();
        if (filter.getStatus()    != null) sb.append("<li><strong>Estado:</strong> ").append(translateStatus(filter.getStatus())).append("</li>");
        if (filter.getStartDate() != null) sb.append("<li><strong>Desde:</strong> ").append(filter.getStartDate().format(DATE_ONLY_FMT)).append("</li>");
        if (filter.getEndDate()   != null) sb.append("<li><strong>Hasta:</strong> ").append(filter.getEndDate().format(DATE_ONLY_FMT)).append("</li>");
        return sb.toString();
    }

    // ════════════════════════════════════════════════════════════════════════
    // PDF — helpers internos
    // ════════════════════════════════════════════════════════════════════════

    private Table buildTable(ReportData data, PdfFont regular, PdfFont bold) {
        float[] colWidths = {130f, 160f, 95f, 110f, 45f, 95f};
        Table table = new Table(UnitValue.createPointArray(colWidths));
        table.setWidth(UnitValue.createPercentValue(100));

        addHeaderCell(table, bold, "N° Orden",  TextAlignment.LEFT);
        addHeaderCell(table, bold, "Proveedor", TextAlignment.LEFT);
        addHeaderCell(table, bold, "Estado",    TextAlignment.LEFT);
        addHeaderCell(table, bold, "Fecha",     TextAlignment.LEFT);
        addHeaderCell(table, bold, "Ítems",     TextAlignment.RIGHT);
        addHeaderCell(table, bold, "Total",     TextAlignment.RIGHT);

        // Iteramos en orden cronológico DESC
        BigDecimal grandTotal = BigDecimal.ZERO;
        boolean alt           = false;
        boolean firstOrder    = true;

        Map<UUID, BigDecimal> providerTotals = new LinkedHashMap<>();
        Map<UUID, Integer>    providerCounts = new LinkedHashMap<>();

        for (PurchaseDomain p : data.purchases()) {
            DeviceRgb rowBg   = alt ? ORANGE_PALE : WHITE;
            alt               = !alt;
            int itemCount     = p.getItems() != null ? p.getItems().size() : 0;
            BigDecimal pTotal = p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO;
            grandTotal        = grandTotal.add(pTotal);

            providerTotals.merge(p.getProviderId(), pTotal,  BigDecimal::add);
            providerCounts.merge(p.getProviderId(), 1,       Integer::sum);

            String providerName = data.providerNames().getOrDefault(p.getProviderId(), "—");
            addOrderRow(table, regular, bold, rowBg, p, providerName, itemCount, firstOrder);
            firstOrder = false;

            if (p.getItems() != null) {
                for (PurchaseItemDomain item : p.getItems()) {
                    String productName = data.productNames().getOrDefault(item.getProductId(), "Desconocido");
                    addItemRow(table, regular, item, productName);
                }
            }
        }

        // Subtotales por proveedor al pie de la tabla
        for (Map.Entry<UUID, BigDecimal> entry : providerTotals.entrySet()) {
            String providerName = data.providerNames().getOrDefault(entry.getKey(), "—");
            addProviderSubtotalRow(table, bold, providerName, providerCounts.get(entry.getKey()), entry.getValue());
        }

        addTotalRow(table, bold, data.purchases().size(), grandTotal);
        return table;
    }

    private Table buildFilterBlock(PurchaseExportFilter filter, PdfFont regular) {
        Table wrapper = new Table(UnitValue.createPercentArray(new float[]{1}));
        wrapper.setWidth(UnitValue.createPercentValue(100)).setMarginBottom(10);
        wrapper.addCell(new Cell()
                .add(new Paragraph(buildSubtitleText(filter))
                        .setFont(regular).setFontSize(8).setFontColor(TEXT_MUTED))
                .setBackgroundColor(new DeviceRgb(249, 250, 251))
                .setBorder(new SolidBorder(new DeviceRgb(209, 213, 219), 0.5f))
                .setPaddingTop(6).setPaddingBottom(6).setPaddingLeft(10).setPaddingRight(10));
        return wrapper;
    }

    private Table buildSummaryBlock(ReportData data, PdfFont regular, PdfFont bold) {
        List<PurchaseDomain> purchases = data.purchases();
        BigDecimal totalGastado = purchases.stream()
                .map(p -> p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal promedio = purchases.isEmpty() ? BigDecimal.ZERO :
                totalGastado.divide(BigDecimal.valueOf(purchases.size()), 2, RoundingMode.HALF_UP);
        String topProvider = purchases.stream()
                .collect(Collectors.groupingBy(PurchaseDomain::getProviderId, Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue())
                .map(e -> data.providerNames().getOrDefault(e.getKey(), "—")).orElse("—");

        Table summary = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1}));
        summary.setWidth(UnitValue.createPercentValue(100)).setMarginBottom(14);
        addMetricCell(summary, bold, regular, "Total compras",     String.valueOf(purchases.size()));
        addMetricCell(summary, bold, regular, "Total gastado",     formatMoney(totalGastado));
        addMetricCell(summary, bold, regular, "Proveedor top",     topProvider);
        addMetricCell(summary, bold, regular, "Promedio / compra", formatMoney(promedio));
        return summary;
    }

    private Paragraph emptyMsg(PdfFont regular) {
        return new Paragraph("No se encontraron compras con los filtros seleccionados.")
                .setFont(regular).setFontSize(11).setFontColor(TEXT_MUTED)
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(20);
    }

    private void addMetricCell(Table table, PdfFont bold, PdfFont regular, String label, String value) {
        table.addCell(new Cell()
                .add(new Paragraph(value).setFont(bold).setFontSize(13)
                        .setFontColor(BRAND_ORANGE).setTextAlignment(TextAlignment.CENTER).setMarginBottom(2))
                .add(new Paragraph(label).setFont(regular).setFontSize(7.5f)
                        .setFontColor(TEXT_MUTED).setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(SUMMARY_BG).setBorder(new SolidBorder(SUMMARY_BORDER, 0.8f))
                .setPaddingTop(10).setPaddingBottom(10).setPaddingLeft(8).setPaddingRight(8));
    }

    private void addOrderRow(Table table, PdfFont regular, PdfFont bold, DeviceRgb rowBg,
                             PurchaseDomain p, String providerName, int itemCount, boolean firstRow) {
        SolidBorder top = firstRow ? new SolidBorder(BORDER, 0.5f) : new SolidBorder(ORDER_SEPARATOR, 1.2f);
        SolidBorder def = new SolidBorder(BORDER, 0.5f);

        table.addCell(cell(p.getOrderNumber(), bold, 8.5f, TEXT_MAIN, rowBg, TextAlignment.LEFT, top, def));
        table.addCell(cell(providerName, regular, 8.5f, TEXT_MAIN, rowBg, TextAlignment.LEFT, top, def));
        addStatusCell(table, bold, p.getStatus(), top, def);
        table.addCell(cell(p.getCreatedDate() != null ? p.getCreatedDate().format(DATE_FMT) : "-",
                regular, 8.5f, TEXT_MAIN, rowBg, TextAlignment.LEFT, top, def));
        table.addCell(cell(String.valueOf(itemCount), regular, 8.5f, TEXT_MAIN, rowBg, TextAlignment.RIGHT, top, def));
        table.addCell(cell(formatMoney(p.getTotal()), bold, 8.5f, TEXT_MAIN, rowBg, TextAlignment.RIGHT, top, def));
    }

    private Cell cell(String text, PdfFont font, float size, DeviceRgb fg, DeviceRgb bg,
                      TextAlignment align, SolidBorder top, SolidBorder def) {
        return new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(size).setFontColor(fg).setTextAlignment(align))
                .setBackgroundColor(bg)
                .setBorderTop(top).setBorderBottom(def).setBorderLeft(def).setBorderRight(def)
                .setPaddingTop(8).setPaddingBottom(8).setPaddingLeft(6).setPaddingRight(6);
    }

    private void addStatusCell(Table table, PdfFont bold, PurchaseStatus status,
                               SolidBorder top, SolidBorder def) {
        DeviceRgb bg; DeviceRgb fg;
        if (status == null) { bg = new DeviceRgb(243,244,246); fg = TEXT_MUTED; }
        else switch (status) {
            case CREATED   -> { bg = STATUS_CREATED_BG;   fg = STATUS_CREATED_FG;   }
            case APPROVED  -> { bg = STATUS_APPROVED_BG;  fg = STATUS_APPROVED_FG;  }
            case RECEIVED  -> { bg = STATUS_RECEIVED_BG;  fg = STATUS_RECEIVED_FG;  }
            case CANCELLED -> { bg = STATUS_CANCELLED_BG; fg = STATUS_CANCELLED_FG; }
            default        -> { bg = new DeviceRgb(243,244,246); fg = TEXT_MUTED;   }
        }
        table.addCell(new Cell()
                .add(new Paragraph(translateStatus(status)).setFont(bold).setFontSize(7.5f)
                        .setFontColor(fg).setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(bg)
                .setBorderTop(top).setBorderBottom(def).setBorderLeft(def).setBorderRight(def)
                .setPaddingTop(8).setPaddingBottom(8).setPaddingLeft(4).setPaddingRight(4));
    }

    private void addItemRow(Table table, PdfFont regular, PurchaseItemDomain item, String productName) {
        table.addCell(new Cell(1, 2)
                .add(new Paragraph("↳  " + productName).setFont(regular).setFontSize(7.5f).setFontColor(TEXT_ITEM))
                .setBackgroundColor(ITEM_BG)
                .setBorderTop(new SolidBorder(BORDER_LIGHT, 0.3f)).setBorderBottom(new SolidBorder(BORDER_LIGHT, 0.3f))
                .setBorderLeft(new SolidBorder(BORDER, 0.5f)).setBorderRight(new SolidBorder(BORDER_LIGHT, 0.3f))
                .setPaddingLeft(24).setPaddingTop(4).setPaddingBottom(4).setPaddingRight(6));
        table.addCell(spacerCell());
        table.addCell(new Cell()
                .add(new Paragraph(formatMoney(item.getUnitPrice())).setFont(regular).setFontSize(7.5f)
                        .setFontColor(TEXT_ITEM).setTextAlignment(TextAlignment.LEFT))
                .setBackgroundColor(ITEM_BG).setBorder(new SolidBorder(BORDER_LIGHT, 0.3f)).setPadding(4));
        table.addCell(new Cell()
                .add(new Paragraph(item.getQuantity().setScale(2, RoundingMode.HALF_UP).toPlainString())
                        .setFont(regular).setFontSize(7.5f).setFontColor(TEXT_ITEM).setTextAlignment(TextAlignment.RIGHT))
                .setBackgroundColor(ITEM_BG).setBorder(new SolidBorder(BORDER_LIGHT, 0.3f)).setPadding(4));
        table.addCell(new Cell()
                .add(new Paragraph(formatMoney(item.getSubtotal())).setFont(regular).setFontSize(7.5f)
                        .setFontColor(TEXT_ITEM).setTextAlignment(TextAlignment.RIGHT))
                .setBackgroundColor(ITEM_BG)
                .setBorderTop(new SolidBorder(BORDER_LIGHT, 0.3f)).setBorderBottom(new SolidBorder(BORDER_LIGHT, 0.3f))
                .setBorderLeft(new SolidBorder(BORDER_LIGHT, 0.3f)).setBorderRight(new SolidBorder(BORDER, 0.5f))
                .setPadding(4));
    }

    private Cell spacerCell() {
        return new Cell().add(new Paragraph("")).setBackgroundColor(ITEM_BG)
                .setBorder(new SolidBorder(BORDER_LIGHT, 0.3f)).setPadding(4);
    }

    private void addProviderSubtotalRow(Table table, PdfFont bold, String providerName, int count, BigDecimal subtotal) {
        String label = "Subtotal " + providerName + " (" + count + " " + (count == 1 ? "compra" : "compras") + ")";
        table.addCell(new Cell(1, 5)
                .add(new Paragraph(label).setFont(bold).setFontSize(8).setFontColor(TEXT_MAIN).setTextAlignment(TextAlignment.RIGHT))
                .setBackgroundColor(ORANGE_PALE).setBorder(new SolidBorder(BORDER, 0.5f))
                .setPaddingTop(6).setPaddingBottom(6).setPaddingLeft(6).setPaddingRight(6));
        table.addCell(new Cell()
                .add(new Paragraph(formatMoney(subtotal)).setFont(bold).setFontSize(8)
                        .setFontColor(BRAND_ORANGE).setTextAlignment(TextAlignment.RIGHT))
                .setBackgroundColor(ORANGE_PALE).setBorder(new SolidBorder(BORDER, 0.5f))
                .setPaddingTop(6).setPaddingBottom(6).setPaddingLeft(6).setPaddingRight(6));
    }

    private void addTotalRow(Table table, PdfFont bold, int count, BigDecimal total) {
        String label = "Total general (" + count + " " + (count == 1 ? "compra" : "compras") + ")";
        table.addCell(new Cell(1, 5)
                .add(new Paragraph(label).setFont(bold).setFontSize(9).setFontColor(TEXT_MAIN).setTextAlignment(TextAlignment.RIGHT))
                .setBackgroundColor(ORANGE_LIGHT).setBorder(new SolidBorder(BORDER, 0.5f)).setPadding(8));
        table.addCell(new Cell()
                .add(new Paragraph(formatMoney(total)).setFont(bold).setFontSize(9)
                        .setFontColor(BRAND_ORANGE).setTextAlignment(TextAlignment.RIGHT))
                .setBackgroundColor(ORANGE_LIGHT).setBorder(new SolidBorder(BORDER, 0.5f)).setPadding(8));
    }

    private void addHeaderCell(Table table, PdfFont bold, String text, TextAlignment align) {
        table.addHeaderCell(new Cell()
                .add(new Paragraph(text).setFont(bold).setFontSize(9).setFontColor(WHITE).setTextAlignment(align))
                .setBackgroundColor(BRAND_ORANGE).setBorder(new SolidBorder(BRAND_ORANGE, 1))
                .setPaddingTop(9).setPaddingBottom(9).setPaddingLeft(6).setPaddingRight(6));
    }

    private void addPageNumbers(PdfDocument pdf, PdfFont font) {
        int total = pdf.getNumberOfPages();
        for (int i = 1; i <= total; i++) {
            PdfPage page = pdf.getPage(i);
            Rectangle size = page.getPageSize();
            PdfCanvas pdfCanvas = new PdfCanvas(page.newContentStreamAfter(), page.getResources(), pdf);
            try (Canvas canvas = new Canvas(pdfCanvas, size)) {
                canvas.add(new Paragraph("Página " + i + " de " + total)
                        .setFont(font).setFontSize(8).setFontColor(TEXT_MUTED)
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setFixedPosition(size.getLeft() + 30, size.getBottom() + 18, size.getWidth() - 60));
            }
        }
    }

    // ── HTML helpers ─────────────────────────────────────────────────────────

    private String metricCard(String value, String label) {
        return "<div class=\"metric\"><div class=\"metric-value\">" + value +
                "</div><div class=\"metric-label\">" + label + "</div></div>";
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    private String translateStatus(PurchaseStatus status) {
        if (status == null) return "-";
        return switch (status) {
            case CREATED   -> "Creada";
            case APPROVED  -> "Aprobada";
            case RECEIVED  -> "Recibida";
            case CANCELLED -> "Cancelada";
        };
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "$0.00";
        return "$" + String.format("%,.2f", amount.setScale(2, RoundingMode.HALF_UP));
    }

    private String buildSubtitleText(PurchaseExportFilter filter) {
        StringBuilder sb = new StringBuilder("Generado el: ")
                .append(java.time.LocalDateTime.now().format(DATE_FMT));
        if (filter.getStatus()    != null) sb.append("  ·  Estado: ").append(translateStatus(filter.getStatus()));
        if (filter.getStartDate() != null) sb.append("  ·  Desde: ").append(filter.getStartDate().format(DATE_ONLY_FMT));
        if (filter.getEndDate()   != null) sb.append("  ·  Hasta: ").append(filter.getEndDate().format(DATE_ONLY_FMT));
        return sb.toString();
    }

    // ── Excepción ────────────────────────────────────────────────────────────

    public static class PdfGenerationException extends RuntimeException {
        public PdfGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}