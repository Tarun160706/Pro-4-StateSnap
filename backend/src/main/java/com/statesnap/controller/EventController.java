package com.statesnap.controller;

import com.statesnap.dto.*;
import com.statesnap.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions/{transactionId}/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionEventDto>> ingestEvent(
            @PathVariable String transactionId,
            @Valid @RequestBody IngestEventRequest request
    ) {
        TransactionEventDto dto = eventService.ingestEvent(transactionId, request);
        return new ResponseEntity<>(ApiResponse.ok("Event ingested successfully", dto), HttpStatus.CREATED);
    }

    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<List<TransactionEventDto>>> batchIngest(
            @PathVariable String transactionId,
            @Valid @RequestBody BatchIngestRequest request
    ) {
        List<TransactionEventDto> dtos = eventService.batchIngestEvents(transactionId, request);
        return new ResponseEntity<>(ApiResponse.ok("Batch events ingested successfully", dtos), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TransactionEventDto>>> getEvents(@PathVariable String transactionId) {
        List<TransactionEventDto> list = eventService.getOrderedEvents(transactionId);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }
}

