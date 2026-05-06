package com.co.eatupapi.messaging.commercial.discount;

import java.time.LocalDateTime;

public class DiscountCommandEvent {

    private String eventType;      // "DISCOUNT_CREATED", "DISCOUNT_UPDATED", "DISCOUNT_DELETED"
    private String discountId;
    private LocalDateTime occurredAt;
    private Object payload;

    public DiscountCommandEvent() {}

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getDiscountId() { return discountId; }
    public void setDiscountId(String discountId) { this.discountId = discountId; }

    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }

    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }
}