package com.co.eatupapi.utils.payment.invoice.factory;

import com.co.eatupapi.domain.payment.invoice.Invoice;
import com.co.eatupapi.domain.payment.invoice.InvoiceDetail;
import com.co.eatupapi.domain.payment.invoice.InvoiceStatus;
import com.co.eatupapi.dto.commercial.sales.SaleDetailDTO;
import com.co.eatupapi.dto.commercial.sales.SaleResponseDTO;
import com.co.eatupapi.dto.inventory.location.LocationResponseDTO;
import com.co.eatupapi.utils.payment.invoice.calculator.InvoiceCalculator.InvoiceTotals;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class InvoiceFactory {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    public Invoice create(CreateInvoiceCommand command) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(command.invoiceNumber());
        invoice.setSalesId(command.salesId());
        invoice.setCustomerDiscountId(command.customerDiscountId());
        invoice.setLocationId(command.locationId());
        invoice.setTableId(command.sale().getTableId());
        invoice.setCustomerId(command.customerId());
        invoice.setDiscountId(command.discountId());
        invoice.setDiscountPercentage(command.discountPercentage());
        invoice.setDiscountDescription(command.discountDescription());
        invoice.setLocationName(command.location().getName());
        invoice.setSubtotal(command.totals().subtotal());
        invoice.setDiscountAmount(command.totals().discountAmount());
        invoice.setTaxAmount(command.totals().taxAmount());
        invoice.setTotalPrice(command.totals().total());
        invoice.setStatus(InvoiceStatus.OPEN);
        invoice.setInvoiceDate(LocalDateTime.now());

        addDetails(invoice, command.sale().getDetails());

        return invoice;
    }

    private void addDetails(Invoice invoice, List<SaleDetailDTO> saleDetails) {
        if (saleDetails == null) {
            return;
        }

        for (SaleDetailDTO saleDetail : saleDetails) {
            InvoiceDetail detail = new InvoiceDetail();
            detail.setRecipeId(saleDetail.getRecipeId());
            detail.setItemName(resolveItemName(saleDetail));
            detail.setQuantity(saleDetail.getQuantity());
            detail.setUnitPrice(saleDetail.getUnitPrice());
            detail.setSubtotal(resolveLineSubtotal(saleDetail));
            detail.setDiscountAmount(ZERO);
            detail.setTaxAmount(ZERO);
            detail.setTotal(resolveLineSubtotal(saleDetail));
            detail.setComment(saleDetail.getRecipeLineComment());
            invoice.addDetail(detail);
        }
    }

    private String resolveItemName(SaleDetailDTO saleDetail) {
        if (saleDetail.getLineDisplayName() != null && !saleDetail.getLineDisplayName().isBlank()) {
            return saleDetail.getLineDisplayName().trim();
        }
        if (saleDetail.getRecipeId() != null) {
            return "ITEM-" + saleDetail.getRecipeId();
        }
        return "ITEM-SIN-RECETA";
    }

    private BigDecimal resolveLineSubtotal(SaleDetailDTO saleDetail) {
        if (saleDetail.getSubtotal() != null) {
            return saleDetail.getSubtotal();
        }
        if (saleDetail.getQuantity() != null && saleDetail.getUnitPrice() != null) {
            return saleDetail.getQuantity().multiply(saleDetail.getUnitPrice());
        }
        return ZERO;
    }

    public record CreateInvoiceCommand(
            String invoiceNumber,
            UUID locationId,
            UUID salesId,
            SaleResponseDTO sale,
            UUID customerDiscountId,
            UUID customerId,
            UUID discountId,
            BigDecimal discountPercentage,
            String discountDescription,
            LocationResponseDTO location,
            InvoiceTotals totals
    ) {
    }
}
