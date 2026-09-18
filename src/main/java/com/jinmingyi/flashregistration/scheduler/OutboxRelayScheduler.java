package com.jinmingyi.flashregistration.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinmingyi.flashregistration.entity.OutboxEvent;
import com.jinmingyi.flashregistration.event.RegistrationSucceededEvent;
import com.jinmingyi.flashregistration.mapper.OutboxEventMapper;
import com.jinmingyi.flashregistration.mq.RabbitPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelayScheduler {
    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRIES = 5;
    private final OutboxEventMapper outboxEventMapper;
    private final RabbitPublisher rabbitPublisher;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 3000)
    public void relayPendingEvents() {
        RLock lock = redissonClient.getLock("lock:outbox-relay");
        boolean acquired = lock.tryLock();
        if (!acquired) {
            return;
        }
        try {
            List<OutboxEvent> records = outboxEventMapper.selectList(new LambdaQueryWrapper<OutboxEvent>()
                    .eq(OutboxEvent::getStatus, OutboxEvent.PENDING)
                    .le(OutboxEvent::getNextRetryAt, LocalDateTime.now())
                    .orderByAsc(OutboxEvent::getCreatedAt)
                    .last("LIMIT " + BATCH_SIZE));
            records.forEach(this::deliver);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void deliver(OutboxEvent record) {
        try {
            RegistrationSucceededEvent event = objectMapper.readValue(record.getPayload(), RegistrationSucceededEvent.class);
            rabbitPublisher.publish(event);
            outboxEventMapper.update(null, new LambdaUpdateWrapper<OutboxEvent>()
                    .eq(OutboxEvent::getId, record.getId())
                    .eq(OutboxEvent::getStatus, OutboxEvent.PENDING)
                    .set(OutboxEvent::getStatus, OutboxEvent.DELIVERED)
                    .set(OutboxEvent::getDeliveredAt, LocalDateTime.now()));
        } catch (Exception e) {
            scheduleRetry(record, e);
        }
    }

    private void scheduleRetry(OutboxEvent record, Exception error) {
        int nextAttempt = record.getRetryCount() + 1;
        boolean exhausted = nextAttempt >= MAX_RETRIES;
        long delaySeconds = Math.min(30, 1L << Math.min(nextAttempt - 1, 5));
        outboxEventMapper.update(null, new LambdaUpdateWrapper<OutboxEvent>()
                .eq(OutboxEvent::getId, record.getId())
                .eq(OutboxEvent::getStatus, OutboxEvent.PENDING)
                .set(OutboxEvent::getRetryCount, nextAttempt)
                .set(OutboxEvent::getNextRetryAt, LocalDateTime.now().plusSeconds(delaySeconds))
                .set(exhausted, OutboxEvent::getStatus, OutboxEvent.FAILED));
        log.warn("Outbox event {} delivery failed, attempt {}/{}: {}",
                record.getId(), nextAttempt, MAX_RETRIES, error.getMessage());
    }
}
