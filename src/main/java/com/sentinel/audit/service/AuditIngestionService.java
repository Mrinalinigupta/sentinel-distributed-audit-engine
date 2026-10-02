package com.sentinel.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.audit.crypto.HashChainService;
import com.sentinel.audit.dto.request.AuditEventRequest;
import com.sentinel.audit.entity.AuditRecord;
import com.sentinel.audit.repository.AuditRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Core Ingestion Service.
 * 
 * 1. Resolves prior chain state (Genesis or Head).
 * 2. Invokes HashChainService to compute sequential SHA-256 seal.
 * 3. Persists to PostgreSQL immutable ledger.
 * 4. Updates Redis hot timeline cache (LPUSH + LTRIM).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditIngestionService {

    private final AuditRecordRepository auditRecordRepository;
    private final HashChainService hashChainService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${sentinel.cache.timeline-limit:20}")
    private int timelineLimit;

    @Transactional
    public AuditRecord ingestAndSeal(AuditEventRequest request) {
        String entityId = request.getEntityId();
        Instant timestamp = request.getTimestamp() != null ? request.getTimestamp() : Instant.now();

        // 1. Serialize payload to deterministic JSON string
        String payloadJson = serializePayload(request.getPayload());

        // 2. Fetch the latest record for this entity to chain forward
        Optional<AuditRecord> latestRecordOpt = auditRecordRepository
                .findTopByEntityIdOrderBySequenceNumberDesc(entityId);

        Long sequenceNumber;
        String previousHash;

        if (latestRecordOpt.isEmpty()) {
            // Genesis Block
            sequenceNumber = 1L;
            previousHash = HashChainService.GENESIS_HASH;
            log.info("Creating GENESIS Block (Sequence #1) for entity [{}]", entityId);
        } else {
            AuditRecord latest = latestRecordOpt.get();
            sequenceNumber = latest.getSequenceNumber() + 1L;
            previousHash = latest.getCurrentHash();
            log.info("Chaining Block (Sequence #{}) to previous hash [{}] for entity [{}]",
                    sequenceNumber, previousHash.substring(0, 10) + "...", entityId);
        }

        // 3. Compute SHA-256 seal
        String currentHash = hashChainService.calculateHash(
                previousHash,
                entityId,
                request.getEventType(),
                request.getActor(),
                payloadJson,
                timestamp,
                sequenceNumber
        );

        // 4. Build and persist immutable AuditRecord
        AuditRecord record = AuditRecord.builder()
                .entityId(entityId)
                .eventType(request.getEventType())
                .actor(request.getActor())
                .payload(payloadJson)
                .timestamp(timestamp)
                .sequenceNumber(sequenceNumber)
                .previousHash(previousHash)
                .currentHash(currentHash)
                .build();

        AuditRecord savedRecord = auditRecordRepository.save(record);
        log.info("Persisted AuditRecord [ID: {}] sealed with hash [{}]",
                savedRecord.getId(), currentHash.substring(0, 10) + "...");

        // 5. Update Redis hot timeline cache
        updateRedisTimelineCache(entityId, savedRecord);

        return savedRecord;
    }

    private void updateRedisTimelineCache(String entityId, AuditRecord record) {
        try {
            String cacheKey = "audit:timeline:" + entityId;
            redisTemplate.opsForList().leftPush(cacheKey, record);
            redisTemplate.opsForList().trim(cacheKey, 0, timelineLimit - 1);
            log.debug("Updated Redis timeline cache for key [{}]", cacheKey);
        } catch (Exception e) {
            // Cache failure must never rollback primary DB transaction (Graceful Degradation)
            log.warn("Redis cache update failed for entity [{}]: {}. Continuing without cache.",
                    entityId, e.getMessage());
        }
    }

    private String serializePayload(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payload to JSON, falling back to toString()", e);
            return String.valueOf(payload);
        }
    }
}
