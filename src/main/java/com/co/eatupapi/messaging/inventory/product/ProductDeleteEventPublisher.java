package com.co.eatupapi.messaging.inventory.product;

import com.co.eatupapi.dto.inventory.product.ProductDeleteMessageDTO;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ProductDeleteEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ProductQueueConfig productQueueConfig;

    public ProductDeleteEventPublisher(RabbitTemplate rabbitTemplate,
                                       ProductQueueConfig productQueueConfig) {
        this.rabbitTemplate = rabbitTemplate;
        this.productQueueConfig = productQueueConfig;
    }

    public void publish(UUID productId) {

        String messageId = UUID.randomUUID().toString();

        ProductDeleteMessageDTO payload = new ProductDeleteMessageDTO(productId);

        rabbitTemplate.convertAndSend(
                productQueueConfig.getProductExchangeName(),
                productQueueConfig.getDeleteRoutingKey(),
                payload,
                message -> {

                    message.getMessageProperties()
                            .setContentType(MessageProperties.CONTENT_TYPE_JSON);

                    message.getMessageProperties()
                            .setHeader("messageId", messageId);
                    message.getMessageProperties()
                            .setHeader("eventType", "PRODUCT_DELETE");

                    return message;
                }
        );
    }
}