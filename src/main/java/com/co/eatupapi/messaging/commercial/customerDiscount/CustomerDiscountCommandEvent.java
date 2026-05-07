package com.co.eatupapi.messaging.commercial.customerDiscount;

import java.time.LocalDateTime;

public class CustomerDiscountCommandEvent {

    private String eventType;
    private String customerDiscountId;
    private LocalDateTime occurredAt;
    private Object payload;

    public CustomerDiscountCommandEvent() {}

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getCustomerDiscountId() { return customerDiscountId; }
    public void setCustomerDiscountId(String customerDiscountId) { this.customerDiscountId = customerDiscountId; }

    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }

    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }
}

