package com.statesnap.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.statesnap.dto.*;
import com.statesnap.entity.EventIntegrityStatus;
import com.statesnap.entity.TransactionEvent;
import com.statesnap.entity.TransactionRecord;
import com.statesnap.exception.BadRequestException;
import com.statesnap.repository.TransactionEventRepository;
import com.statesnap.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class EventService {

    private final TransactionRepository transactionRepository;
    private final TransactionEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    public EventService(
            TransactionRepository transactionRepository,
            TransactionEventRepository eventRepository,
            ObjectMapper objectMapper
    ) {
        this.transactionRepository = transactionRepository;
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TransactionEventDto ingestEvent(String transactionId, IngestEventRequest request) {
        TransactionRecord transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new BadRequestException("Transaction not found with ID: " + transactionId));

        EventIntegrityStatus integrity = validatePayloadIntegrity(request.getRawPayload());

        TransactionEvent event = TransactionEvent.builder()
                .id(UUID.randomUUID().toString())
                .transaction(transaction)
                .eventId(request.getEventId().trim())
                .eventType(request.getEventType().trim().toUpperCase())
                .sourceSystem(request.getSourceSystem().trim())
                .timestamp(request.getTimestamp())
                .sequenceNumber(request.getSequenceNumber())
                .rawPayload(request.getRawPayload())
                .ingestionTimestamp(Instant.now())
                .integrityStatus(integrity)
                .build();

        event = eventRepository.save(event);

        transaction.setUpdatedAt(Instant.now());
        transactionRepository.save(transaction);

        return mapToDto(event);
    }

    @Transactional
    public List<TransactionEventDto> batchIngestEvents(String transactionId, BatchIngestRequest request) {
        TransactionRecord transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new BadRequestException("Transaction not found with ID: " + transactionId));

        List<TransactionEventDto> ingested = new ArrayList<>();
        for (IngestEventRequest req : request.getEvents()) {
            EventIntegrityStatus integrity = validatePayloadIntegrity(req.getRawPayload());

            TransactionEvent event = TransactionEvent.builder()
                    .id(UUID.randomUUID().toString())
                    .transaction(transaction)
                    .eventId(req.getEventId().trim())
                    .eventType(req.getEventType().trim().toUpperCase())
                    .sourceSystem(req.getSourceSystem().trim())
                    .timestamp(req.getTimestamp())
                    .sequenceNumber(req.getSequenceNumber())
                    .rawPayload(req.getRawPayload())
                    .ingestionTimestamp(Instant.now())
                    .integrityStatus(integrity)
                    .build();

            event = eventRepository.save(event);
            ingested.add(mapToDto(event));
        }

        transaction.setUpdatedAt(Instant.now());
        transactionRepository.save(transaction);

        return ingested;
    }

    @Transactional(readOnly = true)
    public List<TransactionEventDto> getOrderedEvents(String transactionId) {
        List<TransactionEvent> events = eventRepository.findByTransactionIdOrdered(transactionId);
        return events.stream().map(this::mapToDto).toList();
    }

    private EventIntegrityStatus validatePayloadIntegrity(String rawPayload) {
        if (rawPayload == null || rawPayload.isBlank()) {
            return EventIntegrityStatus.MALFORMED;
        }
        try {
            objectMapper.readTree(rawPayload);
            return EventIntegrityStatus.VALID;
        } catch (Exception e) {
            return EventIntegrityStatus.MALFORMED;
        }
    }

    public TransactionEventDto mapToDto(TransactionEvent entity) {
        return TransactionEventDto.builder()
                .id(entity.getId())
                .eventId(entity.getEventId())
                .eventType(entity.getEventType())
                .sourceSystem(entity.getSourceSystem())
                .timestamp(entity.getTimestamp())
                .sequenceNumber(entity.getSequenceNumber())
                .rawPayload(entity.getRawPayload())
                .ingestionTimestamp(entity.getIngestionTimestamp())
                .integrityStatus(entity.getIntegrityStatus())
                .build();
    }
}

