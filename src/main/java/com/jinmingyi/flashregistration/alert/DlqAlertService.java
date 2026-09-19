package com.jinmingyi.flashregistration.alert;

import com.jinmingyi.flashregistration.entity.FailedMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Reports a persisted dead-letter event to an optional external webhook.
 * Alert delivery must never rethrow: the DLQ record is already durable and a
 * failing alert endpoint must not create another retry loop in the consumer.
 */
@Slf4j
@Service
public class DlqAlertService {
    private final RestClient restClient = RestClient.create();

    @Value("${alert.webhook-url:}")
    private String webhookUrl;

    public void notifyDeadLetter(FailedMessage failed) {
        log.error("DLQ message persisted: failedMessageId={}, eventId={}, queue={}",
                failed.getId(), failed.getOriginalEventId(), failed.getQueueName());
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }
        try {
            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new DlqAlert(
                            "DLQ message detected",
                            failed.getId(),
                            failed.getOriginalEventId(),
                            failed.getQueueName(),
                            failed.getFailureReason()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("DLQ webhook alert failed for failedMessageId={}", failed.getId(), e);
        }
    }

    private record DlqAlert(String title, Long failedMessageId, Long eventId,
                            String queue, String reason) {
    }
}
