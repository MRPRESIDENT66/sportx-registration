package com.jinmingyi.flashregistration.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares a distributed sliding-window request limit on a controller method. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int limit();
    long windowSeconds();
    boolean perUser() default true;
}
