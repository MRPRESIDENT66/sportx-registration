package com.jinmingyi.flashregistration.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("failed_message")
public class FailedMessage {
    @TableId(type = com.baomidou.mybatisplus.annotation.IdType.AUTO)
    private Long id;
    private Long originalEventId;
    private String queueName;
    private String payload;
    private String failureReason;
    private String status;
    private Integer replayCount;
}
