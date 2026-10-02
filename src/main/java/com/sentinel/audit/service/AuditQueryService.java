package com.sentinel.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.audit.dto.response.TimelineEventResponse;
import com.sentinel.audit.entity.AuditRecord;
import com.sentinel.audit.exception.EntityAuditNotFoundException;
import com.sentinel.audit.repository.AuditRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for querying audit timelines.
 * Uses a Redis-first Cache-Aside pattern:
 * 1. Checks Redis hot-cache list for recent timeline entries (< 2ms).
 * 2. On cache miss, falls back to PostgreSQL, populates cache, and returns.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditQueryService {

    private final AuditRecordRepository auditRecordRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public List<TimelineEventResponse> getTimeline(String entityId, int limit) {
        String cacheKey = "audit:timeline:" + entityId;

        // 1. Try fetching from Redis hot cache
        try {
            Long size = redisTemplate.opsForList().size(cacheKey);
            if (size != null && size > 0) {
                log.info("Cache HIT for entity timeline [{}] in Redis ({} records)", entityId, size);
                List<Object> rawList = redisTemplate.opsForList().range(cacheKey, 0, limit - 1);
                if (rawList != null && !rawList.isEmpty()) {
                    List<TimelineEventResponse> responseList = new ArrayList<>();
                    for (Object obj : rawList) {
                        AuditRecord record = objectMapper.convertValue(obj, AuditRecord.class);
                        responseList.add(mapToTimelineResponse(record));
                    }
                    return responseList;
                }
            }
        } catch (Exception e) {
            log.warn("Redis read failed for entity [{}]: {}. Falling back to PostgreSQL.", entityId, e.getMessage());
        }

        // 2. Fallback to PostgreSQL
        log.info("Cache MISS for entity timeline [{}]. Querying PostgreSQL.", entityId);
        List<AuditRecord> dbRecords = auditRecordRepository.findByEntityIdOrderBySequenceNumberDesc(
                entityId, PageRequest.of(0, limit)
        );

        if (dbRecords.isEmpty()) {
            throw new EntityAuditNotFoundException(entityId);
        }

        return dbRecords.stream()
                .map(this::mapToTimelineResponse)
                .toList();
    }

    private TimelineEventResponse mapToTimelineResponse(AuditRecord record) {
        return TimelineEventResponse.builder()
                .id(record.getId())
                .entityId(record.getEntityId())
                .eventType(record.getEventType())
                .actor(record.getActor())
                .payload(record.getPayload())
                .timestamp(record.getTimestamp())
                .sequenceNumber(record.getSequenceNumber())
                .previousHash(record.getPreviousHash())
                .currentHash(record.getCurrentHash())
                .build();
    }
}
