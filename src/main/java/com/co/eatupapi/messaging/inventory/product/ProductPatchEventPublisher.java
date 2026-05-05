package com.co.eatupapi.messaging.inventory.product;

import com.co.eatupapi.dto.inventory.product.ProductPatchDTO;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class ProductPatchEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ProductQueueConfig config;

    public ProductPatchEventPublisher(RabbitTemplate rabbitTemplate,
                                      ProductQueueConfig config) {
        this.rabbitTemplate = rabbitTemplate;
        this.config = config;
    }

    public void publish(UUID id, ProductPatchDTO request) {

        String messageId = UUID.randomUUID().toString();

        rabbitTemplate.convertAndSend(
                config.getProductExchangeName(),
                config.getUpdateRoutingKey(),
                Map.of(
                        "id", id,
                        "data", request
                ),
                message -> {

                    message.getMessageProperties()
                            .setContentType(MessageProperties.CONTENT_TYPE_JSON);

                    message.getMessageProperties()
                            .setHeader("messageId", messageId);

                    message.getMessageProperties()
                            .setHeader("eventType", "PRODUCT_PATCH_REQUESTED");

                    return message;
                }
        );
    }
}