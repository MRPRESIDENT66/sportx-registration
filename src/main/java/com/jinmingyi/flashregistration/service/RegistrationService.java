package com.jinmingyi.flashregistration.service;

import com.jinmingyi.flashregistration.service.impl.RegistrationServiceImpl.RegistrationResult;

public interface RegistrationService {
    RegistrationResult register(Long activityId, Long userId, boolean forceConsumerFailure);
}
