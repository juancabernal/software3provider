package com.co.eatupapi.config.rabbitmq.commercial;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommercialRabbitMQConfig {

    @Value("${rabbitmq.exchange.commercial}")
    private String exchangeName;

    @Value("${rabbitmq.queue.purchase}")
    private String queueName;

    @Value("${rabbitmq.routing-key.purchase}")
    private String routingKey;

    @Value("${rabbitmq.exchange.sales-create-request}")
    private String salesCreateRequestExchangeName;

    @Value("${rabbitmq.queue.sales-create-request}")
    private String salesCreateRequestQueueName;

    @Value("${rabbitmq.routing-key.sales-create-request}")
    private String salesCreateRequestRoutingKey;

    @Value("${rabbitmq.exchange.sales-update-request}")
    private String salesUpdateRequestExchangeName;

    @Value("${rabbitmq.queue.sales-update-request}")
    private String salesUpdateRequestQueueName;

    @Value("${rabbitmq.routing-key.sales-update-request}")
    private String salesUpdateRequestRoutingKey;

    @Value("${rabbitmq.exchange.sales-patch-request}")
    private String salesPatchRequestExchangeName;

    @Value("${rabbitmq.queue.sales-patch-request}")
    private String salesPatchRequestQueueName;

    @Value("${rabbitmq.routing-key.sales-patch-request}")
    private String salesPatchRequestRoutingKey;

    @Value("${rabbitmq.exchange.sales-delete-request}")
    private String salesDeleteRequestExchangeName;

    @Value("${rabbitmq.queue.sales-delete-request}")
    private String salesDeleteRequestQueueName;

    @Value("${rabbitmq.routing-key.sales-delete-request}")
    private String salesDeleteRequestRoutingKey;

    @Value("${rabbitmq.queue.table-crud}")
    private String tableCrudQueueName;

    @Value("${rabbitmq.routing-key.table-crud}")
    private String tableCrudRoutingKey;

    @Value("${rabbitmq.queue.table-session}")
    private String tableSessionQueueName;

    @Value("${rabbitmq.routing-key.table-session}")
    private String tableSessionRoutingKey;

    @Value("${rabbitmq.queue.table-reservation}")
    private String tableReservationQueueName;

    @Value("${rabbitmq.routing-key.table-reservation}")
    private String tableReservationRoutingKey;


    @Value("${rabbitmq.queue.discount}")
    private String discountQueueName;

    @Value("${rabbitmq.routing-key.discount}")
    private String discountRoutingKey;

    @Value("${rabbitmq.queue.customer-discount}")
    private String customerDiscountQueueName;

    @Value("${rabbitmq.routing-key.customer-discount}")
    private String customerDiscountRoutingKey;

    @Value("${rabbitmq.queue.seller}")
    private String sellerQueueName;

    @Value("${rabbitmq.routing-key.seller}")
    private String sellerRoutingKey;


    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.initialize();
        return admin;
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    @Bean
    public DirectExchange commercialExchange() {
        return new DirectExchange(exchangeName);
    }

    @Bean
    public DirectExchange salesCreateRequestExchange() {
        return new DirectExchange(salesCreateRequestExchangeName);
    }

    @Bean
    public Queue salesCreateRequestQueue() {
        return QueueBuilder.durable(salesCreateRequestQueueName).build();
    }

    @Bean
    public Binding salesCreateRequestBinding(Queue salesCreateRequestQueue, DirectExchange salesCreateRequestExchange) {
        return BindingBuilder
                .bind(salesCreateRequestQueue)
                .to(salesCreateRequestExchange)
                .with(salesCreateRequestRoutingKey);
    }

    @Bean
    public Queue tableCrudQueue() {
        return QueueBuilder.durable(tableCrudQueueName).build();
    }

    @Bean
    public Binding tableCrudBinding(Queue tableCrudQueue, DirectExchange commercialExchange) {
        return BindingBuilder.bind(tableCrudQueue).to(commercialExchange).with(tableCrudRoutingKey);
    }

    @Bean
    public Queue tableSessionQueue() {
        return QueueBuilder.durable(tableSessionQueueName).build();
    }

    @Bean
    public Binding tableSessionBinding(Queue tableSessionQueue, DirectExchange commercialExchange) {
        return BindingBuilder.bind(tableSessionQueue).to(commercialExchange).with(tableSessionRoutingKey);
    }

    @Bean
    public Queue tableReservationQueue() {
        return QueueBuilder.durable(tableReservationQueueName).build();
    }

    @Bean
    public Binding tableReservationBinding(Queue tableReservationQueue, DirectExchange commercialExchange) {
        return BindingBuilder.bind(tableReservationQueue).to(commercialExchange).with(tableReservationRoutingKey);
    }


    @Bean
    public DirectExchange salesUpdateRequestExchange() {
        return new DirectExchange(salesUpdateRequestExchangeName);
    }

    @Bean
    public Queue salesUpdateRequestQueue() {
        return QueueBuilder.durable(salesUpdateRequestQueueName).build();
    }

    @Bean
    public Binding salesUpdateRequestBinding(Queue salesUpdateRequestQueue, DirectExchange salesUpdateRequestExchange) {
        return BindingBuilder.bind(salesUpdateRequestQueue).to(salesUpdateRequestExchange).with(salesUpdateRequestRoutingKey);
    }

    @Bean
    public DirectExchange salesPatchRequestExchange() {
        return new DirectExchange(salesPatchRequestExchangeName);
    }

    @Bean
    public Queue salesPatchRequestQueue() {
        return QueueBuilder.durable(salesPatchRequestQueueName).build();
    }

    @Bean
    public Binding salesPatchRequestBinding(Queue salesPatchRequestQueue, DirectExchange salesPatchRequestExchange) {
        return BindingBuilder.bind(salesPatchRequestQueue).to(salesPatchRequestExchange).with(salesPatchRequestRoutingKey);
    }

    @Bean
    public DirectExchange salesDeleteRequestExchange() {
        return new DirectExchange(salesDeleteRequestExchangeName);
    }

    @Bean
    public Queue salesDeleteRequestQueue() {
        return QueueBuilder.durable(salesDeleteRequestQueueName).build();
    }

    @Bean
    public Binding salesDeleteRequestBinding(Queue salesDeleteRequestQueue, DirectExchange salesDeleteRequestExchange) {
        return BindingBuilder.bind(salesDeleteRequestQueue).to(salesDeleteRequestExchange).with(salesDeleteRequestRoutingKey);
    }

    @Bean
    public Queue discountQueue() {
        return QueueBuilder.durable(discountQueueName).build();
    }

    @Bean
    public Binding discountBinding(Queue discountQueue, DirectExchange commercialExchange) {
        return BindingBuilder.bind(discountQueue).to(commercialExchange).with(discountRoutingKey);
    }

    @Bean
    public Queue customerDiscountQueue() {
        return QueueBuilder.durable(customerDiscountQueueName).build();
    }

    @Bean
    public Binding customerDiscountBinding(Queue customerDiscountQueue, DirectExchange commercialExchange) {
        return BindingBuilder.bind(customerDiscountQueue).to(commercialExchange).with(customerDiscountRoutingKey);
    }

    @Bean
    public Queue sellerQueue() {
        return QueueBuilder.durable(sellerQueueName).build();
    }

    @Bean
    public Binding sellerBinding(Queue sellerQueue, DirectExchange commercialExchange) {
        return BindingBuilder.bind(sellerQueue).to(commercialExchange).with(sellerRoutingKey);
    }

}
