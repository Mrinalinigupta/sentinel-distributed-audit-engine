package com.sentinel.audit.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

/**
 * Confirmation returned after an audit event is accepted and published to Kafka.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Acknowledgement after event is submitted to Kafka pipeline")
public class AuditEventResponse {

    @Schema(description = "Assigned tracking ID", example = "ev-7a91fc3b")
    private String eventId;

    @Schema(description = "Target entity ID", example = "ACC_1001")
    private String entityId;

    @Schema(description = "Event type submitted", example = "PAYMENT_DISBURSED")
    private String eventType;

    @Schema(description = "Ingestion status", example = "QUEUED_IN_KAFKA")
    private String status;

    @Schema(description = "Timestamp when received by the ingestion gateway")
    private Instant submittedAt;
}
