package com.co.eatupapi.dto.commercial.purchase;

import com.co.eatupapi.domain.commercial.purchase.PurchaseStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Respuesta con los datos de una compra")
public class PurchaseResponse {

    @Schema(description = "ID único de la compra")
    private UUID id;

    @Schema(description = "Número de orden generado")
    private String orderNumber;

    @Schema(description = "ID del proveedor")
    private UUID providerId;

    @Schema(description = "ID de la ubicación")
    private UUID locationId;

    @Schema(description = "Lista de productos")
    private List<PurchaseItemResponse> items;

    @Schema(description = "Total de la compra")
    private BigDecimal total;

    @Schema(description = "Estado de la compra")
    private PurchaseStatus status;

    @Schema(description = "Fecha de creación")
    private LocalDateTime createdDate;

    @Schema(description = "Fecha de última modificación")
    private LocalDateTime modifiedDate;
}