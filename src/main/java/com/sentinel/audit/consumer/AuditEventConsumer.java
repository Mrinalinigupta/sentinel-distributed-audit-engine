package com.sentinel.audit.consumer;

import com.sentinel.audit.dto.request.AuditEventRequest;
import com.sentinel.audit.service.AuditIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Resilient Kafka Consumer for the primary audit event stream.
 * 
 * Configured with DefaultErrorHandler:
 * - Automatically retries up to 3 times with exponential backoff on failure.
 * - Diverts permanently failed records to the Dead Letter Queue (.DLQ) without halting the consumer.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventConsumer {

    private final AuditIngestionService auditIngestionService;

    @KafkaListener(
            topics = "${sentinel.kafka.topics.audit-events:sentinel.audit.events.v1}",
            groupId = "${spring.kafka.consumer.group-id:sentinel-audit-consumer-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeAuditEvent(
            @Payload AuditEventRequest eventRequest,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_KEY) String key
    ) {
        log.info("Received event for entity [{}] from Kafka partition [{}] at offset [{}]",
                key, partition, offset);

        auditIngestionService.ingestAndSeal(eventRequest);
    }
}
