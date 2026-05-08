package com.co.eatupapi.messaging.commercial.table;

import com.co.eatupapi.dto.commercial.table.TableDTO;
import com.co.eatupapi.dto.commercial.table.TableReservationDTO;
import com.co.eatupapi.dto.commercial.table.TableSessionDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class TableEventPublisherBroker implements TableEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${rabbitmq.exchange.commercial}")
    private String exchange;

    @Value("${rabbitmq.routing-key.table-crud}")
    private String tableCrudRoutingKey;

    @Value("${rabbitmq.routing-key.table-session}")
    private String tableSessionRoutingKey;

    @Value("${rabbitmq.routing-key.table-reservation}")
    private String tableReservationRoutingKey;

    public TableEventPublisherBroker(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    // ── Table ─────────────────────────────────────────────────────────────────

    @Override
    public void publishTableCreated(TableDTO table) {
        sendEvent("TABLE_CREATED", null, null, null, table, tableCrudRoutingKey);
    }

    @Override
    public void publishTableUpdated(TableDTO table) {
        sendEvent("TABLE_UPDATED", table.getId(), null, null, table, tableCrudRoutingKey);
    }

    @Override
    public void publishTableDeactivated(String tableId) {
        sendEvent("TABLE_DEACTIVATED", tableId, null, null, null, tableCrudRoutingKey);
    }

    @Override
    public void publishSessionOpened(TableSessionDTO session) {
        sendEvent("TABLE_SESSION_OPENED", session.getTableId(), null, null, session, tableSessionRoutingKey);
    }

    @Override
    public void publishSessionClosed(TableSessionDTO session) {
        sendEvent("TABLE_SESSION_CLOSED", session.getTableId(), session.getId(), null, null, tableSessionRoutingKey);
    }

    @Override
    public void publishSessionUpdated(String tableId, String sessionId, Integer guestCount) {
        Map<String, Object> payload = Map.of("guestCount", guestCount);
        sendEvent("TABLE_SESSION_UPDATED", tableId, sessionId, null, payload, tableSessionRoutingKey);
    }

    @Override
    public void publishReservationCreated(TableReservationDTO reservation) {
        sendEvent("TABLE_RESERVATION_CREATED", reservation.getTableId(), null, null, reservation, tableReservationRoutingKey);
    }

    @Override
    public void publishReservationUpdated(TableReservationDTO reservation) {
        sendEvent("TABLE_RESERVATION_UPDATED", reservation.getTableId(), null, reservation.getId(), reservation, tableReservationRoutingKey);
    }

    @Override
    public void publishReservationCancelled(String tableId, String reservationId) {
        sendEvent("TABLE_RESERVATION_CANCELLED", tableId, null, reservationId, null, tableReservationRoutingKey);
    }

    @Override
    public void publishReservationSeated(TableSessionDTO session) {
        sendEvent("TABLE_RESERVATION_SEATED", null, null, session.getReservationId(), session, tableReservationRoutingKey);
    }

// ── Core ──────────────────────────────────────────────────────────────────

    private void sendEvent(String eventType,
                           String tableId,
                           String sessionId,
                           String reservationId,
                           Object payloadObj,
                           String routingKey) {

        TableCommandEvent event = new TableCommandEvent();
        event.setEventType(eventType);
        event.setTableId(tableId);
        event.setSessionId(sessionId);
        event.setReservationId(reservationId);
        event.setOccurredAt(LocalDateTime.now());

        if (payloadObj != null) {
            Map<String, Object> payloadMap = objectMapper.convertValue(
                    payloadObj,
                    new TypeReference<Map<String, Object>>() {}
            );
            event.setPayload(payloadMap);
        }

        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }

}