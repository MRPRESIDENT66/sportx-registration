package com.jinmingyi.flashregistration.mq;

public final class RabbitNames {
    private RabbitNames() {}
    public static final String EXCHANGE = "registration.events.exchange";
    public static final String ROUTING_KEY = "registration.succeeded";
    public static final String QUEUE = "registration.events.queue";
    public static final String DLX = "registration.events.dlx";
    public static final String DLQ = "registration.events.dlq";
    public static final String DL_ROUTING_KEY = "registration.succeeded.dlq";
}
