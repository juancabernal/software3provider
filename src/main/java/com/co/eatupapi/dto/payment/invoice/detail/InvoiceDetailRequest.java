package com.co.eatupapi.dto.payment.invoice.detail;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Detalle de item capturado en el snapshot de factura")
public class InvoiceDetailRequest {

    @Schema(description = "ID de receta asociada al item")
    private UUID recipeId;

    @Schema(description = "Nombre del item", example = "Menu ejecutivo")
    @NotBlank(message = "Detail item name is required")
    private String itemName;

    @Schema(description = "Cantidad del item", example = "2")
    @NotNull(message = "Detail quantity is required")
    @DecimalMin(value = "0.0001", message = "Detail quantity must be greater than zero")
    private BigDecimal quantity;

    @Schema(description = "Precio unitario del item", example = "15000.00")
    @NotNull(message = "Detail unit price is required")
    @DecimalMin(value = "0.01", message = "Detail unit price must be greater than zero")
    private BigDecimal unitPrice;

    @Schema(description = "Subtotal del item", example = "30000.00")
    @NotNull(message = "Detail subtotal is required")
    @DecimalMin(value = "0.01", message = "Detail subtotal must be greater than zero")
    private BigDecimal subtotal;

    @Schema(description = "Comentario del item")
    private String comment;
}
