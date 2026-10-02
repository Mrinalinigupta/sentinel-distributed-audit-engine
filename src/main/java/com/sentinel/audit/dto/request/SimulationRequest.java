package com.sentinel.audit.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

/**
 * Parameters for the built-in demo traffic generator.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Parameters to trigger realistic mock audit traffic")
public class SimulationRequest {

    @Schema(description = "Optional entity ID. If omitted, a random UUID will be generated.", example = "ACC_DEMO_01")
    private String entityId;

    @Min(value = 1, message = "Count must be at least 1")
    @Max(value = 100, message = "Count cannot exceed 100 events per simulation batch")
    @Schema(description = "Number of realistic events to generate in sequence", example = "10")
    @Builder.Default
    private int count = 10;

    @Schema(description = "Simulation scenario: FINTECH_ESCROW, USER_ACCESS_AUDIT, or ECOMMERCE_PAYMENTS", example = "FINTECH_ESCROW")
    @Builder.Default
    private String scenario = "FINTECH_ESCROW";
}
