package com.co.eatupapi.messaging.inventory.categories;

import com.co.eatupapi.dto.inventory.categories.CategoryCreateRequestedMessage;
import com.co.eatupapi.dto.inventory.categories.CategoryDTO;
import com.co.eatupapi.dto.inventory.categories.CategoryUpdateStatusRequestedMessage;
import java.util.UUID;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class CategoryEventPublisherBroker implements CategoryEventPublisher {

    private static final String CREATE_EVENT_TYPE = "CATEGORY_CREATE_REQUESTED";
    private static final String UPDATE_STATUS_EVENT_TYPE = "CATEGORY_UPDATE_STATUS_REQUESTED";

    private final RabbitTemplate rabbitTemplate;
    private final CategoryQueueConfig categoryQueueConfig;

    public CategoryEventPublisherBroker(RabbitTemplate rabbitTemplate,
                                        CategoryQueueConfig categoryQueueConfig) {
        this.rabbitTemplate = rabbitTemplate;
        this.categoryQueueConfig = categoryQueueConfig;
    }

    @Override
    public void publishCreateRequested(CategoryDTO request) {
        publish(categoryQueueConfig.getCategoryExchangeName(),
                categoryQueueConfig.getCreateRoutingKey(),
                new CategoryCreateRequestedMessage(request),
                CREATE_EVENT_TYPE);
    }

    @Override
    public void publishUpdateStatusRequested(CategoryUpdateStatusRequestedMessage message) {
        publish(categoryQueueConfig.getCategoryExchangeName(),
                categoryQueueConfig.getUpdateStatusRoutingKey(),
                message,
                UPDATE_STATUS_EVENT_TYPE);
    }

    private void publish(String exchange, String routingKey, Object payload, String eventType) {
        String messageId = UUID.randomUUID().toString();

        rabbitTemplate.convertAndSend(exchange, routingKey, payload, message -> {
            message.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
            message.getMessageProperties().setHeader("messageId", messageId);
            message.getMessageProperties().setHeader("eventType", eventType);
            return message;
        });
    }
}
