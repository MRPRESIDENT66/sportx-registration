# 死信队列与手动回放验证

时间：2026-09-18

## 受控失败

向报名接口发送请求头：

```text
X-User-Id: 900001
X-Demo-Fail-Consumer: true
```

报名事务成功，Outbox Relay 成功投递事件 `54`。消费者因演示开关抛出异常，Spring AMQP 按 `1s, 2s, 4s` 自动重试；重试耗尽后，RabbitMQ 将消息路由到 `registration.events.dlq`。

## DLQ 落库结果

```text
failed_message.id          = 1
original_event_id          = 54
status                     = NEW
replay_count               = 0
notification rows          = 0
```

死信负载：

```json
{
  "eventId": 54,
  "activityId": 10001,
  "userId": 900001,
  "forceConsumerFailure": true
}
```

## 手动回放结果

调用：

```text
POST /mq/failed/1/replay
```

回放会移除演示专用的失败标记后重新发布事件。结果：

```text
failed_message.status      = REPLAYED
failed_message.replay_count = 1
notification.event_id      = 54
notification.user_id       = 900001
```

结论：消费端连续失败不会无限重试或静默丢失；消息可进入 DLQ、持久化留痕，并在根因修复后由人工回放。
