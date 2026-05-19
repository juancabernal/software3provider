package com.co.eatupapi.messaging.payment.invoice;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InvoiceMessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.payment}")
    private String exchange;

    @Value("${rabbitmq.routing-key.payment.invoice.create}")
    private String createRoutingKey;

    @Value("${rabbitmq.routing-key.payment.invoice.cancel}")
    private String cancelRoutingKey;

    @Value("${rabbitmq.routing-key.payment.invoice.mark-paid}")
    private String markPaidRoutingKey;

    @Value("${rabbitmq.routing-key.payment.invoice.status-update}")
    private String statusUpdateRoutingKey;

    public InvoiceMessagePublisher(@Qualifier("paymentRabbitTemplate") RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishCreate(InvoiceCreateMessage message) {
        rabbitTemplate.convertAndSend(exchange, createRoutingKey, message);
    }

    public void publishCancel(InvoiceCancelMessage message) {
        rabbitTemplate.convertAndSend(exchange, cancelRoutingKey, message);
    }

    public void publishMarkPaid(InvoiceMarkPaidMessage message) {
        rabbitTemplate.convertAndSend(exchange, markPaidRoutingKey, message);
    }

    public void publishStatusUpdate(InvoiceStatusUpdateMessage message) {
        rabbitTemplate.convertAndSend(exchange, statusUpdateRoutingKey, message);
    }
}
