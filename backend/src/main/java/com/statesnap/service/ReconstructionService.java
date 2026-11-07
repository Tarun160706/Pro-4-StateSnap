package com.statesnap.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.statesnap.dto.*;
import com.statesnap.engine.ReconstructionEngine;
import com.statesnap.engine.ReconstructionResult;
import com.statesnap.entity.*;
import com.statesnap.exception.ResourceNotFoundException;
import com.statesnap.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class ReconstructionService {

    private final TransactionRepository transactionRepository;
    private final TransactionEventRepository eventRepository;
    private final InferredTransactionStateRepository inferredStateRepository;
    private final EvidenceRecordRepository evidenceRecordRepository;
    private final StateTransitionRepository stateTransitionRepository;
    private final ReconstructionEngine reconstructionEngine;
    private final ObjectMapper objectMapper;

    public ReconstructionService(
            TransactionRepository transactionRepository,
            TransactionEventRepository eventRepository,
            InferredTransactionStateRepository inferredStateRepository,
            EvidenceRecordRepository evidenceRecordRepository,
            StateTransitionRepository stateTransitionRepository,
            ReconstructionEngine reconstructionEngine,
            ObjectMapper objectMapper
    ) {
        this.transactionRepository = transactionRepository;
        this.eventRepository = eventRepository;
        this.inferredStateRepository = inferredStateRepository;
        this.evidenceRecordRepository = evidenceRecordRepository;
        this.stateTransitionRepository = stateTransitionRepository;
        this.reconstructionEngine = reconstructionEngine;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public InferredStateDto reconstructTransaction(String transactionId) {
        TransactionRecord transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + transactionId));

        List<TransactionEvent> events = eventRepository.findByTransactionIdOrdered(transactionId);

        // Run deterministic reconstruction engine
        ReconstructionResult result = reconstructionEngine.reconstruct(transaction, events);

        // Clean previous evidence and transitions for fresh reconstruction
        evidenceRecordRepository.deleteByTransactionId(transactionId);
        stateTransitionRepository.deleteByTransactionId(transactionId);

        // Save evidence records
        if (result.getEvidenceRecords() != null && !result.getEvidenceRecords().isEmpty()) {
            evidenceRecordRepository.saveAll(result.getEvidenceRecords());
        }

        // Save state transitions
        if (result.getStateTransitions() != null && !result.getStateTransitions().isEmpty()) {
            stateTransitionRepository.saveAll(result.getStateTransitions());
        }

        // Persist new InferredTransactionState
        InferredTransactionState stateEntity = InferredTransactionState.builder()
                .id(UUID.randomUUID().toString())
                .transaction(transaction)
                .inferredState(result.getInferredState())
                .certaintyLevel(result.getCertaintyLevel())
                .confidenceScore(result.getConfidenceScore())
                .supportingEventsJson(toJson(result.getSupportingEventIds()))
                .contradictingEventsJson(toJson(result.getContradictingEventIds()))
                .missingEvidenceJson(toJson(result.getMissingEvidenceList()))
                .reasoningExplanation(result.getReasoningExplanation())
                .reconstructedAt(Instant.now())
                .build();

        stateEntity = inferredStateRepository.save(stateEntity);

        // Update transaction status
        transaction.setStatus(result.getInferredState() == TransactionStateEnum.CONFLICTED
                ? "CONFLICTED"
                : "RECONSTRUCTED");
        transaction.setUpdatedAt(Instant.now());
        transactionRepository.save(transaction);

        return mapToDto(stateEntity);
    }

    @Transactional(readOnly = true)
    public InferredStateDto getLatestReconstruction(String transactionId) {
        InferredTransactionState state = inferredStateRepository
                .findFirstByTransactionIdOrderByReconstructedAtDesc(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("No reconstruction found for transaction ID: " + transactionId));
        return mapToDto(state);
    }

    @Transactional(readOnly = true)
    public List<EvidenceRecordDto> getEvidenceRecords(String transactionId) {
        List<EvidenceRecord> list = evidenceRecordRepository.findByTransactionIdOrderByDetectedAtAsc(transactionId);
        return list.stream().map(this::mapEvidenceToDto).toList();
    }

    @Transactional(readOnly = true)
    public List<StateTransitionDto> getStateTransitions(String transactionId) {
        List<StateTransition> list = stateTransitionRepository.findByTransactionIdOrderByTimestampAsc(transactionId);
        return list.stream().map(this::mapTransitionToDto).toList();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public InferredStateDto mapToDto(InferredTransactionState entity) {
        return InferredStateDto.builder()
                .id(entity.getId())
                .inferredState(entity.getInferredState())
                .certaintyLevel(entity.getCertaintyLevel())
                .confidenceScore(entity.getConfidenceScore())
                .supportingEvents(fromJson(entity.getSupportingEventsJson()))
                .contradictingEvents(fromJson(entity.getContradictingEventsJson()))
                .missingEvidence(fromJson(entity.getMissingEvidenceJson()))
                .reasoningExplanation(entity.getReasoningExplanation())
                .reconstructedAt(entity.getReconstructedAt())
                .build();
    }

    public EvidenceRecordDto mapEvidenceToDto(EvidenceRecord entity) {
        return EvidenceRecordDto.builder()
                .id(entity.getId())
                .evidenceType(entity.getEvidenceType())
                .sourceEventId(entity.getSourceEventId())
                .description(entity.getDescription())
                .details(entity.getDetails())
                .confidenceWeight(entity.getConfidenceWeight())
                .detectedAt(entity.getDetectedAt())
                .build();
    }

    public StateTransitionDto mapTransitionToDto(StateTransition entity) {
        return StateTransitionDto.builder()
                .id(entity.getId())
                .fromState(entity.getFromState())
                .toState(entity.getToState())
                .triggerEventId(entity.getTriggerEventId())
                .transitionType(entity.getTransitionType())
                .timestamp(entity.getTimestamp())
                .notes(entity.getNotes())
                .build();
    }
}

