package com.co.eatupapi.messaging.inventory.recipe;

import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class RecipePatchEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RecipeQueueConfig config;

    public RecipePatchEventPublisher(RabbitTemplate rabbitTemplate, RecipeQueueConfig config) {
        this.rabbitTemplate = rabbitTemplate;
        this.config = config;
    }

    public void publish(String name) {
        String messageId = UUID.randomUUID().toString();

        rabbitTemplate.convertAndSend(
                config.getRecipeExchangeName(),
                config.getPatchRoutingKey(),
                Map.of("name", name),
                message -> {
                    message.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
                    message.getMessageProperties().setHeader("messageId", messageId);
                    message.getMessageProperties().setHeader("eventType", "RECIPE_INACTIVATED");
                    return message;
                }
        );
    }
}
