package com.co.eatupapi.messaging.commercial.seller;

import com.co.eatupapi.dto.commercial.seller.SellerDTO;
import com.co.eatupapi.dto.commercial.seller.SellerPatchDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class SellerEventPublisherBroker implements SellerEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SellerEventPublisherBroker.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.commercial}")
    private String exchange;

    @Value("${rabbitmq.routing-key.seller}")
    private String routingKey;

    public SellerEventPublisherBroker(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publishSellerCreated(SellerDTO seller) {
        sendEvent("SELLER_CREATED", null, seller);
    }

    @Override
    public void publishSellerUpdated(String sellerId, SellerDTO seller) {
        sendEvent("SELLER_UPDATED", sellerId, seller);
    }

    @Override
    public void publishSellerStatusUpdated(String sellerId, String status) {
        sendEvent("SELLER_STATUS_UPDATED", sellerId, Map.of("status", status));
    }

    @Override
    public void publishSellerPatched(String sellerId, SellerPatchDTO sellerPatch) {
        sendEvent("SELLER_PATCHED", sellerId, sellerPatch);
    }

    private void sendEvent(String eventType, String sellerId, Object payloadObj) {
        SellerCommandEvent event = new SellerCommandEvent();
        event.setEventType(eventType);
        event.setSellerId(sellerId);
        event.setOccurredAt(LocalDateTime.now());

        if (payloadObj != null) {
            event.setPayload(payloadObj);
        }

        rabbitTemplate.convertAndSend(exchange, routingKey, event);
        log.info("Seller event sent to RabbitMQ. eventType={}, sellerId={}, exchange={}, routingKey={}",
                event.getEventType(),
                event.getSellerId(),
                exchange,
                routingKey);
    }
}
