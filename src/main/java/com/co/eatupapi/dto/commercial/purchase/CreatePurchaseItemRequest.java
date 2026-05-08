package com.co.eatupapi.dto.commercial.purchase;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Producto incluido en la compra")
public class CreatePurchaseItemRequest {

    @Schema(description = "ID del producto")
    @NotNull(message = "Product ID is required")
    private UUID productId;

    @Schema(description = "Cantidad", example = "10.00")
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private BigDecimal quantity;

    @Schema(description = "Precio unitario", example = "25000.00")
    @NotNull(message = "Unit price is required")
    @Positive(message = "Unit price must be greater than zero")
    private BigDecimal unitPrice;
}