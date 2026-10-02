package com.sentinel.audit.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.Map;

/**
 * Ingestion payload sent by external microservices or API clients.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payload for submitting an event to the audit ledger")
public class AuditEventRequest {

    @NotBlank(message = "entityId is required (e.g. ACC_1001, TXN_901)")
    @Schema(description = "Unique identifier of the entity/account", example = "ACC_1001")
    private String entityId;

    @NotBlank(message = "eventType is required (e.g. PAYMENT_DISBURSED, ROLE_UPDATED)")
    @Schema(description = "Domain action or event type", example = "PAYMENT_DISBURSED")
    private String eventType;

    @NotBlank(message = "actor is required (e.g. admin@bank.com, service_escrow)")
    @Schema(description = "User or service that initiated the event", example = "admin@bank.com")
    private String actor;

    @NotNull(message = "payload is required")
    @Schema(description = "Key-value metadata describing the state or delta", example = "{\"amount\": 50000.0, \"currency\": \"INR\", \"target\": \"VENDOR_44\"}")
    private Map<String, Object> payload;

    @Schema(description = "Timestamp when the event occurred (optional, defaults to now)")
    private Instant timestamp;
}
