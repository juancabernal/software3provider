package com.co.eatupapi.dto.payment.paymentmethod;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Solicitud para crear un nuevo método de pago")
public class CreatePaymentMethodRequest {

    @Schema(description = "Nombre del método de pago", example = "Efectivo")
    private String name;

    @Schema(description = "Descripción del método de pago")
    private String description;

    @Schema(description = "Indica si el método de pago está activo al crearse", defaultValue = "true")
    private Boolean active;
}
