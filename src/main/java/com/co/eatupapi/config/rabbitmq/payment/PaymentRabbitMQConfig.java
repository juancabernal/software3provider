package com.co.eatupapi.config.rabbitmq.payment;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentRabbitMQConfig {

    @Value("${rabbitmq.exchange.payment}")
    private String exchangeName;

    @Value("${rabbitmq.queue.payment.cashreceipt.create}")
    private String createQueueName;

    @Value("${rabbitmq.queue.payment.cashreceipt.cancel}")
    private String cancelQueueName;

    @Value("${rabbitmq.routing-key.payment.cashreceipt.create}")
    private String createRoutingKey;

    @Value("${rabbitmq.routing-key.payment.cashreceipt.cancel}")
    private String cancelRoutingKey;

    @Value("${rabbitmq.queue.payment.invoice.create}")
    private String invoiceCreateQueueName;

    @Value("${rabbitmq.queue.payment.invoice.cancel}")
    private String invoiceCancelQueueName;

    @Value("${rabbitmq.queue.payment.invoice.mark-paid}")
    private String invoiceMarkPaidQueueName;

    @Value("${rabbitmq.queue.payment.invoice.status-update}")
    private String invoiceStatusUpdateQueueName;

    @Value("${rabbitmq.routing-key.payment.invoice.create}")
    private String invoiceCreateRoutingKey;

    @Value("${rabbitmq.routing-key.payment.invoice.cancel}")
    private String invoiceCancelRoutingKey;

    @Value("${rabbitmq.routing-key.payment.invoice.mark-paid}")
    private String invoiceMarkPaidRoutingKey;

    @Value("${rabbitmq.routing-key.payment.invoice.status-update}")
    private String invoiceStatusUpdateRoutingKey;

    @Bean
    public RabbitAdmin rabbitAdminPayment(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.initialize();
        return admin;
    }

    @Bean
    public MessageConverter paymentJsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate paymentRabbitTemplate(ConnectionFactory connectionFactory,
                                                MessageConverter paymentJsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(paymentJsonMessageConverter);
        return template;
    }

    @Bean
    public DirectExchange paymentExchange() {
        return new DirectExchange(exchangeName);
    }

    @Bean
    public Queue paymentCashReceiptCreateQueue() {
        return QueueBuilder.durable(createQueueName).build();
    }

    @Bean
    public Queue paymentCashReceiptCancelQueue() {
        return QueueBuilder.durable(cancelQueueName).build();
    }

    @Bean
    public Queue paymentInvoiceCreateQueue() {
        return QueueBuilder.durable(invoiceCreateQueueName).build();
    }

    @Bean
    public Queue paymentInvoiceCancelQueue() {
        return QueueBuilder.durable(invoiceCancelQueueName).build();
    }

    @Bean
    public Queue paymentInvoiceMarkPaidQueue() {
        return QueueBuilder.durable(invoiceMarkPaidQueueName).build();
    }

    @Bean
    public Queue paymentInvoiceStatusUpdateQueue() {
        return QueueBuilder.durable(invoiceStatusUpdateQueueName).build();
    }

    @Bean
    public Binding paymentCashReceiptCreateBinding(Queue paymentCashReceiptCreateQueue, DirectExchange paymentExchange) {
        return BindingBuilder
                .bind(paymentCashReceiptCreateQueue)
                .to(paymentExchange)
                .with(createRoutingKey);
    }

    @Bean
    public Binding paymentCashReceiptCancelBinding(Queue paymentCashReceiptCancelQueue, DirectExchange paymentExchange) {
        return BindingBuilder
                .bind(paymentCashReceiptCancelQueue)
                .to(paymentExchange)
                .with(cancelRoutingKey);
    }

    @Bean
    public Binding paymentInvoiceCreateBinding(
            @Qualifier("paymentInvoiceCreateQueue") Queue paymentInvoiceCreateQueue,
            @Qualifier("paymentExchange") DirectExchange paymentExchange) {
        return BindingBuilder
                .bind(paymentInvoiceCreateQueue)
                .to(paymentExchange)
                .with(invoiceCreateRoutingKey);
    }

    @Bean
    public Binding paymentInvoiceCancelBinding(
            @Qualifier("paymentInvoiceCancelQueue") Queue paymentInvoiceCancelQueue,
            @Qualifier("paymentExchange") DirectExchange paymentExchange) {
        return BindingBuilder
                .bind(paymentInvoiceCancelQueue)
                .to(paymentExchange)
                .with(invoiceCancelRoutingKey);
    }

    @Bean
    public Binding paymentInvoiceMarkPaidBinding(
            @Qualifier("paymentInvoiceMarkPaidQueue") Queue paymentInvoiceMarkPaidQueue,
            @Qualifier("paymentExchange") DirectExchange paymentExchange) {
        return BindingBuilder
                .bind(paymentInvoiceMarkPaidQueue)
                .to(paymentExchange)
                .with(invoiceMarkPaidRoutingKey);
    }

    @Bean
    public Binding paymentInvoiceStatusUpdateBinding(
            @Qualifier("paymentInvoiceStatusUpdateQueue") Queue paymentInvoiceStatusUpdateQueue,
            @Qualifier("paymentExchange") DirectExchange paymentExchange) {
        return BindingBuilder
                .bind(paymentInvoiceStatusUpdateQueue)
                .to(paymentExchange)
                .with(invoiceStatusUpdateRoutingKey);
    }
}
