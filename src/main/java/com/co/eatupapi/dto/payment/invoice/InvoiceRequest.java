package com.co.eatupapi.dto.payment.invoice;

import com.co.eatupapi.dto.payment.invoice.detail.InvoiceDetailRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Datos requeridos para crear una factura")
public class InvoiceRequest {

    @Schema(description = "ID de la venta asociada")
    @NotNull(message = "Sales ID is required")
    private UUID salesId;

    @Schema(description = "ID de la sede asociada")
    @NotNull(message = "Location ID is required")
    private UUID locationId;

    @Schema(description = "Nombre de la sede asociada")
    @NotBlank(message = "Location name is required")
    private String locationName;

    @Schema(description = "ID de mesa asociado al snapshot")
    private String tableId;

    @Schema(description = "ID de sesion de mesa asociado al snapshot")
    private String tableSessionId;

    @Schema(description = "ID del cliente asociado")
    private UUID customerId;

    @Schema(description = "ID del descuento asociado")
    private UUID discountId;

    @Schema(description = "Porcentaje de descuento aplicado", example = "10.00")
    @DecimalMin(value = "0.00", message = "Discount percentage cannot be negative")
    @DecimalMax(value = "100.00", message = "Discount percentage cannot be greater than 100")
    private BigDecimal discountPercentage;

    @Schema(description = "Descripcion del descuento aplicado", example = "Cliente frecuente")
    private String discountDescription;

    @Schema(description = "Subtotal antes de descuento", example = "100000.00")
    @NotNull(message = "Subtotal is required")
    @DecimalMin(value = "0.01", message = "Subtotal must be greater than zero")
    private BigDecimal subtotal;

    @Schema(description = "Total final de la factura", example = "90000.00")
    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.01", message = "Total amount must be greater than zero")
    private BigDecimal totalAmount;

    @Schema(description = "Snapshot de detalles de factura")
    @NotEmpty(message = "Invoice details are required")
    @Valid
    private List<InvoiceDetailRequest> details;
}
