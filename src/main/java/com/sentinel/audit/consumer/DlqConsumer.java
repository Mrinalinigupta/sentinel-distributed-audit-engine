package com.sentinel.audit.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Dead Letter Queue (DLQ) Consumer.
 * 
 * Captures poisoned or unprocessable messages that exhausted all 3 retry attempts,
 * alerting operations teams without blocking healthy consumer traffic.
 */
@Component
@Slf4j
public class DlqConsumer {

    @KafkaListener(
            topics = "${sentinel.kafka.topics.audit-events-dlq:sentinel.audit.events.v1.DLQ}",
            groupId = "sentinel-audit-dlq-group"
    )
    public void handleDeadLetterMessage(
            @Payload Object failedPayload,
            @Header(value = KafkaHeaders.RECEIVED_PARTITION, required = false) Integer partition,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key
    ) {
        log.error("🚨 [DLQ ALERT] Poisoned audit message intercepted in DLQ topic! Key: [{}], Partition: [{}], Offset: [{}], Payload: [{}]",
                key, partition, offset, failedPayload);
    }
}
