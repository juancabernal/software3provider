package com.co.eatupapi.messaging.user;

import com.co.eatupapi.dto.user.CreateUserRequest;
import com.co.eatupapi.dto.user.UpdateUserRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component; 

import java.util.UUID;

@Component
public class UserCommandPublisher {

	private final RabbitTemplate rabbitTemplate;
	private final ObjectMapper objectMapper;

	@Value("${rabbitmq.exchange.user}")
	private String exchange;

	@Value("${rabbitmq.routing-key.user}")
	private String routingKey;

	public UserCommandPublisher(RabbitTemplate rabbitTemplate, @Qualifier("userObjectMapper") ObjectMapper objectMapper) {
		this.rabbitTemplate = rabbitTemplate;
		this.objectMapper = objectMapper;
	}

	public void publishCreate(CreateUserRequest request) {
		UserCommandMessage message = new UserCommandMessage();
		message.setAction(UserCommandAction.CREATE);
		message.setCreateRequest(request);
		rabbitTemplate.send(exchange, routingKey, toMessage(message));
	}

	public void publishUpdate(UUID userId, UpdateUserRequest request) {
		UserCommandMessage message = new UserCommandMessage();
		message.setAction(UserCommandAction.UPDATE);
		message.setUserId(userId);
		message.setUpdateRequest(request);
		rabbitTemplate.send(exchange, routingKey, toMessage(message));
	}

	public void publishStatusUpdate(UUID userId, String status) {
		UserCommandMessage message = new UserCommandMessage();
		message.setAction(UserCommandAction.UPDATE_STATUS);
		message.setUserId(userId);
		message.setStatus(status);
		rabbitTemplate.send(exchange, routingKey, toMessage(message));
	}

	private Message toMessage(UserCommandMessage message) {
		try {
			byte[] body = objectMapper.writeValueAsBytes(message);
			return MessageBuilder.withBody(body)
					.setContentType(MessageProperties.CONTENT_TYPE_JSON)
					.build();
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Could not serialize user command message", exception);
		}
	}
}
