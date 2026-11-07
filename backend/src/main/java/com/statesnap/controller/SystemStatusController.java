package com.statesnap.controller;

import com.statesnap.dto.ApiResponse;
import com.statesnap.dto.SystemStatusDto;
import com.statesnap.repository.TransactionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/system")
public class SystemStatusController {

    private final TransactionRepository transactionRepository;

    public SystemStatusController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<SystemStatusDto>> getSystemStatus() {
        String dbStatus = "HEALTHY";
        String overallStatus = "ONLINE";

        try {
            // Non-destructive probe to verify database connectivity
            transactionRepository.count();
        } catch (Exception e) {
            dbStatus = "UNREACHABLE";
            overallStatus = "DEGRADED";
        }

        SystemStatusDto status = SystemStatusDto.builder()
                .status(overallStatus)
                .engineVersion("RECON-v1")
                .databaseStatus(dbStatus)
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.ok(ApiResponse.ok("System status operational", status));
    }
}

