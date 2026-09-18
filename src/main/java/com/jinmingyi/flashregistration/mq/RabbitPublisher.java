package com.jinmingyi.flashregistration.mq;

import com.jinmingyi.flashregistration.event.RegistrationSucceededEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RabbitPublisher {
    private final RabbitTemplate rabbitTemplate;

    public void publish(RegistrationSucceededEvent event) {
        CorrelationData correlation = new CorrelationData("outbox-" + event.eventId());
        rabbitTemplate.convertAndSend(RabbitNames.EXCHANGE, RabbitNames.ROUTING_KEY, event, correlation);
        try {
            CorrelationData.Confirm confirm = correlation.getFuture().get(5, TimeUnit.SECONDS);
            if (!confirm.isAck()) {
                throw new IllegalStateException("broker rejected event: " + confirm.getReason());
            }
        } catch (Exception e) {
            throw new IllegalStateException("publisher confirm was not received", e);
        }
    }
}
