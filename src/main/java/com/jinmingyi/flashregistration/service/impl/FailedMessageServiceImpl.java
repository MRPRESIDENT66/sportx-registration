package com.jinmingyi.flashregistration.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinmingyi.flashregistration.common.BusinessException;
import com.jinmingyi.flashregistration.entity.FailedMessage;
import com.jinmingyi.flashregistration.event.RegistrationSucceededEvent;
import com.jinmingyi.flashregistration.mapper.FailedMessageMapper;
import com.jinmingyi.flashregistration.mq.RabbitPublisher;
import com.jinmingyi.flashregistration.service.FailedMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FailedMessageServiceImpl implements FailedMessageService {
    private final FailedMessageMapper failedMessageMapper;
    private final RabbitPublisher rabbitPublisher;
    private final ObjectMapper objectMapper;

    @Override
    public List<FailedMessage> list() { return failedMessageMapper.selectList(null); }

    @Override
    public void replay(Long id) {
        FailedMessage failed = failedMessageMapper.selectById(id);
        if (failed == null) throw new BusinessException("failed message does not exist");
        try {
            RegistrationSucceededEvent original = objectMapper.readValue(failed.getPayload(), RegistrationSucceededEvent.class);
            // Clear the intentionally injected demo failure before re-publishing.
            rabbitPublisher.publish(new RegistrationSucceededEvent(original.eventId(), original.activityId(), original.userId(), false));
            failedMessageMapper.update(null, new LambdaUpdateWrapper<FailedMessage>()
                    .eq(FailedMessage::getId, id)
                    .set(FailedMessage::getStatus, "REPLAYED")
                    .setSql("replay_count = replay_count + 1"));
        } catch (Exception e) {
            throw new BusinessException("failed message replay was rejected: " + e.getMessage());
        }
    }
}
