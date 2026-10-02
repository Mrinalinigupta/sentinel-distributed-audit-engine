package com.sentinel.audit.service;

import com.sentinel.audit.dto.request.AuditEventRequest;
import com.sentinel.audit.dto.response.AuditEventResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Service responsible for publishing audit events into Kafka.
 * 
 * CRITICAL ARCHITECTURAL GUARANTEE:
 * Uses `entityId` as the Kafka Record Key!
 * In Apache Kafka, all messages with the exact same key are deterministically routed to the
 * same partition, guaranteeing chronological sequence order per entity.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${sentinel.kafka.topics.audit-events:sentinel.audit.events.v1}")
    private String auditTopic;

    public AuditEventResponse publishEvent(AuditEventRequest request) {
        String eventId = "ev-" + UUID.randomUUID().toString().substring(0, 8);
        if (request.getTimestamp() == null) {
            request.setTimestamp(Instant.now());
        }

        log.info("Dispatching audit event [{}] for entity [{}] to Kafka topic [{}]",
                eventId, request.getEntityId(), auditTopic);

        // Crucial: entityId is used as message key for partition ordering!
        kafkaTemplate.send(auditTopic, request.getEntityId(), request);

        return AuditEventResponse.builder()
                .eventId(eventId)
                .entityId(request.getEntityId())
                .eventType(request.getEventType())
                .status("QUEUED_IN_KAFKA")
                .submittedAt(Instant.now())
                .build();
    }
}
