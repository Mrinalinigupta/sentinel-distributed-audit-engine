package com.sentinel.audit.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Immutable Audit Record Entity.
 * 
 * Represents a single cryptographically-sealed entry in the audit ledger.
 * Once written, this record must NEVER be updated or deleted.
 * 
 * Cryptographic link:
 * currentHash = SHA-256(previousHash | entityId | eventType | actor | payload | timestamp | sequenceNumber)
 */
@Entity
@Table(
    name = "audit_records",
    indexes = {
        @Index(name = "idx_audit_entity_seq", columnList = "entity_id, sequence_number"),
        @Index(name = "idx_audit_timestamp", columnList = "timestamp")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_audit_entity_seq", columnNames = {"entity_id", "sequence_number"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_id", nullable = false, length = 100)
    private String entityId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "actor", nullable = false, length = 100)
    private String actor;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "sequence_number", nullable = false)
    private Long sequenceNumber;

    @Column(name = "previous_hash", nullable = false, length = 64)
    private String previousHash;

    @Column(name = "current_hash", nullable = false, length = 64)
    private String currentHash;
}
