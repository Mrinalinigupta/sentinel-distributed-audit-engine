package com.sentinel.audit.controller;

import com.sentinel.audit.dto.response.VerificationReportResponse;
import com.sentinel.audit.service.VerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(name = "3. Compliance & Verification", description = "Cryptographic auditor endpoint proving zero tampering across historical ledger")
public class ComplianceController {

    private final VerificationService verificationService;

    @GetMapping("/{entityId}/verify")
    @Operation(
            summary = "Verify ledger integrity",
            description = "Walks the complete SHA-256 hash chain for an entity from Block #1 to Head. Returns 200 OK with verification certificate if intact, or 409 Conflict if tampering is detected."
    )
    public ResponseEntity<VerificationReportResponse> verifyLedger(
            @PathVariable @Parameter(description = "Entity or Account ID", example = "ACC_1001") String entityId
    ) {
        VerificationReportResponse report = verificationService.verifyLedger(entityId);
        return ResponseEntity.ok(report);
    }
}
