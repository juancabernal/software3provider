package com.co.eatupapi.utils.payment.invoice.factory;

import com.co.eatupapi.domain.payment.invoice.Invoice;
import com.co.eatupapi.domain.payment.invoice.InvoiceDetail;
import com.co.eatupapi.domain.payment.invoice.InvoiceStatus;
import com.co.eatupapi.dto.payment.invoice.detail.InvoiceDetailRequest;
import com.co.eatupapi.utils.payment.invoice.calculator.InvoiceCalculator.InvoiceTotals;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class InvoiceFactory {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final String NO_DISCOUNT_DESCRIPTION = "Sin descuento";

    public Invoice create(CreateInvoiceCommand command) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(command.invoiceNumber());
        invoice.setSalesId(command.salesId());
        invoice.setCustomerDiscountId(null);
        invoice.setLocationId(command.locationId());
        invoice.setTableId(normalizeBlank(command.tableId()));
        invoice.setTableSessionId(normalizeBlank(command.tableSessionId()));
        invoice.setCustomerId(command.customerId());
        invoice.setDiscountId(command.discountId());
        invoice.setDiscountPercentage(normalizePercentage(command.discountPercentage()));
        invoice.setDiscountDescription(resolveDiscountDescription(command.discountDescription()));
        invoice.setLocationName(command.locationName().trim());
        invoice.setSubtotal(command.totals().subtotal());
        invoice.setDiscountAmount(command.totals().discountAmount());
        invoice.setTaxAmount(command.totals().taxAmount());
        invoice.setTotalPrice(command.totals().total());
        invoice.setStatus(InvoiceStatus.OPEN);
        invoice.setInvoiceDate(LocalDateTime.now());

        addDetails(invoice, command.details());

        return invoice;
    }

    private void addDetails(Invoice invoice, List<InvoiceDetailRequest> requestedDetails) {
        for (InvoiceDetailRequest requestedDetail : requestedDetails) {
            InvoiceDetail detail = new InvoiceDetail();
            detail.setRecipeId(requestedDetail.getRecipeId());
            detail.setItemName(requestedDetail.getItemName().trim());
            detail.setQuantity(requestedDetail.getQuantity());
            detail.setUnitPrice(toMoney(requestedDetail.getUnitPrice()));
            detail.setSubtotal(toMoney(requestedDetail.getSubtotal()));
            detail.setDiscountAmount(ZERO);
            detail.setTaxAmount(ZERO);
            detail.setTotal(toMoney(requestedDetail.getSubtotal()));
            detail.setComment(normalizeBlank(requestedDetail.getComment()));
            invoice.addDetail(detail);
        }
    }

    private BigDecimal normalizePercentage(BigDecimal percentage) {
        if (percentage == null) {
            return BigDecimal.ZERO;
        }
        return percentage;
    }

    private BigDecimal toMoney(BigDecimal value) {
        if (value == null) {
            return ZERO;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String resolveDiscountDescription(String discountDescription) {
        String normalized = normalizeBlank(discountDescription);
        return normalized == null ? NO_DISCOUNT_DESCRIPTION : normalized;
    }

    private String normalizeBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public record CreateInvoiceCommand(
            String invoiceNumber,
            UUID locationId,
            UUID salesId,
            String tableId,
            String tableSessionId,
            UUID customerId,
            UUID discountId,
            BigDecimal discountPercentage,
            String discountDescription,
            String locationName,
            List<InvoiceDetailRequest> details,
            InvoiceTotals totals
    ) {
    }
}
