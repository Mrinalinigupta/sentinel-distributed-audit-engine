# 🛡️ Sentinel: Distributed Audit & Compliance Engine
> **Event-Sourced, Tamper-Evident Immutable Audit Ledger for Financial & Security Transactions**

[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.4-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Apache Kafka](https://img.shields.io/badge/Apache_Kafka-3.7-231F20?style=for-the-badge&logo=apache-kafka&logoColor=white)](https://kafka.apache.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io/)
[![Swagger](https://img.shields.io/badge/OpenAPI-Swagger_UI-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)](http://localhost:8081/swagger-ui.html)

---

## 📌 Executive Summary

**Sentinel** is an enterprise-grade distributed compliance engine designed to capture, cryptographically seal, and verify high-velocity financial and security events without adding latency to primary transactions.

Traditional audit logs suffer from three critical flaws:
1. **The Slowdown Problem**: Synchronously writing heavy audit logs inside the main transaction degrades payment throughput and adds latency.
2. **The Dual-Write Problem**: If an application updates business state in PostgreSQL but crashes before writing to an audit log, audit integrity is lost.
3. **The Rogue DBA Problem**: Anyone with direct database permissions can alter or delete rows in a standard database table without detection.

**Sentinel eliminates all three vulnerabilities** by decoupling ingestion via **Apache Kafka**, cryptographically sealing every record using **SHA-256 Hash Chaining (blockchain-style tamper detection)**, storing permanent records in **PostgreSQL**, and accelerating recent timeline reads using an in-memory **Redis** cache.

---

## 🏛️ System Architecture

```mermaid
flowchart TD
    subgraph Clients["1. Event Sources & Ingestion Layer"]
        Swagger["Swagger UI / Postman"]
        Simulator["Traffic Simulator (POST /api/v1/simulation/generate)"]
        ExternalApps["Microservices (TrustVault, Payment Gateways, Auth)"]
        IngestAPI["Audit Ingestion Gateway (POST /api/v1/audit/events)"]
    end

    Swagger --> IngestAPI
    Simulator --> IngestAPI
    ExternalApps --> IngestAPI

    subgraph KafkaStream["2. Distributed Streaming Infrastructure"]
        Producer["AuditProducerService\n(Keyed by entityId for guaranteed partition order)"]
        MainTopic["Kafka Topic: sentinel.audit.events.v1\n(3 Partitions, High Throughput)"]
        DLQTopic["Kafka DLQ: sentinel.audit.events.v1.DLQ\n(Poisoned / Malformed Records)"]
    end

    IngestAPI --> Producer
    Producer -->|Key: entityId| MainTopic

    subgraph Engine["3. Processing & Cryptographic Sealing"]
        Consumer["AuditEventConsumer\n(@KafkaListener with Exponential Backoff)"]
        DLQConsumer["DlqConsumer\n(Alerts & Dead-Letter Isolation)"]
        Crypto["HashChainService\nSHA-256(prev_hash + payload + seq + ts)"]
        IngestionSvc["AuditIngestionService\n(Dual Persistence Coordinator)"]
    end

    MainTopic --> Consumer
    Consumer -.->|Failed 3x Retries| DLQTopic
    DLQTopic --> DLQConsumer

    Consumer --> Crypto
    Crypto --> IngestionSvc

    subgraph Storage["4. Dual-Persistence & Read Acceleration"]
        Postgres[(PostgreSQL 16\nImmutable Append-Only Ledger)]
        Redis[(Redis 7\nSub-2ms Hot Timeline Cache)]
    end

    IngestionSvc -->|INSERT (Append-Only)| Postgres
    IngestionSvc -->|LPUSH / LTRIM top 20| Redis

    subgraph Auditors["5. Compliance & Verification APIs"]
        TimelineAPI["GET /api/v1/audit/{entityId}/timeline\n(Redis Cache-Aside)"]
        VerifyAPI["GET /api/v1/audit/{entityId}/verify\n(Full Cryptographic Chain Walk)"]
    end

    TimelineAPI --> Redis
    TimelineAPI -.->|Cache Miss| Postgres
    VerifyAPI --> Postgres
```

---

## ⚡ Key Architectural Superpowers

### 1. Cryptographic Tamper-Evidence (SHA-256 Hash Chaining)
Every record in an entity's ledger contains a pointer to the previous record's hash, forming an unbreakable cryptographic chain:

$$\text{current\_hash} = \text{SHA-256}(\text{previous\_hash} + "|" + \text{entityId} + "|" + \text{eventType} + "|" + \text{actor} + "|" + \text{payload} + "|" + \text{timestamp} + "|" + \text{seq})$$

* **Genesis Block (Sequence #1)**: Seeded by a 64-character zero-constant (`0000000000000000...`).
* **The Domino Effect**: If a rogue database admin or attacker modifies even a single character in row #2 (e.g. changing `amount: 500` to `amount: 50000`), row #2's dynamic hash changes. This breaks row #3's `previous_hash` link, invalidating the entire chain.
* **Automated Audit Endpoint**: Calling `GET /api/v1/audit/{entityId}/verify` recalculates the entire mathematical chain and returns a cryptographic certificate or pinpoints the exact corrupted sequence number.

### 2. Chronological Ordering via Kafka Partition Keys
In distributed streaming, out-of-order messages can break cryptographic hash verification. Sentinel publishes all events using `entityId` as the **Kafka Record Key**. Apache Kafka mathematically guarantees that all messages sharing the same key land on the **same partition in strict sequential order**.

### 3. Consumer Resilience & Dead Letter Queue (DLQ)
Malformed or poisoned events do not crash the consumer or cause message-processing stalls:
* Failed messages trigger up to **3 exponential retries** (1s, 2s, 4s).
* Upon exhausting retries, Spring Kafka's `DeadLetterPublishingRecoverer` automatically diverts the corrupted message to `sentinel.audit.events.v1.DLQ`.
* Healthy traffic continues processing uninterrupted.

### 4. Sub-2ms Read Latency via Redis Cache-Aside
Audit timelines are queried frequently by customer support and compliance dashboards. Sentinel maintains a hot list in Redis (`audit:timeline:{entityId}`) capped at the 20 most recent events, achieving sub-2ms read response times with graceful fallback to PostgreSQL on cache misses.

---

## 🧰 Technology Stack

| Layer | Technology | Version | Purpose |
| :--- | :--- | :--- | :--- |
| **Language** | Java | 17 LTS | High-performance enterprise runtime |
| **Framework** | Spring Boot | 3.3.4 | Dependency injection, REST controllers, validation |
| **Streaming** | Apache Kafka (KRaft) | 3.7.0 | Distributed, partition-ordered event streaming |
| **Persistence** | PostgreSQL | 16 | ACID-compliant append-only relational ledger |
| **Caching** | Redis | 7.0 | In-memory hot timeline cache |
| **Testing** | JUnit 5 + Mockito + H2 | Latest | Deterministic unit & integration testing |
| **Documentation** | SpringDoc OpenAPI | 2.6.0 | Interactive Swagger UI API dashboard |

---

## 📂 Project Directory Structure

```
sentinel-audit/
├── docker-compose.yml              # Dedicated infra ports: Kafka (9092), Postgres (5433), Redis (6380)
├── pom.xml                         # Enterprise Maven dependencies
├── README.md                       # Architecture, setup guide, and interview pitch
└── src/
    ├── main/
    │   ├── java/com/sentinel/audit/
    │   │   ├── SentinelAuditApplication.java
    │   │   ├── config/             # KafkaConfig, RedisConfig, OpenApiConfig
    │   │   ├── consumer/           # AuditEventConsumer, DlqConsumer
    │   │   ├── controller/         # Ingest, Timeline, Compliance, Simulation Controllers
    │   │   ├── crypto/             # HashChainService (SHA-256 hashing & chain verification)
    │   │   ├── dto/                # Request/Response payloads with Jakarta validation
    │   │   ├── entity/             # AuditRecord (JPA entity with composite unique index)
    │   │   ├── exception/          # TamperDetectedException, GlobalExceptionHandler
    │   │   ├── repository/         # AuditRecordRepository (custom traversal queries)
    │   │   └── service/            # Producer, Ingestion, Query, Verification, Simulation Services
    │   └── resources/
    │       └── application.yml     # Application configuration (port 8081)
    └── test/
        ├── java/com/sentinel/audit/
        │   ├── crypto/HashChainServiceTest.java       # Unit tests: determinism & avalanche effect
        │   └── integration/TamperDetectionTest.java   # Integration test: live DB tamper detection
        └── resources/
            └── application.yml     # Test profile with in-memory H2 database
```

---

## 🚀 Getting Started

### 1. Prerequisites
* **Java 17+**
* **Maven 3.8+**
* **Docker & Docker Compose**

### 2. Start Infrastructure Containers
Sentinel uses non-colliding ports (`Postgres: 5433`, `Redis: 6380`, `Kafka: 9092`):
```bash
docker compose up -d
```

Verify containers are running:
```bash
docker ps
```

### 3. Build & Run Tests
```bash
mvn clean test
```
All 6 tests (crypto unit tests and live tamper detection integration tests) will execute and pass.

### 4. Launch the Application
```bash
mvn spring-boot:run
```
The application starts on **`http://localhost:8081`**.

---

## 🖥️ Interactive Swagger UI Dashboard

Open your browser to:
👉 **`http://localhost:8081/swagger-ui.html`**

```
┌────────────────────────────────────────────────────────────────────────┐
│  Sentinel - Distributed Audit & Compliance Engine API v1.0.0           │
└────────────────────────────────────────────────────────────────────────┘

1. Demo Traffic Simulator
   POST /api/v1/simulation/generate   - Generate 10-50 realistic events in Kafka

2. Ingestion Gateway
   POST /api/v1/audit/events          - Publish custom audit event to Kafka

3. Timeline Query
   GET  /api/v1/audit/{entityId}/timeline - Sub-2ms Redis-cached audit timeline

4. Compliance & Verification
   GET  /api/v1/audit/{entityId}/verify   - Run mathematical cryptographic audit
```

---

## 🧪 Live Tamper Detection Walkthrough (Demo Guide)

### Step 1: Generate Mock Audit Traffic
Execute in Swagger or run cURL:
```bash
curl -X POST "http://localhost:8081/api/v1/simulation/generate" \
     -H "Content-Type: application/json" \
     -d '{"entityId": "ACC_DEMO_101", "count": 5, "scenario": "FINTECH_ESCROW"}'
```

### Step 2: Verify Cryptographic Integrity
Run verification:
```bash
curl -X GET "http://localhost:8081/api/v1/audit/ACC_DEMO_101/verify"
```
**Response (`200 OK`)**:
```json
{
  "entityId": "ACC_DEMO_101",
  "verified": true,
  "status": "VERIFIED_INTACT",
  "totalRecordsChecked": 5,
  "tamperedSequenceNumber": null,
  "message": "All 5 blocks cryptographically verified from Genesis to Head. Mathematical chain is 100% intact.",
  "verifiedAt": "2026-10-03T10:00:00Z"
}
```

### Step 3: Simulate Rogue Database Tampering
Connect to PostgreSQL and alter a row:
```sql
UPDATE audit_records 
SET payload = '{"amount": 9999999.0}' 
WHERE entity_id = 'ACC_DEMO_101' AND sequence_number = 3;
```

### Step 4: Re-run Verification (Alarm Rings!)
```bash
curl -X GET "http://localhost:8081/api/v1/audit/ACC_DEMO_101/verify"
```
**Response (`409 Conflict`)**:
```json
{
  "status": 409,
  "error": "CRYPTOGRAPHIC_TAMPER_DETECTED",
  "message": "Cryptographic seal INVALID at Sequence #3! Data within this record was tampered with after creation.",
  "entityId": "ACC_DEMO_101",
  "tamperedSequenceNumber": 3,
  "expectedHash": "25ace7b295da7755c36826637a77cf50c91a7ed15c4e7ea597b5fb56a81cf3b6",
  "actualStoredHash": "4798b7a9f70a2396acedb863909645782bc84cd9d5e0b39011cb9e3f3a6b405c"
}
```

---

## 🎯 Interview Defense & Architecture Highlights

When discussing Sentinel in technical interviews:

* **Why Kafka instead of RabbitMQ?**  
  *"RabbitMQ is a traditional message broker where messages disappear once consumed. Kafka acts as an immutable distributed append-only commit log with partition ordering, replayability, and consumer group scalability."*
* **How do you prevent race conditions during hash computation?**  
  *"We partition by `entityId`. In Kafka, all events for a given account land on the exact same partition, guaranteeing single-consumer sequential processing per entity."*
* **What happens if Redis crashes?**  
  *"We follow the Cache-Aside pattern with graceful degradation. If Redis is down, ingestion still persists safely to PostgreSQL and timeline reads fall back to the database without throwing 500 errors."*
* **What happens if Kafka is down?**  
  *"Sentinel provides both a streaming Kafka consumer and a direct synchronous fallback. For end-to-end resilience in multi-service ecosystems, upstream services stage events in a local transactional Outbox table before publishing to Kafka."*

---

## 👤 Author
* **GitHub**: [@Mrinalinigupta](https://github.com/Mrinalinigupta)
* **Project**: Sentinel - Distributed Audit & Compliance Engine
