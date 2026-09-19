package com.jinmingyi.flashregistration.controller;

import com.jinmingyi.flashregistration.annotation.RateLimit;
import com.jinmingyi.flashregistration.common.ApiResponse;
import com.jinmingyi.flashregistration.service.RegistrationService;
import com.jinmingyi.flashregistration.service.impl.RegistrationServiceImpl.RegistrationResult;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/activities")
public class RegistrationController {
    private final RegistrationService registrationService;

    @RateLimit(limit = 5, windowSeconds = 1)
    @PostMapping("/{activityId}/registrations")
    public ApiResponse<RegistrationResult> register(
            @PathVariable @Positive Long activityId,
            @RequestHeader("X-User-Id") @Positive Long userId,
            @RequestHeader(value = "X-Demo-Fail-Consumer", defaultValue = "false") boolean forceConsumerFailure) {
        return ApiResponse.ok(registrationService.register(activityId, userId, forceConsumerFailure));
    }
}
