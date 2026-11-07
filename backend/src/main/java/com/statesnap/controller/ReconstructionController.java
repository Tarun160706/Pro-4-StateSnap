package com.statesnap.controller;

import com.statesnap.dto.*;
import com.statesnap.service.ReconstructionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions/{transactionId}")
public class ReconstructionController {

    private final ReconstructionService reconstructionService;

    public ReconstructionController(ReconstructionService reconstructionService) {
        this.reconstructionService = reconstructionService;
    }

    @PostMapping("/reconstruct")
    public ResponseEntity<ApiResponse<InferredStateDto>> reconstruct(@PathVariable String transactionId) {
        InferredStateDto dto = reconstructionService.reconstructTransaction(transactionId);
        return ResponseEntity.ok(ApiResponse.ok("Deterministic reconstruction complete", dto));
    }

    @GetMapping("/reconstruction")
    public ResponseEntity<ApiResponse<InferredStateDto>> getLatestReconstruction(@PathVariable String transactionId) {
        InferredStateDto dto = reconstructionService.getLatestReconstruction(transactionId);
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @GetMapping("/transitions")
    public ResponseEntity<ApiResponse<List<StateTransitionDto>>> getTransitions(@PathVariable String transactionId) {
        List<StateTransitionDto> list = reconstructionService.getStateTransitions(transactionId);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }
}

