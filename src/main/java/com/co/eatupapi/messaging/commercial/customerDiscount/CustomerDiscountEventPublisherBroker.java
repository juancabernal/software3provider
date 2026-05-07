package com.co.eatupapi.messaging.commercial.customerDiscount;

import com.co.eatupapi.dto.commercial.customerDiscount.CustomerDiscountDTO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class CustomerDiscountEventPublisherBroker implements CustomerDiscountEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.commercial}")
    private String exchange;

    @Value("${rabbitmq.routing-key.customer-discount}")
    private String routingKey;

    public CustomerDiscountEventPublisherBroker(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publishCustomerDiscountCreated(CustomerDiscountDTO customerDiscount) {
        sendEvent("CUSTOMER_DISCOUNT_CREATED", null, customerDiscount);
    }

    @Override
    public void publishCustomerDiscountUpdated(CustomerDiscountDTO customerDiscount) {
        sendEvent("CUSTOMER_DISCOUNT_UPDATED", customerDiscount.getId().toString(), customerDiscount);
    }

    @Override
    public void publishCustomerDiscountDeleted(UUID customerDiscountId) {
        sendEvent("CUSTOMER_DISCOUNT_DELETED", customerDiscountId.toString(), null);
    }

    private void sendEvent(String eventType, String customerDiscountId, Object payload) {
        CustomerDiscountCommandEvent event = new CustomerDiscountCommandEvent();
        event.setEventType(eventType);
        event.setCustomerDiscountId(customerDiscountId);
        event.setOccurredAt(LocalDateTime.now());
        if (payload != null) {
            event.setPayload(payload);
        }
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }
}