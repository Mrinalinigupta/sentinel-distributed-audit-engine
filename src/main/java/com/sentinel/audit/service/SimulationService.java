package com.sentinel.audit.service;

import com.sentinel.audit.dto.request.AuditEventRequest;
import com.sentinel.audit.dto.request.SimulationRequest;
import com.sentinel.audit.dto.response.AuditEventResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Built-in Demo & Load Traffic Generator.
 * 
 * Generates realistic financial, security, and access event batches
 * for instant one-click live demonstrations in Swagger UI.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SimulationService {

    private final AuditProducerService auditProducerService;

    public List<AuditEventResponse> generateTraffic(SimulationRequest request) {
        String entityId = (request.getEntityId() != null && !request.getEntityId().isBlank())
                ? request.getEntityId().trim()
                : "ACC_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        int count = Math.min(Math.max(request.getCount(), 1), 50);
        log.info("Generating {} simulated [{}] audit events for entity [{}]",
                count, request.getScenario(), entityId);

        List<AuditEventResponse> responses = new ArrayList<>();
        List<String> eventTemplates = getScenarioTemplates(request.getScenario());

        for (int i = 0; i < count; i++) {
            String eventType = eventTemplates.get(i % eventTemplates.size());
            Map<String, Object> payload = buildRealisticPayload(eventType, i + 1);

            AuditEventRequest auditEventRequest = AuditEventRequest.builder()
                    .entityId(entityId)
                    .eventType(eventType)
                    .actor(i % 2 == 0 ? "system_escrow_engine" : "compliance_admin@bank.com")
                    .payload(payload)
                    .timestamp(Instant.now().plusSeconds(i * 2))
                    .build();

            AuditEventResponse response = auditProducerService.publishEvent(auditEventRequest);
            responses.add(response);
        }

        return responses;
    }

    private List<String> getScenarioTemplates(String scenario) {
        if ("USER_ACCESS_AUDIT".equalsIgnoreCase(scenario)) {
            return List.of(
                    "USER_REGISTERED",
                    "MFA_CHALLENGE_ISSUED",
                    "MFA_ENROLLED",
                    "PASSWORD_CHANGED",
                    "ROLE_PROMOTED_ADMIN",
                    "API_KEY_ROTATED"
            );
        }
        // Default: FINTECH_ESCROW
        return List.of(
                "ESCROW_CONTRACT_CREATED",
                "BUYER_FUNDS_LOCKED",
                "KYC_AML_VERIFIED",
                "MILESTONE_1_SUBMITTED",
                "MILESTONE_1_APPROVED",
                "DISBURSEMENT_SCHEDULED",
                "FUNDS_SETTLED_TO_SELLER"
        );
    }

    private Map<String, Object> buildRealisticPayload(String eventType, int step) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("step", step);
        payload.put("action", eventType);
        payload.put("currency", "INR");

        if (eventType.contains("FUNDS") || eventType.contains("DISBURSEMENT")) {
            payload.put("amount", 25000.0 * step);
            payload.put("settlementChannel", "NEFT_RTGS");
        } else if (eventType.contains("KYC")) {
            payload.put("riskScore", 0.02);
            payload.put("amlStatus", "CLEARED");
        } else {
            payload.put("metadata", "Verified state transition in compliance with financial regulations");
        }
        return payload;
    }
}
