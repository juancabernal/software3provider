package com.co.eatupapi.utils.payment.invoice.validation;

import com.co.eatupapi.domain.payment.invoice.InvoiceStatus;
import com.co.eatupapi.utils.payment.invoice.exceptions.InvoiceBusinessException;
import com.co.eatupapi.utils.payment.invoice.exceptions.InvoiceValidationException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class InvoiceStateValidator {

    private final Map<InvoiceStatus, Set<InvoiceStatus>> allowedTransitions = new EnumMap<>(InvoiceStatus.class);

    public InvoiceStateValidator() {
        allowedTransitions.put(InvoiceStatus.OPEN, EnumSet.of(
                InvoiceStatus.PENDING,
                InvoiceStatus.PAID,
                InvoiceStatus.CLOSED,
                InvoiceStatus.CANCELLED,
                InvoiceStatus.VOIDED
        ));
        allowedTransitions.put(InvoiceStatus.PENDING, EnumSet.of(
                InvoiceStatus.PARTIALLY_PAID,
                InvoiceStatus.PAID,
                InvoiceStatus.VOIDED,
                InvoiceStatus.CANCELLED
        ));
        allowedTransitions.put(InvoiceStatus.PARTIALLY_PAID, EnumSet.of(InvoiceStatus.PAID));
        allowedTransitions.put(InvoiceStatus.PAID, EnumSet.noneOf(InvoiceStatus.class));
        allowedTransitions.put(InvoiceStatus.VOIDED, EnumSet.noneOf(InvoiceStatus.class));
        allowedTransitions.put(InvoiceStatus.CLOSED, EnumSet.noneOf(InvoiceStatus.class));
        allowedTransitions.put(InvoiceStatus.CANCELLED, EnumSet.noneOf(InvoiceStatus.class));
    }

    public void validateTransition(InvoiceStatus currentStatus, InvoiceStatus newStatus) {
        if (currentStatus == null) {
            throw new InvoiceValidationException("Current invoice status is required");
        }
        if (newStatus == null) {
            throw new InvoiceValidationException("Status is required");
        }
        if (currentStatus == newStatus) {
            throw new InvoiceBusinessException("Invoice already has this status");
        }
        if (currentStatus == InvoiceStatus.PAID && newStatus == InvoiceStatus.VOIDED) {
            throw new InvoiceBusinessException(
                    "Paid invoices cannot be voided until CashReceipt supports payment reversal"
            );
        }
        if (!allowedTransitions.getOrDefault(currentStatus, Set.of()).contains(newStatus)) {
            throw new InvoiceBusinessException(
                    "Invalid invoice status transition from " + currentStatus + " to " + newStatus
            );
        }
    }
}
