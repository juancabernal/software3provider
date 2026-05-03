package com.co.eatupapi.messaging.commercial.sales;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SaleQueueConfig {

    @Value("${rabbitmq.exchange.sales-create-request}")
    private String createRequestExchangeName;

    @Value("${rabbitmq.queue.sales-create-request}")
    private String createRequestQueueName;

    @Value("${rabbitmq.routing-key.sales-create-request}")
    private String createRequestRoutingKeyName;

    public String getCreateRequestExchangeName() {
        return createRequestExchangeName;
    }

    public String getCreateRequestQueueName() {
        return createRequestQueueName;
    }

    public String getCreateRequestRoutingKeyName() {
        return createRequestRoutingKeyName;
    }
}
