package com.sentinel.audit.controller;

import com.sentinel.audit.dto.request.SimulationRequest;
import com.sentinel.audit.dto.response.AuditEventResponse;
import com.sentinel.audit.service.SimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/simulation")
@RequiredArgsConstructor
@Tag(name = "4. Demo Traffic Simulator", description = "Instant one-click mock event generation for live demonstrations")
public class SimulationController {

    private final SimulationService simulationService;

    @PostMapping("/generate")
    @Operation(summary = "Generate simulated audit traffic", description = "Fires a realistic sequence of financial or security audit events through Kafka for testing and live demonstrations.")
    public ResponseEntity<List<AuditEventResponse>> generateSimulatedTraffic(@RequestBody(required = false) SimulationRequest request) {
        if (request == null) {
            request = new SimulationRequest();
        }
        List<AuditEventResponse> responses = simulationService.generateTraffic(request);
        return ResponseEntity.ok(responses);
    }
}
