package com.co.eatupapi.dto.payment.invoice;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Respuesta aceptada para comandos asincronos de factura")
public class InvoiceAcceptedResponse {

    @Schema(description = "Mensaje de confirmacion")
    private String message;

    @Schema(description = "ID de la factura asociada al comando")
    private UUID invoiceId;

    @Schema(description = "Estado HTTP/negocio de aceptacion")
    private String status;

    @Schema(description = "Fecha de aceptacion del comando")
    private LocalDateTime acceptedAt;
}
