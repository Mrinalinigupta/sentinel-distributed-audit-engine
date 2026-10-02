package com.sentinel.audit.controller;

import com.sentinel.audit.dto.response.TimelineEventResponse;
import com.sentinel.audit.service.AuditQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(name = "2. Timeline Query", description = "Endpoints for fetching audit event trails (Redis Cache-Aside)")
public class AuditQueryController {

    private final AuditQueryService auditQueryService;

    @GetMapping("/{entityId}/timeline")
    @Operation(summary = "Get entity audit timeline", description = "Retrieves recent audit events for an entity. Reads from Redis hot-cache with PostgreSQL fallback.")
    public ResponseEntity<List<TimelineEventResponse>> getTimeline(
            @PathVariable @Parameter(description = "Entity or Account ID", example = "ACC_1001") String entityId,
            @RequestParam(defaultValue = "20") @Parameter(description = "Maximum events to return", example = "20") int limit
    ) {
        List<TimelineEventResponse> timeline = auditQueryService.getTimeline(entityId, limit);
        return ResponseEntity.ok(timeline);
    }
}
