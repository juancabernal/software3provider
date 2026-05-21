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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // ── Paleta base ──────────────────────────────────────────────────────────
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

    // ── Paleta de estados ────────────────────────────────────────────────────
    private static final DeviceRgb STATUS_CREATED_BG   = new DeviceRgb(219, 234, 254);
    private static final DeviceRgb STATUS_CREATED_FG   = new DeviceRgb( 29,  78, 216);
    private static final DeviceRgb STATUS_APPROVED_BG  = new DeviceRgb(220, 252, 231);
    private static final DeviceRgb STATUS_APPROVED_FG  = new DeviceRgb( 21, 128,  61);
    private static final DeviceRgb STATUS_RECEIVED_BG  = new DeviceRgb(209, 250, 229);
    private static final DeviceRgb STATUS_RECEIVED_FG  = new DeviceRgb(  6,  95,  70);
    private static final DeviceRgb STATUS_CANCELLED_BG = new DeviceRgb(254, 226, 226);
    private static final DeviceRgb STATUS_CANCELLED_FG = new DeviceRgb(185,  28,  28);

    // ── Paleta resumen ejecutivo ─────────────────────────────────────────────
    private static final DeviceRgb SUMMARY_BG     = new DeviceRgb(255, 247, 237);
    private static final DeviceRgb SUMMARY_BORDER = new DeviceRgb(253, 186, 116);

    // ── Separador entre órdenes ──────────────────────────────────────────────
    private static final DeviceRgb ORDER_SEPARATOR = new DeviceRgb(255, 107, 53);

    private final PurchaseRepository  purchaseRepository;
    private final ProviderRepository  providerRepository;
    private final ProductNameResolver productNameResolver;

    public PurchaseExportServiceImpl(PurchaseRepository purchaseRepository,
                                     ProviderRepository providerRepository,
                                     ProductNameResolver productNameResolver) {
        this.purchaseRepository  = purchaseRepository;
        this.providerRepository  = providerRepository;
        this.productNameResolver = productNameResolver;
    }

    // ── Specification dinámica ───────────────────────────────────────────────

    private Specification<PurchaseDomain> buildSpec(UUID locationId, PurchaseExportFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("locationId"), locationId));
            predicates.add(cb.isFalse(root.get("deleted")));
            if (filter.getStatus() != null)
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            if (filter.getProviderId() != null)
                predicates.add(cb.equal(root.get("providerId"), filter.getProviderId()));
            if (filter.getStartDate() != null)
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get(CREATED_DATE_FIELD), filter.getStartDate().atStartOfDay()));
            if (filter.getEndDate() != null)
                predicates.add(cb.lessThanOrEqualTo(
                        root.get(CREATED_DATE_FIELD), filter.getEndDate().atTime(23, 59, 59)));
            query.orderBy(cb.desc(root.get(CREATED_DATE_FIELD)));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // ── Exportar ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public byte[] exportToPdf(UUID locationId, PurchaseExportFilter filter) {

        List<PurchaseDomain> purchases = purchaseRepository.findAll(buildSpec(locationId, filter));

        // Resolver proveedores
        List<UUID> providerIds = purchases.stream()
                .map(PurchaseDomain::getProviderId).distinct().toList();
        Map<UUID, String> providerNames = providerRepository.findAllById(providerIds)
                .stream()
                .collect(Collectors.toMap(ProviderDomain::getId, ProviderDomain::getBusinessName));

        // Resolver nombres de producto (sin acoplamiento a inventory)
        List<UUID> productIds = purchases.stream()
                .filter(p -> p.getItems() != null)
                .flatMap(p -> p.getItems().stream())
                .map(PurchaseItemDomain::getProductId)
                .distinct().toList();
        Map<UUID, String> productNames = productNameResolver.resolveNames(productIds);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont bold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            PdfWriter   writer   = new PdfWriter(out);
            PdfDocument pdf      = new PdfDocument(writer);
            Document    document = new Document(pdf, PageSize.A4.rotate());
            document.setMargins(40, 30, 50, 30);
            document.setFont(regular);

            // ── Título principal ─────────────────────────────────────────────
            document.add(new Paragraph("Reporte de Compras")
                    .setFont(bold).setFontSize(22)
                    .setFontColor(BRAND_ORANGE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(6));

            // ── Bloque de filtros aplicados ──────────────────────────────────
            document.add(buildFilterBlock(filter, regular));

            // ── Sin resultados ───────────────────────────────────────────────
            if (purchases.isEmpty()) {
                document.add(new Paragraph("No se encontraron compras con los filtros seleccionados.")
                        .setFont(regular).setFontSize(11)
                        .setFontColor(TEXT_MUTED)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginTop(20));
                document.close();
                return out.toByteArray();
            }

            // ── Resumen ejecutivo ────────────────────────────────────────────
            document.add(buildSummaryBlock(purchases, providerNames, regular, bold));

            // ── Tabla principal ──────────────────────────────────────────────
            float[] colWidths = {130f, 160f, 95f, 110f, 45f, 95f};
            Table table = new Table(UnitValue.createPointArray(colWidths));
            table.setWidth(UnitValue.createPercentValue(100));

            addHeaderCell(table, bold, "N° Orden",  TextAlignment.LEFT);
            addHeaderCell(table, bold, "Proveedor", TextAlignment.LEFT);
            addHeaderCell(table, bold, "Estado",    TextAlignment.LEFT);
            addHeaderCell(table, bold, "Fecha",     TextAlignment.LEFT);
            addHeaderCell(table, bold, "Ítems",     TextAlignment.RIGHT);
            addHeaderCell(table, bold, "Total",     TextAlignment.RIGHT);

            // Agrupar por proveedor para subtotales
            Map<UUID, List<PurchaseDomain>> byProvider = purchases.stream()
                    .collect(Collectors.groupingBy(PurchaseDomain::getProviderId,
                            LinkedHashMap::new, Collectors.toList()));

            BigDecimal grandTotal  = BigDecimal.ZERO;
            boolean    altRow      = false;
            boolean    firstOrder  = true;

            for (Map.Entry<UUID, List<PurchaseDomain>> entry : byProvider.entrySet()) {
                UUID   providerId       = entry.getKey();
                List<PurchaseDomain> pp = entry.getValue();
                String providerName     = providerNames.getOrDefault(providerId, providerId.toString());
                BigDecimal providerTotal = BigDecimal.ZERO;

                for (PurchaseDomain p : pp) {
                    DeviceRgb rowBg = altRow ? ORANGE_PALE : WHITE;
                    altRow = !altRow;

                    int itemCount = p.getItems() != null ? p.getItems().size() : 0;

                    addOrderRow(table, regular, bold, rowBg, p, providerName, itemCount, firstOrder);
                    firstOrder = false;

                    BigDecimal pTotal = p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO;
                    providerTotal = providerTotal.add(pTotal);
                    grandTotal    = grandTotal.add(pTotal);

                    // Filas de ítems con nombre real de producto
                    if (p.getItems() != null) {
                        for (PurchaseItemDomain item : p.getItems()) {
                            String productName = productNames.getOrDefault(
                                    item.getProductId(), "Producto desconocido");
                            addItemRow(table, regular, item, productName);
                        }
                    }
                }

                // Subtotal por proveedor
                addProviderSubtotalRow(table, bold, providerName, pp.size(), providerTotal);
            }

            // Total general
            addTotalRow(table, bold, purchases.size(), grandTotal);

            document.add(table);

            // Números de página
            addPageNumbers(pdf, regular);

            document.close();

        } catch (IOException e) {
            throw new PdfGenerationException("Error generando el PDF de compras", e);
        }

        return out.toByteArray();
    }

    // ── Bloque de filtros aplicados ──────────────────────────────────────────

    private Table buildFilterBlock(PurchaseExportFilter filter, PdfFont regular) {
        Table wrapper = new Table(UnitValue.createPercentArray(new float[]{1}));
        wrapper.setWidth(UnitValue.createPercentValue(100));
        wrapper.setMarginBottom(10);

        StringBuilder content = new StringBuilder();
        content.append("Generado el: ").append(java.time.LocalDateTime.now().format(DATE_FMT));
        if (filter.getStatus()     != null) content.append("     Estado: ").append(translateStatus(filter.getStatus()));
        if (filter.getProviderId() != null) content.append("     Proveedor filtrado");
        if (filter.getStartDate()  != null) content.append("     Desde: ").append(filter.getStartDate().format(DATE_ONLY_FMT));
        if (filter.getEndDate()    != null) content.append("     Hasta: ").append(filter.getEndDate().format(DATE_ONLY_FMT));

        wrapper.addCell(new Cell()
                .add(new Paragraph(content.toString())
                        .setFont(regular).setFontSize(8)
                        .setFontColor(TEXT_MUTED))
                .setBackgroundColor(new DeviceRgb(249, 250, 251))
                .setBorder(new SolidBorder(new DeviceRgb(209, 213, 219), 0.5f))
                .setPaddingTop(6).setPaddingBottom(6)
                .setPaddingLeft(10).setPaddingRight(10));

        return wrapper;
    }

    // ── Resumen ejecutivo ────────────────────────────────────────────────────

    private Table buildSummaryBlock(List<PurchaseDomain> purchases,
                                    Map<UUID, String> providerNames,
                                    PdfFont regular, PdfFont bold) {

        BigDecimal totalGastado = purchases.stream()
                .map(p -> p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String topProvider = purchases.stream()
                .collect(Collectors.groupingBy(PurchaseDomain::getProviderId, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> providerNames.getOrDefault(e.getKey(), "—"))
                .orElse("—");

        BigDecimal promedio = purchases.isEmpty() ? BigDecimal.ZERO :
                totalGastado.divide(BigDecimal.valueOf(purchases.size()), 2, RoundingMode.HALF_UP);

        long canceladas = purchases.stream()
                .filter(p -> PurchaseStatus.CANCELLED.equals(p.getStatus())).count();

        Table summary = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1}));
        summary.setWidth(UnitValue.createPercentValue(100));
        summary.setMarginBottom(14);

        addMetricCell(summary, bold, regular, "Total compras",     String.valueOf(purchases.size()));
        addMetricCell(summary, bold, regular, "Total gastado",     formatMoney(totalGastado));
        addMetricCell(summary, bold, regular, "Proveedor top",     topProvider);
        addMetricCell(summary, bold, regular, "Promedio / compra", formatMoney(promedio));

        return summary;
    }

    private void addMetricCell(Table table, PdfFont bold, PdfFont regular,
                               String label, String value) {
        table.addCell(new Cell()
                .add(new Paragraph(value)
                        .setFont(bold).setFontSize(13)
                        .setFontColor(BRAND_ORANGE)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(2))
                .add(new Paragraph(label)
                        .setFont(regular).setFontSize(7.5f)
                        .setFontColor(TEXT_MUTED)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(SUMMARY_BG)
                .setBorder(new SolidBorder(SUMMARY_BORDER, 0.8f))
                .setPaddingTop(10).setPaddingBottom(10)
                .setPaddingLeft(8).setPaddingRight(8));
    }

    // ── Fila de orden con separador visual ───────────────────────────────────

    private void addOrderRow(Table table, PdfFont regular, PdfFont bold,
                             DeviceRgb rowBg, PurchaseDomain p,
                             String providerName, int itemCount, boolean firstRow) {

        SolidBorder topBorder     = firstRow
                ? new SolidBorder(BORDER, 0.5f)
                : new SolidBorder(ORDER_SEPARATOR, 1.2f);
        SolidBorder defaultBorder = new SolidBorder(BORDER, 0.5f);

        // N° Orden
        table.addCell(new Cell()
                .add(new Paragraph(p.getOrderNumber())
                        .setFont(bold).setFontSize(8.5f).setFontColor(TEXT_MAIN))
                .setBackgroundColor(rowBg)
                .setBorderTop(topBorder).setBorderBottom(defaultBorder)
                .setBorderLeft(defaultBorder).setBorderRight(defaultBorder)
                .setPaddingTop(8).setPaddingBottom(8).setPaddingLeft(6).setPaddingRight(6));

        // Proveedor
        table.addCell(new Cell()
                .add(new Paragraph(providerName)
                        .setFont(regular).setFontSize(8.5f).setFontColor(TEXT_MAIN))
                .setBackgroundColor(rowBg)
                .setBorderTop(topBorder).setBorderBottom(defaultBorder)
                .setBorderLeft(defaultBorder).setBorderRight(defaultBorder)
                .setPaddingTop(8).setPaddingBottom(8).setPaddingLeft(6).setPaddingRight(6));

        // Estado con color
        addStatusCell(table, bold, p.getStatus(), topBorder, defaultBorder);

        // Fecha
        table.addCell(new Cell()
                .add(new Paragraph(p.getCreatedDate() != null
                        ? p.getCreatedDate().format(DATE_FMT) : "-")
                        .setFont(regular).setFontSize(8.5f).setFontColor(TEXT_MAIN))
                .setBackgroundColor(rowBg)
                .setBorderTop(topBorder).setBorderBottom(defaultBorder)
                .setBorderLeft(defaultBorder).setBorderRight(defaultBorder)
                .setPaddingTop(8).setPaddingBottom(8).setPaddingLeft(6).setPaddingRight(6));

        // Ítems
        table.addCell(new Cell()
                .add(new Paragraph(String.valueOf(itemCount))
                        .setFont(regular).setFontSize(8.5f).setFontColor(TEXT_MAIN)
                        .setTextAlignment(TextAlignment.RIGHT))
                .setBackgroundColor(rowBg)
                .setBorderTop(topBorder).setBorderBottom(defaultBorder)
                .setBorderLeft(defaultBorder).setBorderRight(defaultBorder)
                .setPaddingTop(8).setPaddingBottom(8).setPaddingLeft(6).setPaddingRight(6));

        // Total
        table.addCell(new Cell()
                .add(new Paragraph(formatMoney(p.getTotal()))
                        .setFont(bold).setFontSize(8.5f).setFontColor(TEXT_MAIN)
                        .setTextAlignment(TextAlignment.RIGHT))
                .setBackgroundColor(rowBg)
                .setBorderTop(topBorder).setBorderBottom(defaultBorder)
                .setBorderLeft(defaultBorder).setBorderRight(defaultBorder)
                .setPaddingTop(8).setPaddingBottom(8).setPaddingLeft(6).setPaddingRight(6));
    }

    // ── Celda de estado con color ────────────────────────────────────────────

    private void addStatusCell(Table table, PdfFont bold, PurchaseStatus status,
                               SolidBorder topBorder, SolidBorder defaultBorder) {
        DeviceRgb bg;
        DeviceRgb fg;

        if (status == null) {
            bg = new DeviceRgb(243, 244, 246);
            fg = TEXT_MUTED;
        } else {
            switch (status) {
                case CREATED   -> { bg = STATUS_CREATED_BG;   fg = STATUS_CREATED_FG;   }
                case APPROVED  -> { bg = STATUS_APPROVED_BG;  fg = STATUS_APPROVED_FG;  }
                case RECEIVED  -> { bg = STATUS_RECEIVED_BG;  fg = STATUS_RECEIVED_FG;  }
                case CANCELLED -> { bg = STATUS_CANCELLED_BG; fg = STATUS_CANCELLED_FG; }
                default        -> { bg = new DeviceRgb(243, 244, 246); fg = TEXT_MUTED; }
            }
        }

        table.addCell(new Cell()
                .add(new Paragraph(translateStatus(status))
                        .setFont(bold).setFontSize(7.5f)
                        .setFontColor(fg)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(bg)
                .setBorderTop(topBorder).setBorderBottom(defaultBorder)
                .setBorderLeft(defaultBorder).setBorderRight(defaultBorder)
                .setPaddingTop(8).setPaddingBottom(8).setPaddingLeft(4).setPaddingRight(4));
    }

    // ── Fila de ítem con nombre de producto ──────────────────────────────────

    private void addItemRow(Table table, PdfFont regular,
                            PurchaseItemDomain item, String productName) {
        // Col 1+2: nombre del producto con indentación
        table.addCell(new Cell(1, 2)
                .add(new Paragraph("↳  " + productName)
                        .setFont(regular).setFontSize(7.5f)
                        .setFontColor(TEXT_ITEM))
                .setBackgroundColor(ITEM_BG)
                .setBorderTop(new SolidBorder(BORDER_LIGHT, 0.3f))
                .setBorderBottom(new SolidBorder(BORDER_LIGHT, 0.3f))
                .setBorderLeft(new SolidBorder(BORDER, 0.5f))
                .setBorderRight(new SolidBorder(BORDER_LIGHT, 0.3f))
                .setPaddingLeft(24).setPaddingTop(4).setPaddingBottom(4).setPaddingRight(6));

        // Col 3: vacío
        table.addCell(spacerCell());

        // Col 4: precio unitario
        table.addCell(new Cell()
                .add(new Paragraph(formatMoney(item.getUnitPrice()))
                        .setFont(regular).setFontSize(7.5f)
                        .setFontColor(TEXT_ITEM)
                        .setTextAlignment(TextAlignment.LEFT))
                .setBackgroundColor(ITEM_BG)
                .setBorder(new SolidBorder(BORDER_LIGHT, 0.3f))
                .setPadding(4));

        // Col 5: cantidad
        table.addCell(new Cell()
                .add(new Paragraph(item.getQuantity().setScale(2, RoundingMode.HALF_UP).toPlainString())
                        .setFont(regular).setFontSize(7.5f)
                        .setFontColor(TEXT_ITEM)
                        .setTextAlignment(TextAlignment.RIGHT))
                .setBackgroundColor(ITEM_BG)
                .setBorder(new SolidBorder(BORDER_LIGHT, 0.3f))
                .setPadding(4));

        // Col 6: subtotal
        table.addCell(new Cell()
                .add(new Paragraph(formatMoney(item.getSubtotal()))
                        .setFont(regular).setFontSize(7.5f)
                        .setFontColor(TEXT_ITEM)
                        .setTextAlignment(TextAlignment.RIGHT))
                .setBackgroundColor(ITEM_BG)
                .setBorderTop(new SolidBorder(BORDER_LIGHT, 0.3f))
                .setBorderBottom(new SolidBorder(BORDER_LIGHT, 0.3f))
                .setBorderLeft(new SolidBorder(BORDER_LIGHT, 0.3f))
                .setBorderRight(new SolidBorder(BORDER, 0.5f))
                .setPadding(4));
    }

    private Cell spacerCell() {
        return new Cell()
                .add(new Paragraph(""))
                .setBackgroundColor(ITEM_BG)
                .setBorder(new SolidBorder(BORDER_LIGHT, 0.3f))
                .setPadding(4);
    }

    // ── Subtotal por proveedor ───────────────────────────────────────────────

    private void addProviderSubtotalRow(Table table, PdfFont bold,
                                        String providerName, int count, BigDecimal subtotal) {
        String label = "Subtotal " + providerName
                + " (" + count + " " + (count == 1 ? "compra" : "compras") + ")";

        table.addCell(new Cell(1, 5)
                .add(new Paragraph(label)
                        .setFont(bold).setFontSize(8)
                        .setFontColor(TEXT_MAIN)
                        .setTextAlignment(TextAlignment.RIGHT))
                .setBackgroundColor(ORANGE_PALE)
                .setBorder(new SolidBorder(BORDER, 0.5f))
                .setPaddingTop(6).setPaddingBottom(6).setPaddingLeft(6).setPaddingRight(6));

        table.addCell(new Cell()
                .add(new Paragraph(formatMoney(subtotal))
                        .setFont(bold).setFontSize(8)
                        .setFontColor(BRAND_ORANGE)
                        .setTextAlignment(TextAlignment.RIGHT))
                .setBackgroundColor(ORANGE_PALE)
                .setBorder(new SolidBorder(BORDER, 0.5f))
                .setPaddingTop(6).setPaddingBottom(6).setPaddingLeft(6).setPaddingRight(6));
    }

    // ── Total general ────────────────────────────────────────────────────────

    private void addTotalRow(Table table, PdfFont bold, int count, BigDecimal total) {
        String label = "Total general (" + count + " " + (count == 1 ? "compra" : "compras") + ")";

        table.addCell(new Cell(1, 5)
                .add(new Paragraph(label)
                        .setFont(bold).setFontSize(9)
                        .setFontColor(TEXT_MAIN)
                        .setTextAlignment(TextAlignment.RIGHT))
                .setBackgroundColor(ORANGE_LIGHT)
                .setBorder(new SolidBorder(BORDER, 0.5f))
                .setPadding(8));

        table.addCell(new Cell()
                .add(new Paragraph(formatMoney(total))
                        .setFont(bold).setFontSize(9)
                        .setFontColor(BRAND_ORANGE)
                        .setTextAlignment(TextAlignment.RIGHT))
                .setBackgroundColor(ORANGE_LIGHT)
                .setBorder(new SolidBorder(BORDER, 0.5f))
                .setPadding(8));
    }

    // ── Helper cabecera tabla ─────────────────────────────────────────────────

    private void addHeaderCell(Table table, PdfFont bold, String text, TextAlignment align) {
        table.addHeaderCell(new Cell()
                .add(new Paragraph(text)
                        .setFont(bold).setFontSize(9)
                        .setFontColor(WHITE)
                        .setTextAlignment(align))
                .setBackgroundColor(BRAND_ORANGE)
                .setBorder(new SolidBorder(BRAND_ORANGE, 1))
                .setPaddingTop(9).setPaddingBottom(9)
                .setPaddingLeft(6).setPaddingRight(6));
    }

    // ── Números de página ────────────────────────────────────────────────────

    private void addPageNumbers(PdfDocument pdf, PdfFont font) {
        int totalPages = pdf.getNumberOfPages();
        for (int i = 1; i <= totalPages; i++) {
            PdfPage   page      = pdf.getPage(i);
            Rectangle size      = page.getPageSize();
            PdfCanvas pdfCanvas = new PdfCanvas(
                    page.newContentStreamAfter(), page.getResources(), pdf);
            try (Canvas canvas = new Canvas(pdfCanvas, size)) {
                canvas.add(new Paragraph("Página " + i + " de " + totalPages)
                        .setFont(font).setFontSize(8)
                        .setFontColor(TEXT_MUTED)
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setFixedPosition(
                                size.getLeft()   + 30,
                                size.getBottom() + 18,
                                size.getWidth()  - 60));
            }
        }
    }

    // ── Utilidades ───────────────────────────────────────────────────────────

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

    // ── Excepción específica ─────────────────────────────────────────────────

    public static class PdfGenerationException extends RuntimeException {
        public PdfGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}