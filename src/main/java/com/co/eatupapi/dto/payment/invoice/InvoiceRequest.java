package com.co.eatupapi.dto.payment.invoice;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Datos requeridos para crear una factura")
public class InvoiceRequest {

    @Schema(description = "Número de factura", example = "INV-001")
    @NotNull(message = "Invoice number is required")
    private String invoiceNumber;

    @Schema(description = "ID de la venta asociada")
    @NotNull(message = "Sales ID is required")
    private UUID salesId;

    @Schema(description = "ID de la relación cliente-descuento")
    private UUID customerDiscountId;

    @Schema(description = "ID de la sede asociada")
    @NotNull(message = "Location ID is required")
    private UUID locationId;
}
