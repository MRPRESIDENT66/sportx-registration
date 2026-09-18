# 真实压测记录

时间：2026-09-18

## 压测命令

```bash
wrk -t8 -c100 -d30s -s loadtest/register.lua http://127.0.0.1:8080
```

## 原始输出

```text
Running 30s test @ http://127.0.0.1:8080
  8 threads and 100 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    87.56ms   11.21ms 289.44ms   92.74%
    Req/Sec   137.44     23.05   232.00     83.60%
  32902 requests in 30.09s, 6.24MB read
  Non-2xx or 3xx responses: 32852
Requests/sec:   1093.63
Transfer/sec:    212.54KB
```

`Non-2xx` 是活动售罄或重复请求后的业务拒绝响应。该指标不能当作错误率，更不能把总 `requests/sec` 表述为成功报名 TPS；真正成功报名数由数据库最终状态验证。

## SQL 验证结果

```text
activity:      total_slots = 50, joined_slots = 50
registration:  registrations = 50, unique_users = 50
duplicates:    0 rows
outbox_event:  DELIVERED = 50
notification:  50 rows
```

结论：100 并发连接争抢 50 个名额时，最终报名数不超过名额且没有用户重复报名。
