package com.co.eatupapi.dto.commercial.purchase;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Producto incluido en la compra")
public class PurchaseItemResponse {

    @Schema(description = "ID del producto")
    private UUID productId;

    @Schema(description = "Cantidad")
    private BigDecimal quantity;

    @Schema(description = "Precio unitario")
    private BigDecimal unitPrice;

    @Schema(description = "Subtotal")
    private BigDecimal subtotal;
}