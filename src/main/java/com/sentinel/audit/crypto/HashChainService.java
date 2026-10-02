package com.sentinel.audit.crypto;

import com.sentinel.audit.entity.AuditRecord;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

/**
 * Cryptographic Hashing Engine.
 * 
 * Implements deterministic SHA-256 hash chaining (similar to Git commit trees / blockchain blocks).
 * 
 * Formula:
 * currentHash = SHA-256(previousHash | entityId | eventType | actor | payload | timestamp | sequenceNumber)
 * 
 * The Genesis block (Sequence #1) uses a constant 64-character zero-hash as its previousHash.
 */
@Service
public class HashChainService {

    public static final String GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";
    private static final String DELIMITER = "|";

    /**
     * Computes the SHA-256 seal for an audit event.
     */
    public String calculateHash(
            String previousHash,
            String entityId,
            String eventType,
            String actor,
            String payload,
            Instant timestamp,
            Long sequenceNumber
    ) {
        String canonicalString = String.join(DELIMITER,
                previousHash != null ? previousHash : GENESIS_HASH,
                entityId != null ? entityId.trim() : "",
                eventType != null ? eventType.trim() : "",
                actor != null ? actor.trim() : "",
                payload != null ? payload.trim() : "{}",
                timestamp != null ? timestamp.toString() : "",
                sequenceNumber != null ? sequenceNumber.toString() : "1"
        );

        return applySha256(canonicalString);
    }

    /**
     * Validates whether a current audit record correctly and mathematically chains to its predecessor.
     */
    public boolean verifyLink(AuditRecord previousRecord, AuditRecord currentRecord) {
        if (currentRecord == null) {
            return false;
        }

        // 1. Sequence must be exactly 1 greater than previous
        if (previousRecord == null) {
            if (currentRecord.getSequenceNumber() != 1L) {
                return false;
            }
            if (!GENESIS_HASH.equals(currentRecord.getPreviousHash())) {
                return false;
            }
        } else {
            if (currentRecord.getSequenceNumber() != previousRecord.getSequenceNumber() + 1) {
                return false;
            }
            if (!currentRecord.getPreviousHash().equals(previousRecord.getCurrentHash())) {
                return false;
            }
        }

        // 2. Recompute and verify SHA-256 seal
        String recomputedHash = calculateHash(
                currentRecord.getPreviousHash(),
                currentRecord.getEntityId(),
                currentRecord.getEventType(),
                currentRecord.getActor(),
                currentRecord.getPayload(),
                currentRecord.getTimestamp(),
                currentRecord.getSequenceNumber()
        );

        return recomputedHash.equals(currentRecord.getCurrentHash());
    }

    /**
     * Helper to compute SHA-256 hex digest.
     */
    public String applySha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available in current JVM", e);
        }
    }
}
