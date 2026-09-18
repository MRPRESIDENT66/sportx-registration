package com.jinmingyi.flashregistration.service;

import com.jinmingyi.flashregistration.entity.FailedMessage;
import java.util.List;

public interface FailedMessageService {
    List<FailedMessage> list();
    void replay(Long id);
}
