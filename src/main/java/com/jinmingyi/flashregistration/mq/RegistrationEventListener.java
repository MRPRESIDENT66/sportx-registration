package com.jinmingyi.flashregistration.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinmingyi.flashregistration.entity.FailedMessage;
import com.jinmingyi.flashregistration.entity.Notification;
import com.jinmingyi.flashregistration.event.RegistrationSucceededEvent;
import com.jinmingyi.flashregistration.mapper.FailedMessageMapper;
import com.jinmingyi.flashregistration.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RegistrationEventListener {
    private final StringRedisTemplate redisTemplate;
    private final NotificationMapper notificationMapper;
    private final FailedMessageMapper failedMessageMapper;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitNames.QUEUE)
    public void onRegistrationSucceeded(RegistrationSucceededEvent event) {
        // Deliberate fault injection for the DLQ demonstration; never enable this in a real API.
        if (event.forceConsumerFailure()) {
            throw new IllegalStateException("demo consumer failure requested");
        }
        String idempotencyKey = "idem:registration-event:" + event.eventId();
        Boolean firstDelivery = redisTemplate.opsForValue().setIfAbsent(idempotencyKey, "1", 7, TimeUnit.DAYS);
        if (!Boolean.TRUE.equals(firstDelivery)) {
            return;
        }
        try {
            Notification notification = new Notification();
            notification.setEventId(event.eventId());
            notification.setUserId(event.userId());
            notification.setTitle("Registration confirmed");
            notification.setContent("Your registration for activity " + event.activityId() + " is confirmed.");
            notificationMapper.insert(notification);
        } catch (RuntimeException e) {
            // A transient failure must be retryable; remove the idempotency key before rethrowing.
            redisTemplate.delete(idempotencyKey);
            throw e;
        }
    }

    @RabbitListener(queues = RabbitNames.DLQ)
    public void onDeadLetter(Message message) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        FailedMessage failed = new FailedMessage();
        failed.setQueueName(RabbitNames.DLQ);
        failed.setPayload(payload);
        failed.setFailureReason(String.valueOf(message.getMessageProperties().getHeaders().get("x-death")));
        failed.setStatus("NEW");
        failed.setReplayCount(0);
        try {
            failed.setOriginalEventId(objectMapper.readValue(payload, RegistrationSucceededEvent.class).eventId());
        } catch (Exception ignored) {
            // Preserve an unparsable dead letter for investigation instead of discarding it.
        }
        failedMessageMapper.insert(failed);
    }
}
