package com.jinmingyi.flashregistration.aspect;

import com.jinmingyi.flashregistration.annotation.RateLimit;
import com.jinmingyi.flashregistration.common.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;

@Aspect
@Component
@Order(0)
@RequiredArgsConstructor
public class RateLimitAspect {
    private static final DefaultRedisScript<Long> SLIDING_WINDOW_SCRIPT = new DefaultRedisScript<>("""
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local maxRequests = tonumber(ARGV[3])
            local requestId = ARGV[4]
            redis.call('ZREMRANGEBYSCORE', key, 0, now - window)
            if redis.call('ZCARD', key) >= maxRequests then
                return 0
            end
            redis.call('ZADD', key, now, requestId)
            redis.call('PEXPIRE', key, window + 1000)
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    @Around("@annotation(rateLimit)")
    public Object enforce(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String scope = signature.getDeclaringType().getSimpleName() + ":" + signature.getMethod().getName();
        String subject = rateLimit.perUser()
                ? request.getHeader("X-User-Id")
                : "global";
        if (subject == null || subject.isBlank()) {
            subject = request.getRemoteAddr();
        }

        String key = "rate_limit:" + scope + ":" + subject;
        long windowMillis = rateLimit.windowSeconds() * 1000L;
        Long allowed = redisTemplate.execute(SLIDING_WINDOW_SCRIPT, List.of(key),
                Long.toString(System.currentTimeMillis()),
                Long.toString(windowMillis),
                Integer.toString(rateLimit.limit()),
                UUID.randomUUID().toString());
        if (!Long.valueOf(1L).equals(allowed)) {
            throw new RateLimitExceededException();
        }
        return joinPoint.proceed();
    }
}
