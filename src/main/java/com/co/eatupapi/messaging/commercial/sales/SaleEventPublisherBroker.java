package com.co.eatupapi.messaging.commercial.sales;

import com.co.eatupapi.dto.commercial.sales.SaleDeleteRequestedMessage;
import com.co.eatupapi.dto.commercial.sales.SalePatchRequestedMessage;
import com.co.eatupapi.dto.commercial.sales.SaleRequestDTO;
import com.co.eatupapi.dto.commercial.sales.SaleUpdateRequestedMessage;
import java.util.UUID;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class SaleEventPublisherBroker implements SaleEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final SaleQueueConfig saleQueueConfig;

    public SaleEventPublisherBroker(RabbitTemplate rabbitTemplate,
                                    SaleQueueConfig saleQueueConfig) {
        this.rabbitTemplate = rabbitTemplate;
        this.saleQueueConfig = saleQueueConfig;
    }

    @Override
    public void publishCreateRequested(SaleRequestDTO request) {
        publish(saleQueueConfig.getCreateRequestExchangeName(), saleQueueConfig.getCreateRequestRoutingKeyName(), request);
    }

    @Override
    public void publishUpdateRequested(SaleUpdateRequestedMessage message) {
        publish(saleQueueConfig.getUpdateRequestExchangeName(), saleQueueConfig.getUpdateRequestRoutingKeyName(), message);
    }

    @Override
    public void publishPatchRequested(SalePatchRequestedMessage message) {
        publish(saleQueueConfig.getPatchRequestExchangeName(), saleQueueConfig.getPatchRequestRoutingKeyName(), message);
    }

    @Override
    public void publishDeleteRequested(SaleDeleteRequestedMessage message) {
        publish(saleQueueConfig.getDeleteRequestExchangeName(), saleQueueConfig.getDeleteRequestRoutingKeyName(), message);
    }

    private void publish(String exchange, String routingKey, Object payload) {
        rabbitTemplate.convertAndSend(exchange, routingKey, payload, message -> {
            message.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
            message.getMessageProperties().setHeader("idMensaje", UUID.randomUUID().toString());
            return message;
        });
    }
}
