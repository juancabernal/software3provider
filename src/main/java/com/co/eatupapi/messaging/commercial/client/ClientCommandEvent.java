package com.co.eatupapi.messaging.commercial.client;

import java.time.LocalDateTime;

public class ClientCommandEvent {

    private String eventType;
    private String clientId;
    private LocalDateTime occurredAt;
    private Object payload;

    public ClientCommandEvent() {
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }
}
