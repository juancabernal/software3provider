package com.co.eatupapi.messaging.payment.invoice;

import com.co.eatupapi.domain.payment.invoice.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceStatusUpdateMessage {

    private UUID locationId;
    private UUID invoiceId;
    private String invoiceNumber;
    private UUID salesId;
    private String tableId;
    private String tableSessionId;
    private InvoiceStatus previousStatus;
    private InvoiceStatus status;
    private LocalDateTime eventDate;
}
