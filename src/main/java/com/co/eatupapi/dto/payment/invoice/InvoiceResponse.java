package com.co.eatupapi.dto.payment.invoice;

import com.co.eatupapi.domain.payment.invoice.InvoiceStatus;
import com.co.eatupapi.dto.payment.invoice.detail.InvoiceDetailResponse;
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
@Schema(description = "Factura generada en el sistema")
public class InvoiceResponse {

    @Schema(description = "ID unico de la factura")
    private UUID invoiceId;

    @Schema(description = "Numero de factura", example = "FAC-000001")
    private String invoiceNumber;

    @Schema(description = "Estado de la factura", example = "OPEN")
    private InvoiceStatus status;

    @Schema(description = "Fecha de la factura")
    private LocalDateTime invoiceDate;

    @Schema(description = "ID de la venta asociada")
    private UUID salesId;

    @Schema(description = "ID de la relacion cliente-descuento asociada")
    private UUID customerDiscountId;

    @Schema(description = "ID de la sede asociada")
    private UUID locationId;

    @Schema(description = "ID del descuento asociado")
    private UUID discountId;

    @Schema(description = "Identificador de mesa capturado en el snapshot")
    private String tableId;

    @Schema(description = "Identificador de sesion de mesa capturado en el snapshot")
    private String tableSessionId;

    @Schema(description = "Nombre de la sede asociada")
    private String locationName;

    @Schema(description = "ID del cliente")
    private UUID customerId;

    @Schema(description = "Porcentaje de descuento aplicado")
    private BigDecimal discountPercentage;

    @Schema(description = "Descripcion del descuento aplicado")
    private String discountDescription;

    @Schema(description = "Subtotal antes de descuentos e impuestos", example = "15000.00")
    private BigDecimal subtotal;

    @Schema(description = "Valor descontado", example = "1500.00")
    private BigDecimal discountAmount;

    @Schema(description = "Valor de impuestos", example = "0.00")
    private BigDecimal taxAmount;

    @Schema(description = "Precio total", example = "15000.00")
    private BigDecimal totalPrice;

    @Schema(description = "Snapshot basico de los detalles facturados")
    private List<InvoiceDetailResponse> details;
}
