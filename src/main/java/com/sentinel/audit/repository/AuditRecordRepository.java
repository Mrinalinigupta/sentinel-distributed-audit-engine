package com.sentinel.audit.repository;

import com.sentinel.audit.entity.AuditRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for AuditRecord.
 * 
 * Provides queries to:
 * 1. Fetch the head of the chain (latest sequence) to link the next incoming event.
 * 2. Retrieve the complete chronological sequence for cryptographic validation.
 * 3. Retrieve recent history for timeline inspection.
 */
@Repository
public interface AuditRecordRepository extends JpaRepository<AuditRecord, Long> {

    /**
     * Finds the latest record for an entity to determine the previousHash and next sequenceNumber.
     */
    Optional<AuditRecord> findTopByEntityIdOrderBySequenceNumberDesc(String entityId);

    /**
     * Fetches all records for an entity in strict chronological sequence (1, 2, 3...)
     * to recalculate and verify the entire SHA-256 chain.
     */
    List<AuditRecord> findByEntityIdOrderBySequenceNumberAsc(String entityId);

    /**
     * Fetches recent records ordered by newest first for timeline views.
     */
    List<AuditRecord> findByEntityIdOrderBySequenceNumberDesc(String entityId, Pageable pageable);

    /**
     * Returns true if an audit trail exists for this entity.
     */
    boolean existsByEntityId(String entityId);

    /**
     * Counts the total number of audit records for an entity.
     */
    long countByEntityId(String entityId);
}
