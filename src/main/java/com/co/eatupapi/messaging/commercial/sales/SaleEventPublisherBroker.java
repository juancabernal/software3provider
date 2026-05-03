package com.co.eatupapi.messaging.commercial.sales;

import com.co.eatupapi.dto.commercial.sales.SaleRequestDTO;
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
        String idMessage = UUID.randomUUID().toString();

        rabbitTemplate.convertAndSend(
                saleQueueConfig.getCreateRequestExchangeName(),
                saleQueueConfig.getCreateRequestRoutingKeyName(),
                request,
                message -> {
                    message.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
                    message.getMessageProperties().setHeader("idMensaje", idMessage);
                    return message;
                }
        );
    }
}
