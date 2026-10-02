package com.sentinel.audit.service;

import com.sentinel.audit.crypto.HashChainService;
import com.sentinel.audit.dto.response.VerificationReportResponse;
import com.sentinel.audit.entity.AuditRecord;
import com.sentinel.audit.exception.EntityAuditNotFoundException;
import com.sentinel.audit.exception.TamperDetectedException;
import com.sentinel.audit.repository.AuditRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Compliance & Tamper Verification Engine.
 * 
 * Walks the mathematical SHA-256 chain for an entity from Genesis block (#1) to Head.
 * If any row was edited, deleted, or inserted out of order, the chain breaks instantly.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationService {

    private final AuditRecordRepository auditRecordRepository;
    private final HashChainService hashChainService;

    /**
     * Verifies the cryptographic integrity of an entity's complete historical ledger.
     * Throws TamperDetectedException on mismatch.
     */
    public VerificationReportResponse verifyLedger(String entityId) {
        List<AuditRecord> records = auditRecordRepository.findByEntityIdOrderBySequenceNumberAsc(entityId);

        if (records.isEmpty()) {
            throw new EntityAuditNotFoundException(entityId);
        }

        log.info("Starting cryptographic chain audit for entity [{}] across {} records",
                entityId, records.size());

        AuditRecord previousRecord = null;

        for (int i = 0; i < records.size(); i++) {
            AuditRecord current = records.get(i);
            long expectedSeq = (i + 1L);

            // 1. Verify Sequence Monotonicity
            if (current.getSequenceNumber() != expectedSeq) {
                String error = String.format("Sequence gap or reordering detected! Expected sequence [%d] but found [%d]",
                        expectedSeq, current.getSequenceNumber());
                log.error("🚨 {}", error);
                throw new TamperDetectedException(entityId, current.getSequenceNumber(), "Seq " + expectedSeq, "Seq " + current.getSequenceNumber(), error);
            }

            // 2. Verify Pointer to Previous Hash
            if (i == 0) {
                // Block 1 must link to Genesis
                if (!HashChainService.GENESIS_HASH.equals(current.getPreviousHash())) {
                    String error = "Genesis Block #1 pointer is corrupt! Does not match GENESIS_HASH constant.";
                    log.error("🚨 {}", error);
                    throw new TamperDetectedException(entityId, 1L, HashChainService.GENESIS_HASH, current.getPreviousHash(), error);
                }
            } else {
                // Block i must link to Block i-1 currentHash
                if (!current.getPreviousHash().equals(previousRecord.getCurrentHash())) {
                    String error = String.format("Chain pointer broken at Sequence #%d! Previous block hash was [%s], but current block points to [%s]",
                            current.getSequenceNumber(), previousRecord.getCurrentHash(), current.getPreviousHash());
                    log.error("🚨 {}", error);
                    throw new TamperDetectedException(entityId, current.getSequenceNumber(), previousRecord.getCurrentHash(), current.getPreviousHash(), error);
                }
            }

            // 3. Recompute SHA-256 seal dynamically from raw column data
            String recomputedHash = hashChainService.calculateHash(
                    current.getPreviousHash(),
                    current.getEntityId(),
                    current.getEventType(),
                    current.getActor(),
                    current.getPayload(),
                    current.getTimestamp(),
                    current.getSequenceNumber()
            );

            if (!recomputedHash.equals(current.getCurrentHash())) {
                String error = String.format("Cryptographic seal INVALID at Sequence #%d! Data within this record was tampered with after creation.",
                        current.getSequenceNumber());
                log.error("🚨 {} Expected recomputed hash: [{}], but found stored hash: [{}]",
                        error, recomputedHash, current.getCurrentHash());
                throw new TamperDetectedException(entityId, current.getSequenceNumber(), recomputedHash, current.getCurrentHash(), error);
            }

            previousRecord = current;
        }

        log.info("✅ Cryptographic audit PASSED for entity [{}]. All {} blocks verified intact.",
                entityId, records.size());

        return VerificationReportResponse.builder()
                .entityId(entityId)
                .verified(true)
                .status("VERIFIED_INTACT")
                .totalRecordsChecked(records.size())
                .tamperedSequenceNumber(null)
                .expectedHash(null)
                .storedHash(null)
                .message(String.format("All %d blocks cryptographically verified from Genesis to Head. Mathematical chain is 100%% intact.", records.size()))
                .verifiedAt(Instant.now())
                .build();
    }
}
