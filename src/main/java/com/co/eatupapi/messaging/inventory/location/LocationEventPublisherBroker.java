package com.co.eatupapi.messaging.inventory.location;

import com.co.eatupapi.dto.inventory.location.LocationPatchRequestedMessage;
import com.co.eatupapi.dto.inventory.location.LocationRequestDTO;
import com.co.eatupapi.dto.inventory.location.LocationUpdateRequestedMessage;
import java.util.UUID;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class LocationEventPublisherBroker implements LocationEventPublisher {

    private static final String CREATE_EVENT_TYPE = "LOCATION_CREATE_REQUESTED";
    private static final String UPDATE_EVENT_TYPE = "LOCATION_UPDATE_REQUESTED";
    private static final String PATCH_EVENT_TYPE = "LOCATION_PATCH_REQUESTED";

    private final RabbitTemplate rabbitTemplate;
    private final LocationQueueConfig locationQueueConfig;

    public LocationEventPublisherBroker(RabbitTemplate rabbitTemplate,
                                        LocationQueueConfig locationQueueConfig) {
        this.rabbitTemplate = rabbitTemplate;
        this.locationQueueConfig = locationQueueConfig;
    }

    @Override
    public void publishCreateRequested(LocationRequestDTO request) {
        publish(locationQueueConfig.getLocationExchangeName(),
                locationQueueConfig.getCreateRoutingKey(),
                request,
                CREATE_EVENT_TYPE);
    }

    @Override
    public void publishUpdateRequested(LocationUpdateRequestedMessage message) {
        publish(locationQueueConfig.getLocationExchangeName(),
                locationQueueConfig.getUpdateRoutingKey(),
                message,
                UPDATE_EVENT_TYPE);
    }

    @Override
    public void publishPatchRequested(LocationPatchRequestedMessage message) {
        publish(locationQueueConfig.getLocationExchangeName(),
                locationQueueConfig.getPatchRoutingKey(),
                message,
                PATCH_EVENT_TYPE);
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
