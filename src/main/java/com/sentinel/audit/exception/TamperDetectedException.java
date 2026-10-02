package com.sentinel.audit.exception;

import lombok.Getter;

/**
 * Thrown whenever a record's currentHash does not match the dynamically recomputed SHA-256 hash,
 * or when the previousHash pointer is broken.
 */
@Getter
public class TamperDetectedException extends RuntimeException {

    private final String entityId;
    private final Long sequenceNumber;
    private final String expectedHash;
    private final String actualHash;

    public TamperDetectedException(String entityId, Long sequenceNumber, String expectedHash, String actualHash, String message) {
        super(message);
        this.entityId = entityId;
        this.sequenceNumber = sequenceNumber;
        this.expectedHash = expectedHash;
        this.actualHash = actualHash;
    }
}
