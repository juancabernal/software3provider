package com.co.eatupapi.messaging.payment.invoice;

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
public class InvoiceCancelMessage {

    private UUID locationId;
    private UUID invoiceId;
    private String reason;
    private LocalDateTime eventDate;
}
