package com.sentinel.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Sentinel: Distributed Audit & Compliance Engine
 * 
 * An event-sourced, tamper-evident audit platform featuring:
 * - High-speed ingestion via Apache Kafka
 * - Cryptographic SHA-256 hash chaining (blockchain-style tamper detection)
 * - Append-only immutable ledger in PostgreSQL
 * - Sub-2ms hot timeline cache in Redis
 * - Dead Letter Queue (DLQ) consumer resilience
 */
@SpringBootApplication
@EnableKafka
public class SentinelAuditApplication {

    public static void main(String[] args) {
        SpringApplication.run(SentinelAuditApplication.class, args);
    }
}
