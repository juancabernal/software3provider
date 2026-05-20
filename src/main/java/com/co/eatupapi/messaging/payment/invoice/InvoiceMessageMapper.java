package com.co.eatupapi.messaging.payment.invoice;

import com.co.eatupapi.domain.payment.invoice.Invoice;
import com.co.eatupapi.domain.payment.invoice.InvoiceDetail;
import com.co.eatupapi.domain.payment.invoice.InvoiceStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class InvoiceMessageMapper {

    public InvoiceCreateMessage toCreateMessage(Invoice invoice) {
        List<InvoiceItemMessage> detailMessages = invoice.getDetails() == null
                ? List.of()
                : invoice.getDetails().stream().map(this::toItemMessage).toList();

        return new InvoiceCreateMessage(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getLocationId(),
                invoice.getSalesId(),
                invoice.getCustomerDiscountId(),
                invoice.getCustomerId(),
                invoice.getDiscountId(),
                invoice.getDiscountPercentage(),
                invoice.getDiscountDescription(),
                invoice.getTableId(),
                invoice.getTableSessionId(),
                invoice.getLocationName(),
                invoice.getSubtotal(),
                invoice.getDiscountAmount(),
                invoice.getTaxAmount(),
                invoice.getTotalPrice(),
                invoice.getStatus(),
                invoice.getInvoiceDate(),
                detailMessages,
                LocalDateTime.now()
        );
    }

    public InvoiceCancelMessage toCancelMessage(Invoice invoice,
                                                InvoiceStatus previousStatus,
                                                String reason) {
        return new InvoiceCancelMessage(
                invoice.getLocationId(),
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getSalesId(),
                previousStatus,
                invoice.getStatus(),
                reason,
                LocalDateTime.now()
        );
    }

    public InvoiceMarkPaidMessage toMarkPaidMessage(Invoice invoice, InvoiceStatus previousStatus) {
        return new InvoiceMarkPaidMessage(
                invoice.getLocationId(),
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getSalesId(),
                invoice.getTableId(),
                invoice.getTableSessionId(),
                null,
                null,
                null,
                previousStatus,
                invoice.getStatus(),
                LocalDateTime.now()
        );
    }

    public InvoiceStatusUpdateMessage toStatusUpdateMessage(Invoice invoice, InvoiceStatus previousStatus) {
        return new InvoiceStatusUpdateMessage(
                invoice.getLocationId(),
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getSalesId(),
                invoice.getTableId(),
                invoice.getTableSessionId(),
                previousStatus,
                invoice.getStatus(),
                LocalDateTime.now()
        );
    }

    private InvoiceItemMessage toItemMessage(InvoiceDetail detail) {
        return new InvoiceItemMessage(
                detail.getRecipeId(),
                detail.getItemName(),
                detail.getQuantity(),
                detail.getUnitPrice(),
                detail.getSubtotal(),
                detail.getDiscountAmount(),
                detail.getTaxAmount(),
                detail.getTotal(),
                detail.getComment()
        );
    }
}
