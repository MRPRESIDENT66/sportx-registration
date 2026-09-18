package com.jinmingyi.flashregistration.event;

public record RegistrationSucceededEvent(Long eventId, Long activityId, Long userId, boolean forceConsumerFailure) {}
