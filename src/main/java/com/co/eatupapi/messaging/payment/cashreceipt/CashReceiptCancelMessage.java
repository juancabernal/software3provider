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
public class CashReceiptCancelMessage {

    private UUID locationId;
    private UUID receiptId;
    private BigDecimal invoiceTotal;
    private LocalDateTime eventDate;

    public CashReceiptCancelMessage(UUID locationId, UUID receiptId, BigDecimal invoiceTotal) {
        this.locationId = locationId;
        this.receiptId = receiptId;
        this.invoiceTotal = invoiceTotal;
        this.eventDate = LocalDateTime.now();
    }
}
