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

    @Value("${rabbitmq.exchange.sales-update-request}")
    private String updateRequestExchangeName;

    @Value("${rabbitmq.queue.sales-update-request}")
    private String updateRequestQueueName;

    @Value("${rabbitmq.routing-key.sales-update-request}")
    private String updateRequestRoutingKeyName;

    @Value("${rabbitmq.exchange.sales-patch-request}")
    private String patchRequestExchangeName;

    @Value("${rabbitmq.queue.sales-patch-request}")
    private String patchRequestQueueName;

    @Value("${rabbitmq.routing-key.sales-patch-request}")
    private String patchRequestRoutingKeyName;

    @Value("${rabbitmq.exchange.sales-delete-request}")
    private String deleteRequestExchangeName;

    @Value("${rabbitmq.queue.sales-delete-request}")
    private String deleteRequestQueueName;

    @Value("${rabbitmq.routing-key.sales-delete-request}")
    private String deleteRequestRoutingKeyName;

    public String getCreateRequestExchangeName() { return createRequestExchangeName; }
    public String getCreateRequestQueueName() { return createRequestQueueName; }
    public String getCreateRequestRoutingKeyName() { return createRequestRoutingKeyName; }
    public String getUpdateRequestExchangeName() { return updateRequestExchangeName; }
    public String getUpdateRequestQueueName() { return updateRequestQueueName; }
    public String getUpdateRequestRoutingKeyName() { return updateRequestRoutingKeyName; }
    public String getPatchRequestExchangeName() { return patchRequestExchangeName; }
    public String getPatchRequestQueueName() { return patchRequestQueueName; }
    public String getPatchRequestRoutingKeyName() { return patchRequestRoutingKeyName; }
    public String getDeleteRequestExchangeName() { return deleteRequestExchangeName; }
    public String getDeleteRequestQueueName() { return deleteRequestQueueName; }
    public String getDeleteRequestRoutingKeyName() { return deleteRequestRoutingKeyName; }
}
