package com.co.eatupapi.messaging.inventory.categories;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CategoryQueueConfig {

    @Value("${rabbitmq.exchange.category}")
    private String categoryExchangeName;

    @Value("${rabbitmq.routing-key.category.create}")
    private String createRoutingKey;

    @Value("${rabbitmq.routing-key.category.update-status}")
    private String updateStatusRoutingKey;

    public String getCategoryExchangeName() {
        return categoryExchangeName;
    }

    public String getCreateRoutingKey() {
        return createRoutingKey;
    }

    public String getUpdateStatusRoutingKey() {
        return updateStatusRoutingKey;
    }
}
