package com.sentinel.audit.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

/**
 * Single event representation in an entity's audit timeline.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Single entry in an entity's historical audit timeline")
public class TimelineEventResponse {

    @Schema(description = "Database ID", example = "104")
    private Long id;

    @Schema(description = "Entity ID", example = "ACC_1001")
    private String entityId;

    @Schema(description = "Event type", example = "ESCROW_LOCKED")
    private String eventType;

    @Schema(description = "Actor who triggered the event", example = "system")
    private String actor;

    @Schema(description = "JSON payload containing event metadata")
    private String payload;

    @Schema(description = "Timestamp when the event occurred")
    private Instant timestamp;

    @Schema(description = "Sequential index in the cryptographic chain", example = "3")
    private Long sequenceNumber;

    @Schema(description = "SHA-256 hash of the previous record in chain", example = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
    private String previousHash;

    @Schema(description = "SHA-256 cryptographic seal of this record", example = "8f434346648f6b96df89dda901c5176b10f607629f7660cc63254c77f0088880")
    private String currentHash;
}
