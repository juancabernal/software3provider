package com.co.eatupapi.dto.payment.invoice;

import com.co.eatupapi.domain.payment.invoice.InvoiceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Datos requeridos para actualizar el estado de una factura")
public class InvoiceStatusUpdateRequest {

    @Schema(
            description = "Nuevo estado de la factura",
            example = "CLOSED",
            allowableValues = {"OPEN", "PENDING", "PARTIALLY_PAID", "PAID", "VOIDED", "CLOSED", "CANCELLED"}
    )
    @NotNull(message = "Status is required")
    private InvoiceStatus status;
}
