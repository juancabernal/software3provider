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
    public Queue purchaseQueue() {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    public Binding purchaseBinding(Queue purchaseQueue, DirectExchange commercialExchange) {
        return BindingBuilder
                .bind(purchaseQueue)
                .to(commercialExchange)
                .with(routingKey);
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

}
