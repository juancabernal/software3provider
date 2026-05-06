package com.co.eatupapi.dto.payment.invoice.detail;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Detalle historico asociado a una factura")
public class InvoiceDetailResponse {

    @Schema(description = "ID unico del detalle de factura")
    private UUID detailId;

    @Schema(description = "ID de receta capturado desde la venta")
    private UUID recipeId;

    @Schema(description = "Nombre visible del item al momento de facturar")
    private String itemName;

    @Schema(description = "Cantidad facturada")
    private BigDecimal quantity;

    @Schema(description = "Precio unitario historico")
    private BigDecimal unitPrice;

    @Schema(description = "Subtotal de la linea")
    private BigDecimal subtotal;

    @Schema(description = "Descuento aplicado a la linea")
    private BigDecimal discountAmount;

    @Schema(description = "Impuesto aplicado a la linea")
    private BigDecimal taxAmount;

    @Schema(description = "Total de la linea")
    private BigDecimal total;

    @Schema(description = "Comentario capturado desde la venta")
    private String comment;
}
