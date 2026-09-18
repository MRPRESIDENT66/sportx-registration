package com.jinmingyi.flashregistration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinmingyi.flashregistration.common.BusinessException;
import com.jinmingyi.flashregistration.entity.Activity;
import com.jinmingyi.flashregistration.entity.OutboxEvent;
import com.jinmingyi.flashregistration.entity.Registration;
import com.jinmingyi.flashregistration.event.RegistrationSucceededEvent;
import com.jinmingyi.flashregistration.mapper.ActivityMapper;
import com.jinmingyi.flashregistration.mapper.OutboxEventMapper;
import com.jinmingyi.flashregistration.mapper.RegistrationMapper;
import com.jinmingyi.flashregistration.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {
    private final ActivityMapper activityMapper;
    private final RegistrationMapper registrationMapper;
    private final OutboxEventMapper outboxEventMapper;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public RegistrationResult register(Long activityId, Long userId, boolean forceConsumerFailure) {
        if (activityId == null || userId == null || userId <= 0) {
            throw new BusinessException("activityId and a positive X-User-Id are required");
        }
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("activity does not exist");
        }

        // This lock only suppresses simultaneous double-clicks for one user and activity.
        // Final correctness still comes from the atomic SQL update and the unique index.
        RLock lock = redissonClient.getLock("lock:registration:" + activityId + ":" + userId);
        boolean acquired;
        try {
            acquired = lock.tryLock(0, 10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("registration request was interrupted");
        }
        if (!acquired) {
            throw new BusinessException("duplicate request is being processed");
        }

        try {
            long existing = registrationMapper.selectCount(new LambdaQueryWrapper<Registration>()
                    .eq(Registration::getActivityId, activityId)
                    .eq(Registration::getUserId, userId));
            if (existing > 0) {
                throw new BusinessException("this user has already registered");
            }

            // Atomic conditional update: only one of concurrent requests can occupy each remaining slot.
            if (activityMapper.reserveSlot(activityId) != 1) {
                throw new BusinessException("activity is sold out");
            }

            Registration registration = new Registration();
            registration.setActivityId(activityId);
            registration.setUserId(userId);
            registration.setStatus("REGISTERED");
            try {
                registrationMapper.insert(registration);
            } catch (DuplicateKeyException e) {
                // Runtime exception rolls back the previously reserved slot in this transaction.
                throw new BusinessException("this user has already registered");
            }

            // The intent to publish is persisted in the same MySQL transaction as the business writes.
            OutboxEvent outbox = new OutboxEvent();
            outbox.setEventType("REGISTRATION_SUCCEEDED");
            outbox.setAggregateId(activityId);
            outbox.setPayload("{}");
            outbox.setStatus(OutboxEvent.PENDING);
            outbox.setRetryCount(0);
            outbox.setNextRetryAt(LocalDateTime.now());
            outboxEventMapper.insert(outbox);

            RegistrationSucceededEvent event = new RegistrationSucceededEvent(
                    outbox.getId(), activityId, userId, forceConsumerFailure);
            outbox.setPayload(writeJson(event));
            outboxEventMapper.updateById(outbox);
            return new RegistrationResult(registration.getId(), outbox.getId());
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private String writeJson(RegistrationSucceededEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("cannot serialize outbox event", e);
        }
    }

    public record RegistrationResult(Long registrationId, Long outboxEventId) {}
}
