package com.co.eatupapi.messaging.commercial.client;

import com.co.eatupapi.dto.commercial.client.ClientDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class ClientEventPublisherBroker implements ClientEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ClientEventPublisherBroker.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.commercial:commercial.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key.client:client.events}")
    private String routingKey;

    public ClientEventPublisherBroker(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publishClientCreated(ClientDTO client) {
        sendEvent("CLIENT_CREATED", client.getId(), client);
    }

    @Override
    public void publishClientUpdated(String clientId, ClientDTO client) {
        sendEvent("CLIENT_UPDATED", clientId, client);
    }

    @Override
    public void publishClientStatusUpdated(String clientId, Boolean active) {
        sendEvent("CLIENT_STATUS_UPDATED", clientId, Map.of("active", active));
    }

    private void sendEvent(String eventType, String clientId, Object payload) {
        ClientCommandEvent event = new ClientCommandEvent();
        event.setEventType(eventType);
        event.setClientId(clientId);
        event.setOccurredAt(LocalDateTime.now());
        event.setPayload(payload);

        rabbitTemplate.convertAndSend(exchange, routingKey, event);
        log.info("Client event sent to RabbitMQ. eventType={}, clientId={}, exchange={}, routingKey={}",
                eventType, clientId, exchange, routingKey);
    }
}
