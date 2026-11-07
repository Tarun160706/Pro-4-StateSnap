package com.statesnap.controller;

import com.statesnap.dto.*;
import com.statesnap.entity.User;
import com.statesnap.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionDto>> createTransaction(
            @Valid @RequestBody CreateTransactionRequest request,
            @AuthenticationPrincipal User user
    ) {
        TransactionDto dto = transactionService.createTransaction(request, user);
        return new ResponseEntity<>(ApiResponse.ok("Transaction registered successfully", dto), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<TransactionDto>>> listTransactions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        PagedResponse<TransactionDto> paged = transactionService.listTransactions(status, search, page, size, sortBy, direction);
        return ResponseEntity.ok(ApiResponse.ok(paged));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionDto>> getTransaction(@PathVariable String id) {
        TransactionDto dto = transactionService.getTransaction(id);
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }
}

