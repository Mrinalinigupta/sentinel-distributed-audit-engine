package com.sentinel.audit.controller;

import com.sentinel.audit.dto.request.AuditEventRequest;
import com.sentinel.audit.dto.response.AuditEventResponse;
import com.sentinel.audit.service.AuditProducerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(name = "1. Ingestion Gateway", description = "Endpoints for publishing audit events asynchronously into Kafka")
public class AuditIngestController {

    private final AuditProducerService auditProducerService;

    @PostMapping("/events")
    @Operation(summary = "Publish an audit event", description = "Asynchronously streams an audit event to Kafka, keyed by entityId for chronological ordering.")
    public ResponseEntity<AuditEventResponse> ingestEvent(@Valid @RequestBody AuditEventRequest request) {
        AuditEventResponse response = auditProducerService.publishEvent(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
