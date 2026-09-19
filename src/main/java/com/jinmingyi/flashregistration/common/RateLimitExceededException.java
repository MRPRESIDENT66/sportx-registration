package com.jinmingyi.flashregistration.common;

public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException() {
        super("too many requests, please try again later");
    }
}
