package com.jinmingyi.flashregistration.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("activity")
public class Activity {
    @TableId
    private Long id;
    private String title;
    private Integer totalSlots;
    private Integer joinedSlots;
}
