package com.sentinel.audit.integration;

import com.sentinel.audit.dto.request.AuditEventRequest;
import com.sentinel.audit.dto.response.VerificationReportResponse;
import com.sentinel.audit.entity.AuditRecord;
import com.sentinel.audit.exception.TamperDetectedException;
import com.sentinel.audit.repository.AuditRecordRepository;
import com.sentinel.audit.service.AuditIngestionService;
import com.sentinel.audit.service.VerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
class TamperDetectionTest {

    @Autowired
    private AuditIngestionService auditIngestionService;

    @Autowired
    private VerificationService verificationService;

    @Autowired
    private AuditRecordRepository auditRecordRepository;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    void setUp() {
        auditRecordRepository.deleteAll();

        // Mock redis operations gracefully
        ListOperations<String, Object> listOperations = mock(ListOperations.class);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.leftPush(anyString(), any())).thenReturn(1L);
    }

    @Test
    @DisplayName("Should ingest 3 chained events and successfully pass cryptographic audit verification")
    void testValidLedgerVerification() {
        String entityId = "ACC_TEST_001";

        // Step 1: Ingest 3 events in sequence
        auditIngestionService.ingestAndSeal(AuditEventRequest.builder()
                .entityId(entityId)
                .eventType("ESCROW_INITIALIZED")
                .actor("buyer@fintech.com")
                .payload(Map.of("escrowId", "ESC_101", "amount", 50000.0))
                .timestamp(Instant.parse("2026-10-03T10:00:00Z"))
                .build());

        auditIngestionService.ingestAndSeal(AuditEventRequest.builder()
                .entityId(entityId)
                .eventType("FUNDS_LOCKED")
                .actor("system_vault")
                .payload(Map.of("escrowId", "ESC_101", "status", "LOCKED"))
                .timestamp(Instant.parse("2026-10-03T10:05:00Z"))
                .build());

        auditIngestionService.ingestAndSeal(AuditEventRequest.builder()
                .entityId(entityId)
                .eventType("FUNDS_RELEASED")
                .actor("admin@bank.com")
                .payload(Map.of("escrowId", "ESC_101", "releasedTo", "SELLER_99"))
                .timestamp(Instant.parse("2026-10-03T10:10:00Z"))
                .build());

        // Step 2: Run cryptographic verification
        VerificationReportResponse report = verificationService.verifyLedger(entityId);

        // Step 3: Assert clean bill of health
        assertNotNull(report);
        assertTrue(report.isVerified());
        assertEquals("VERIFIED_INTACT", report.getStatus());
        assertEquals(3, report.getTotalRecordsChecked());
        assertNull(report.getTamperedSequenceNumber());
    }

    @Test
    @DisplayName("Should detect rogue database tampering at Block #2 and throw TamperDetectedException")
    void testTamperDetectionOnRogueDatabaseEdit() {
        String entityId = "ACC_TAMPER_002";

        // Step 1: Ingest 3 valid events
        auditIngestionService.ingestAndSeal(AuditEventRequest.builder()
                .entityId(entityId)
                .eventType("ESCROW_CREATED")
                .actor("buyer@fintech.com")
                .payload(Map.of("amount", 1000.0))
                .timestamp(Instant.parse("2026-10-03T10:00:00Z"))
                .build());

        auditIngestionService.ingestAndSeal(AuditEventRequest.builder()
                .entityId(entityId)
                .eventType("PAYMENT_SETTLED")
                .actor("system")
                .payload(Map.of("amount", 1000.0))
                .timestamp(Instant.parse("2026-10-03T10:05:00Z"))
                .build());

        auditIngestionService.ingestAndSeal(AuditEventRequest.builder()
                .entityId(entityId)
                .eventType("FROZEN")
                .actor("admin")
                .payload(Map.of("reason", "AUDIT_REVIEW"))
                .timestamp(Instant.parse("2026-10-03T10:10:00Z"))
                .build());

        // Step 2: Simulate rogue DBA directly editing Row #2 (changing amount from 1000 to 999999)
        var records = auditRecordRepository.findByEntityIdOrderBySequenceNumberAsc(entityId);
        assertEquals(3, records.size());

        AuditRecord block2 = records.get(1); // Sequence #2
        block2.setPayload("{\"amount\":999999.0}");
        auditRecordRepository.saveAndFlush(block2);

        // Step 3: Run cryptographic verification and assert immediate tamper alarm
        TamperDetectedException exception = assertThrows(
                TamperDetectedException.class,
                () -> verificationService.verifyLedger(entityId),
                "Verification must fail when row payload was modified without valid SHA-256 seal"
        );

        assertEquals(entityId, exception.getEntityId());
        assertEquals(2L, exception.getSequenceNumber(), "Alarm must pinpoint Sequence #2 as the corrupted record!");
        assertNotNull(exception.getExpectedHash());
        assertNotNull(exception.getActualHash());
        assertNotEquals(exception.getExpectedHash(), exception.getActualHash());
    }
}
