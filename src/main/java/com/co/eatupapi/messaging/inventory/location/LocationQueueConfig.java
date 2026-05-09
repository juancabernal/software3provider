package com.co.eatupapi.messaging.inventory.location;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocationQueueConfig {

    @Value("${rabbitmq.exchange.location}")
    private String locationExchangeName;

    @Value("${rabbitmq.queue.location.create}")
    private String createQueueName;

    @Value("${rabbitmq.routing-key.location.create}")
    private String createRoutingKey;

    @Value("${rabbitmq.queue.location.update}")
    private String updateQueueName;

    @Value("${rabbitmq.routing-key.location.update}")
    private String updateRoutingKey;

    @Value("${rabbitmq.queue.location.patch}")
    private String patchQueueName;

    @Value("${rabbitmq.routing-key.location.patch}")
    private String patchRoutingKey;

    public String getLocationExchangeName() {
        return locationExchangeName;
    }

    public String getCreateQueueName() {
        return createQueueName;
    }

    public String getCreateRoutingKey() {
        return createRoutingKey;
    }

    public String getUpdateQueueName() {
        return updateQueueName;
    }

    public String getUpdateRoutingKey() {
        return updateRoutingKey;
    }

    public String getPatchQueueName() {
        return patchQueueName;
    }

    public String getPatchRoutingKey() {
        return patchRoutingKey;
    }
}
