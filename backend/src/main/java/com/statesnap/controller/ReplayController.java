package com.statesnap.controller;

import com.statesnap.dto.*;
import com.statesnap.entity.User;
import com.statesnap.service.ReplayService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ReplayController {

    private final ReplayService replayService;

    public ReplayController(ReplayService replayService) {
        this.replayService = replayService;
    }

    @PostMapping("/transactions/{transactionId}/replay")
    public ResponseEntity<ApiResponse<ReplayExecutionDto>> executeReplay(
            @PathVariable String transactionId,
            @Valid @RequestBody ReplayScenarioRequest request,
            @AuthenticationPrincipal User user
    ) {
        ReplayExecutionDto dto = replayService.executeReplay(transactionId, request, user);
        return new ResponseEntity<>(ApiResponse.ok("Sandboxed fault-injection simulation completed", dto), HttpStatus.CREATED);
    }

    @GetMapping("/transactions/{transactionId}/replays")
    public ResponseEntity<ApiResponse<List<ReplayExecutionDto>>> listReplays(@PathVariable String transactionId) {
        List<ReplayExecutionDto> list = replayService.listReplaysForTransaction(transactionId);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/replay/{executionId}")
    public ResponseEntity<ApiResponse<ReplayExecutionDto>> getReplay(@PathVariable String executionId) {
        ReplayExecutionDto dto = replayService.getReplay(executionId);
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }
}

