package com.statesnap.controller;

import com.statesnap.dto.ApiResponse;
import com.statesnap.dto.RecoveryAnalysisDto;
import com.statesnap.service.RecoveryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions/{transactionId}/recovery")
public class RecoveryController {

    private final RecoveryService recoveryService;

    public RecoveryController(RecoveryService recoveryService) {
        this.recoveryService = recoveryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<RecoveryAnalysisDto>> getRecoveryAnalysis(@PathVariable String transactionId) {
        RecoveryAnalysisDto dto = recoveryService.getLatestRecovery(transactionId);
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse<RecoveryAnalysisDto>> triggerRecoveryAnalysis(@PathVariable String transactionId) {
        RecoveryAnalysisDto dto = recoveryService.analyzeRecovery(transactionId);
        return ResponseEntity.ok(ApiResponse.ok("Recovery analysis completed", dto));
    }
}

