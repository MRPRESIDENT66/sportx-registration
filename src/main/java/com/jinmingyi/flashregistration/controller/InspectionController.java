package com.jinmingyi.flashregistration.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jinmingyi.flashregistration.common.ApiResponse;
import com.jinmingyi.flashregistration.entity.Notification;
import com.jinmingyi.flashregistration.entity.OutboxEvent;
import com.jinmingyi.flashregistration.mapper.NotificationMapper;
import com.jinmingyi.flashregistration.mapper.OutboxEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/inspection")
public class InspectionController {
    private final OutboxEventMapper outboxEventMapper;
    private final NotificationMapper notificationMapper;

    @GetMapping("/outbox")
    public ApiResponse<List<OutboxEvent>> outbox() {
        return ApiResponse.ok(outboxEventMapper.selectList(new LambdaQueryWrapper<OutboxEvent>()
                .orderByDesc(OutboxEvent::getId)));
    }

    @GetMapping("/notifications")
    public ApiResponse<List<Notification>> notifications() {
        return ApiResponse.ok(notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
                .orderByDesc(Notification::getId)));
    }
}
