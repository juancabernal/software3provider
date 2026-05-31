package com.co.eatupapi.config.rabbitmq.commercial.client;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClientRabbitMQConfig {

    @Value("${rabbitmq.queue.client:client.queue}")
    private String clientQueueName;

    @Value("${rabbitmq.routing-key.client:client.events}")
    private String clientRoutingKey;

    @Bean
    public Queue clientQueue() {
        return QueueBuilder.durable(clientQueueName).build();
    }

    @Bean
    public Binding clientBinding(Queue clientQueue,
                                 @Qualifier("commercialExchange") DirectExchange commercialExchange) {
        return BindingBuilder.bind(clientQueue).to(commercialExchange).with(clientRoutingKey);
    }
}
