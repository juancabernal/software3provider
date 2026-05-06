package com.co.eatupapi.utils.payment.invoice.calculator;

import com.co.eatupapi.utils.payment.invoice.exceptions.InvoiceBusinessException;
import com.co.eatupapi.utils.payment.invoice.exceptions.InvoiceValidationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class InvoiceCalculator {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final int MONEY_SCALE = 2;

    public InvoiceTotals calculate(BigDecimal subtotal, BigDecimal discountPercentage) {
        BigDecimal normalizedSubtotal = requirePositiveSubtotal(subtotal);
        BigDecimal normalizedDiscountPercentage = normalizeDiscountPercentage(discountPercentage);

        BigDecimal discountAmount = normalizedSubtotal
                .multiply(normalizedDiscountPercentage)
                .divide(ONE_HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal taxAmount = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal total = normalizedSubtotal.subtract(discountAmount).add(taxAmount);

        if (total.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvoiceBusinessException("Invoice total cannot be negative after discount");
        }

        return new InvoiceTotals(
                normalizedSubtotal.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                discountAmount,
                taxAmount,
                total.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
        );
    }

    private BigDecimal requirePositiveSubtotal(BigDecimal subtotal) {
        if (subtotal == null) {
            throw new InvoiceValidationException("Sale total amount is required to create invoice");
        }
        if (subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvoiceBusinessException("Sale total amount must be greater than zero to create invoice");
        }
        return subtotal;
    }

    private BigDecimal normalizeDiscountPercentage(BigDecimal discountPercentage) {
        BigDecimal percentage = discountPercentage == null ? BigDecimal.ZERO : discountPercentage;
        if (percentage.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvoiceBusinessException("Discount percentage cannot be negative");
        }
        if (percentage.compareTo(ONE_HUNDRED) > 0) {
            throw new InvoiceBusinessException("Discount percentage cannot be greater than 100");
        }
        return percentage;
    }

    public record InvoiceTotals(
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal taxAmount,
            BigDecimal total
    ) {
    }
}
