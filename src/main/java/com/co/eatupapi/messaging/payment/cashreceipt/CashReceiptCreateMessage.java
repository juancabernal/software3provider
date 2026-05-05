package com.co.eatupapi.messaging.payment.cashreceipt;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CashReceiptCreateMessage {

    private UUID locationId;
    private UUID invoiceId;
    private UUID invoiceLocationId;
    private String invoiceStatus;
    private BigDecimal invoiceTotal;
    private BigDecimal amount;
    private UUID paymentMethodId;
    private Boolean paymentMethodActive;
    private LocalDateTime eventDate;

    public CashReceiptCreateMessage(UUID locationId,
                                    UUID invoiceId,
                                    UUID invoiceLocationId,
                                    String invoiceStatus,
                                    BigDecimal invoiceTotal,
                                    BigDecimal amount,
                                    UUID paymentMethodId,
                                    Boolean paymentMethodActive) {
        this.locationId = locationId;
        this.invoiceId = invoiceId;
        this.invoiceLocationId = invoiceLocationId;
        this.invoiceStatus = invoiceStatus;
        this.invoiceTotal = invoiceTotal;
        this.amount = amount;
        this.paymentMethodId = paymentMethodId;
        this.paymentMethodActive = paymentMethodActive;
        this.eventDate = LocalDateTime.now();
    }
}
