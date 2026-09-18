package com.jinmingyi.flashregistration.controller;

import com.jinmingyi.flashregistration.common.ApiResponse;
import com.jinmingyi.flashregistration.entity.FailedMessage;
import com.jinmingyi.flashregistration.service.FailedMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mq/failed")
public class FailedMessageController {
    private final FailedMessageService failedMessageService;

    @GetMapping
    public ApiResponse<List<FailedMessage>> list() { return ApiResponse.ok(failedMessageService.list()); }

    @PostMapping("/{id}/replay")
    public ApiResponse<Void> replay(@PathVariable Long id) {
        failedMessageService.replay(id);
        return ApiResponse.ok();
    }
}
