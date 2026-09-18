package com.jinmingyi.flashregistration.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Bean
    MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    DirectExchange registrationExchange() { return new DirectExchange(RabbitNames.EXCHANGE, true, false); }
    @Bean
    DirectExchange registrationDlx() { return new DirectExchange(RabbitNames.DLX, true, false); }
    @Bean
    Queue registrationQueue() {
        return QueueBuilder.durable(RabbitNames.QUEUE)
                .deadLetterExchange(RabbitNames.DLX)
                .deadLetterRoutingKey(RabbitNames.DL_ROUTING_KEY)
                .build();
    }
    @Bean
    Queue registrationDlq() { return QueueBuilder.durable(RabbitNames.DLQ).build(); }
    @Bean
    Binding registrationBinding() {
        return BindingBuilder.bind(registrationQueue()).to(registrationExchange()).with(RabbitNames.ROUTING_KEY);
    }
    @Bean
    Binding registrationDlxBinding() {
        return BindingBuilder.bind(registrationDlq()).to(registrationDlx()).with(RabbitNames.DL_ROUTING_KEY);
    }
    @Bean
    RabbitTemplateCustomizer rabbitTemplateCustomizer() {
        // Mandatory publishing reports unroutable messages instead of silently dropping them.
        return template -> template.setReturnsCallback(returned ->
                throwAsRuntime("message was returned: " + returned.getReplyText()));
    }

    private static void throwAsRuntime(String message) {
        throw new IllegalStateException(message);
    }
}
