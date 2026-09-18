package com.jinmingyi.flashregistration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jinmingyi.flashregistration.entity.Activity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface ActivityMapper extends BaseMapper<Activity> {
    // One SQL statement: InnoDB checks the slot condition while holding the row lock.
    @Update("UPDATE activity SET joined_slots = joined_slots + 1 "
            + "WHERE id = #{activityId} AND joined_slots < total_slots")
    int reserveSlot(@Param("activityId") Long activityId);
}
