package com.co.eatupapi.messaging.inventory.recipe;

import com.co.eatupapi.dto.inventory.recipe.RecipeRequest;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RecipeCreateEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RecipeQueueConfig config;

    public RecipeCreateEventPublisher(RabbitTemplate rabbitTemplate, RecipeQueueConfig config) {
        this.rabbitTemplate = rabbitTemplate;
        this.config = config;
    }

    public void publish(RecipeRequest request) {
        String messageId = UUID.randomUUID().toString();

        rabbitTemplate.convertAndSend(
                config.getRecipeExchangeName(),
                config.getCreateRoutingKey(),
                request,
                message -> {
                    message.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
                    message.getMessageProperties().setHeader("messageId", messageId);
                    message.getMessageProperties().setHeader("eventType", "RECIPE_CREATED");
                    return message;
                }
        );
    }
}
