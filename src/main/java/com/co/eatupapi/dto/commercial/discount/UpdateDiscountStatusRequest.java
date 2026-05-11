package com.co.eatupapi.dto.commercial.discount;

import jakarta.validation.constraints.NotNull;

public class UpdateDiscountStatusRequest {

    @NotNull(message = "status es obligatorio")
    private Boolean status;

    public UpdateDiscountStatusRequest() {}

    public Boolean getStatus() { return status; }
    public void setStatus(Boolean status) { this.status = status; }
}