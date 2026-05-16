package com.co.eatupapi.config.rabbitmq.inventory;

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
public class InventoryRabbitMQConfig {

    @Value("${rabbitmq.exchange.product}")
    private String productExchange;

    @Value("${rabbitmq.queue.product.create}")
    private String productCreateQueue;

    @Value("${rabbitmq.queue.product.update}")
    private String productUpdateQueue;

    @Value("${rabbitmq.queue.product.patch}")
    private String productPatchQueue;

    @Value("${rabbitmq.queue.product.stock}")
    private String productStockQueue;

    @Value("${rabbitmq.queue.product.delete}")
    private String productDeleteQueue;

    @Value("${rabbitmq.routing-key.product.create}")
    private String createRoutingKey;

    @Value("${rabbitmq.routing-key.product.update}")
    private String updateRoutingKey;

    @Value("${rabbitmq.routing-key.product.patch}")
    private String patchRoutingKey;

    @Value("${rabbitmq.routing-key.product.stock}")
    private String stockRoutingKey;

    @Value("${rabbitmq.routing-key.product.delete}")
    private String deleteRoutingKey;



    @Value("${rabbitmq.exchange.location}")
    private String locationExchange;

    @Value("${rabbitmq.queue.location.create}")
    private String locationCreateQueue;

    @Value("${rabbitmq.queue.location.update}")
    private String locationUpdateQueue;

    @Value("${rabbitmq.queue.location.patch}")
    private String locationPatchQueue;

    @Value("${rabbitmq.routing-key.location.create}")
    private String locationCreateRoutingKey;

    @Value("${rabbitmq.routing-key.location.update}")
    private String locationUpdateRoutingKey;

    @Value("${rabbitmq.routing-key.location.patch}")
    private String locationPatchRoutingKey;

    //RECIPE

    @Value("${rabbitmq.exchange.recipe}")
    private String recipeExchange;

    @Value("${rabbitmq.queue.recipe.create}")
    private String recipeCreateQueue;

    @Value("${rabbitmq.queue.recipe.update}")
    private String recipeUpdateQueue;

    @Value("${rabbitmq.queue.recipe.patch}")
    private String recipePatchQueue;

    @Value("${rabbitmq.routing-key.recipe.create}")
    private String recipeCreateRoutingKey;

    @Value("${rabbitmq.routing-key.recipe.update}")
    private String recipeUpdateRoutingKey;

    @Value("${rabbitmq.routing-key.recipe.patch}")
    private String recipePatchRoutingKey;

    @Bean
    public RabbitAdmin rabbitAdminInventory(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.initialize();
        return admin;
    }

    @Bean
    public MessageConverter inventoryJsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate inventoryRabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter inventoryJsonMessageConverter) {

        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(inventoryJsonMessageConverter);
        return template;
    }

    @Bean
    public DirectExchange productExchange() {
        return new DirectExchange(productExchange);
    }

    @Bean
    public Queue productCreateQueue() {
        return QueueBuilder.durable(productCreateQueue).build();
    }

    @Bean
    public Queue productUpdateQueue() {
        return QueueBuilder.durable(productUpdateQueue).build();
    }

    @Bean
    public Queue productPatchQueue() {
        return QueueBuilder.durable(productPatchQueue).build();
    }

    @Bean
    public Queue productStockQueue() {
        return QueueBuilder.durable(productStockQueue).build();
    }

    @Bean
    public Queue productDeleteQueue() {
        return QueueBuilder.durable(productDeleteQueue).build();
    }

    @Bean
    public Binding createBinding(Queue productCreateQueue, DirectExchange productExchange) {
        return BindingBuilder.bind(productCreateQueue)
                .to(productExchange)
                .with(createRoutingKey);
    }

    @Bean
    public Binding updateBinding(Queue productUpdateQueue, DirectExchange productExchange) {
        return BindingBuilder.bind(productUpdateQueue)
                .to(productExchange)
                .with(updateRoutingKey);
    }

    @Bean
    public Binding patchBinding(Queue productPatchQueue, DirectExchange productExchange) {
        return BindingBuilder.bind(productPatchQueue)
                .to(productExchange)
                .with(patchRoutingKey);
    }

    @Bean
    public Binding stockBinding(Queue productStockQueue, DirectExchange productExchange) {
        return BindingBuilder.bind(productStockQueue)
                .to(productExchange)
                .with(stockRoutingKey);
    }

    @Bean
    public Binding deleteBinding(Queue productDeleteQueue, DirectExchange productExchange) {
        return BindingBuilder.bind(productDeleteQueue)
                .to(productExchange)
                .with(deleteRoutingKey);
    }

    @Bean
    public DirectExchange locationExchange() {
        return new DirectExchange(locationExchange);
    }

    @Bean
    public Queue locationCreateQueue() {
        return QueueBuilder.durable(locationCreateQueue).build();
    }

    @Bean
    public Queue locationUpdateQueue() {
        return QueueBuilder.durable(locationUpdateQueue).build();
    }

    @Bean
    public Queue locationPatchQueue() {
        return QueueBuilder.durable(locationPatchQueue).build();
    }

    @Bean
    public Binding locationCreateBinding(Queue locationCreateQueue, DirectExchange locationExchange) {
        return BindingBuilder.bind(locationCreateQueue)
                .to(locationExchange)
                .with(locationCreateRoutingKey);
    }

    @Bean
    public Binding locationUpdateBinding(Queue locationUpdateQueue, DirectExchange locationExchange) {
        return BindingBuilder.bind(locationUpdateQueue)
                .to(locationExchange)
                .with(locationUpdateRoutingKey);
    }

    @Bean
    public Binding locationPatchBinding(Queue locationPatchQueue, DirectExchange locationExchange) {
        return BindingBuilder.bind(locationPatchQueue)
                .to(locationExchange)
                .with(locationPatchRoutingKey);
    }

    // RECIPE BEANS

    @Bean
    public DirectExchange recipeExchange() {
        return new DirectExchange(recipeExchange);
    }

    @Bean
    public Queue recipeCreateQueue() {
        return QueueBuilder.durable(recipeCreateQueue).build();
    }

    @Bean
    public Queue recipeUpdateQueue() {
        return QueueBuilder.durable(recipeUpdateQueue).build();
    }

    @Bean
    public Queue recipePatchQueue() {
        return QueueBuilder.durable(recipePatchQueue).build();
    }

    @Bean
    public Binding recipeCreateBinding(Queue recipeCreateQueue, DirectExchange recipeExchange) {
        return BindingBuilder.bind(recipeCreateQueue)
                .to(recipeExchange)
                .with(recipeCreateRoutingKey);
    }

    @Bean
    public Binding recipeUpdateBinding(Queue recipeUpdateQueue, DirectExchange recipeExchange) {
        return BindingBuilder.bind(recipeUpdateQueue)
                .to(recipeExchange)
                .with(recipeUpdateRoutingKey);
    }

    @Bean
    public Binding recipePatchBinding(Queue recipePatchQueue, DirectExchange recipeExchange) {
        return BindingBuilder.bind(recipePatchQueue)
                .to(recipeExchange)
                .with(recipePatchRoutingKey);
    }
}