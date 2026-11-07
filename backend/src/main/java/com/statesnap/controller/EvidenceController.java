package com.statesnap.controller;

import com.statesnap.dto.ApiResponse;
import com.statesnap.dto.EvidenceRecordDto;
import com.statesnap.service.ReconstructionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions/{transactionId}/evidence")
public class EvidenceController {

    private final ReconstructionService reconstructionService;

    public EvidenceController(ReconstructionService reconstructionService) {
        this.reconstructionService = reconstructionService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EvidenceRecordDto>>> getEvidence(@PathVariable String transactionId) {
        List<EvidenceRecordDto> list = reconstructionService.getEvidenceRecords(transactionId);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }
}

