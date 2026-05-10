package com.co.eatupapi.dto.commercial.discount;

import java.time.LocalDateTime;

public class DiscountAsyncResponseDTO {

    private String message;
    private LocalDateTime requestedAt;

    public DiscountAsyncResponseDTO() {}

    public DiscountAsyncResponseDTO(String message, LocalDateTime requestedAt) {
        this.message = message;
        this.requestedAt = requestedAt;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
}