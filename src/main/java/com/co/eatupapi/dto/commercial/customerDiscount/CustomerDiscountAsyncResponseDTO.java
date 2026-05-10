package com.co.eatupapi.dto.commercial.customerDiscount;

import java.time.LocalDateTime;

public class CustomerDiscountAsyncResponseDTO {

    private String message;
    private LocalDateTime requestedAt;

    public CustomerDiscountAsyncResponseDTO() {}

    public CustomerDiscountAsyncResponseDTO(String message, LocalDateTime requestedAt) {
        this.message = message;
        this.requestedAt = requestedAt;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
}