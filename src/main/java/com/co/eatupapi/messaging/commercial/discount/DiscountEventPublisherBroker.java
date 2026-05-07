package com.co.eatupapi.messaging.commercial.discount;

import com.co.eatupapi.dto.commercial.discount.DiscountDTO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class DiscountEventPublisherBroker implements DiscountEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.commercial}")
    private String exchange;

    @Value("${rabbitmq.routing-key.discount}")
    private String routingKey;

    public DiscountEventPublisherBroker(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publishDiscountCreated(DiscountDTO discount) {
        sendEvent("DISCOUNT_CREATED", null, discount); // id es null en create, está bien
    }

    @Override
    public void publishDiscountUpdated(DiscountDTO discount) {
        sendEvent("DISCOUNT_UPDATED", discount.getId().toString(), discount);
    }

    @Override
    public void publishDiscountStatusUpdated(DiscountDTO discount) {
        sendEvent("DISCOUNT_STATUS_UPDATED", discount.getId().toString(), discount);
    }

    @Override
    public void publishDiscountDeleted(UUID discountId) {
        sendEvent("DISCOUNT_DELETED", discountId.toString(), null);
    }

    private void sendEvent(String eventType, String discountId, Object payload) {
        DiscountCommandEvent event = new DiscountCommandEvent();
        event.setEventType(eventType);
        event.setDiscountId(discountId);
        event.setOccurredAt(LocalDateTime.now());
        if (payload != null) {
            event.setPayload(payload);
        }
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }
}