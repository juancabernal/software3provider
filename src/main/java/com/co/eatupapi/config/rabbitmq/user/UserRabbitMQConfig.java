package com.co.eatupapi.config.rabbitmq.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserRabbitMQConfig {

	@Value("${rabbitmq.exchange.user}")
	private String exchangeName;

	@Value("${rabbitmq.queue.user}")
	private String queueName;

	@Value("${rabbitmq.routing-key.user}")
	private String routingKey;

	@Bean
	public ObjectMapper userObjectMapper() {
		return JsonMapper.builder().findAndAddModules().build();
	}

	@Bean
	public DirectExchange userExchange() {
		return new DirectExchange(exchangeName);
	}

	@Bean
	public Queue userQueue() {
		return QueueBuilder.durable(queueName).build();
	}

	@Bean
	public Binding userBinding(@Qualifier("userQueue") Queue userQueue,
							   @Qualifier("userExchange") DirectExchange userExchange) {
		return BindingBuilder.bind(userQueue).to(userExchange).with(routingKey);
	}
}