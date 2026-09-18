package com.jinmingyi.flashregistration.controller;

import com.jinmingyi.flashregistration.common.ApiResponse;
import com.jinmingyi.flashregistration.entity.Activity;
import com.jinmingyi.flashregistration.mapper.ActivityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/activities")
public class ActivityController {
    private final ActivityMapper activityMapper;

    @GetMapping("/{id}")
    public ApiResponse<Activity> detail(@PathVariable Long id) {
        return ApiResponse.ok(activityMapper.selectById(id));
    }
}
