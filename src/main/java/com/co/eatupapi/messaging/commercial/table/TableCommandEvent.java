package com.co.eatupapi.messaging.commercial.table;

import java.time.LocalDateTime;
import java.util.Map;

public class TableCommandEvent {

    private String eventType;
    private String tableId;
    private String sessionId;
    private String reservationId;
    private LocalDateTime occurredAt;
    private Map<String, Object> payload;

    public TableCommandEvent() {
    }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getTableId() { return tableId; }
    public void setTableId(String tableId) { this.tableId = tableId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }

    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }

    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }
}