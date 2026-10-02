package com.sentinel.audit.crypto;

import com.sentinel.audit.entity.AuditRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class HashChainServiceTest {

    private HashChainService hashChainService;

    @BeforeEach
    void setUp() {
        hashChainService = new HashChainService();
    }

    @Test
    @DisplayName("Should deterministically generate identical SHA-256 hash for identical inputs")
    void testDeterministicHashing() {
        Instant now = Instant.parse("2026-09-29T10:00:00Z");
        String hash1 = hashChainService.calculateHash(
                HashChainService.GENESIS_HASH,
                "ACC_1001",
                "ESCROW_LOCKED",
                "system",
                "{\"amount\": 50000}",
                now,
                1L
        );

        String hash2 = hashChainService.calculateHash(
                HashChainService.GENESIS_HASH,
                "ACC_1001",
                "ESCROW_LOCKED",
                "system",
                "{\"amount\": 50000}",
                now,
                1L
        );

        assertNotNull(hash1);
        assertEquals(64, hash1.length(), "SHA-256 hex string must be 64 characters long");
        assertEquals(hash1, hash2, "Identical inputs must produce identical SHA-256 hashes");
    }

    @Test
    @DisplayName("Should demonstrate the cryptographic avalanche effect when payload is subtly altered")
    void testAvalancheEffect() {
        Instant now = Instant.parse("2026-09-29T10:00:00Z");

        String originalHash = hashChainService.calculateHash(
                HashChainService.GENESIS_HASH,
                "ACC_1001",
                "PAYMENT_DISBURSED",
                "admin",
                "{\"amount\": 500}",
                now,
                1L
        );

        // Altering just 1 digit: from 500 to 5000
        String tamperedHash = hashChainService.calculateHash(
                HashChainService.GENESIS_HASH,
                "ACC_1001",
                "PAYMENT_DISBURSED",
                "admin",
                "{\"amount\": 5000}",
                now,
                1L
        );

        assertNotEquals(originalHash, tamperedHash, "Altering even 1 character must result in a completely different hash");
    }

    @Test
    @DisplayName("Should successfully verify a valid cryptographic chain between Block 1 and Block 2")
    void testVerifyLinkSuccess() {
        Instant t1 = Instant.parse("2026-09-29T10:00:00Z");
        String hash1 = hashChainService.calculateHash(
                HashChainService.GENESIS_HASH, "ACC_1001", "ACCOUNT_OPENED", "admin", "{}", t1, 1L
        );

        AuditRecord block1 = AuditRecord.builder()
                .id(1L)
                .entityId("ACC_1001")
                .eventType("ACCOUNT_OPENED")
                .actor("admin")
                .payload("{}")
                .timestamp(t1)
                .sequenceNumber(1L)
                .previousHash(HashChainService.GENESIS_HASH)
                .currentHash(hash1)
                .build();

        // Block 1 links to Genesis
        assertTrue(hashChainService.verifyLink(null, block1));

        Instant t2 = Instant.parse("2026-09-29T10:05:00Z");
        String hash2 = hashChainService.calculateHash(
                hash1, "ACC_1001", "DEPOSIT_RECEIVED", "system", "{\"amount\": 1000}", t2, 2L
        );

        AuditRecord block2 = AuditRecord.builder()
                .id(2L)
                .entityId("ACC_1001")
                .eventType("DEPOSIT_RECEIVED")
                .actor("system")
                .payload("{\"amount\": 1000}")
                .timestamp(t2)
                .sequenceNumber(2L)
                .previousHash(hash1)
                .currentHash(hash2)
                .build();

        // Block 2 links to Block 1
        assertTrue(hashChainService.verifyLink(block1, block2));
    }

    @Test
    @DisplayName("Should detect tamper if Block 1 payload is altered after Block 2 is chained")
    void testDetectTamperInChain() {
        Instant t1 = Instant.parse("2026-09-29T10:00:00Z");
        String hash1 = hashChainService.calculateHash(
                HashChainService.GENESIS_HASH, "ACC_1001", "ACCOUNT_OPENED", "admin", "{\"balance\": 100}", t1, 1L
        );

        AuditRecord block1 = AuditRecord.builder()
                .id(1L)
                .entityId("ACC_1001")
                .eventType("ACCOUNT_OPENED")
                .actor("admin")
                .payload("{\"balance\": 100}")
                .timestamp(t1)
                .sequenceNumber(1L)
                .previousHash(HashChainService.GENESIS_HASH)
                .currentHash(hash1)
                .build();

        // Simulate rogue edit in Block 1 payload to 1000000 without updating hash
        block1.setPayload("{\"balance\": 1000000}");

        // verifyLink on block1 must now fail because recomputed hash won't match stored currentHash
        assertFalse(hashChainService.verifyLink(null, block1));
    }
}
