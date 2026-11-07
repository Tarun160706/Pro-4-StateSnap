package com.statesnap.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.statesnap.dto.RecoveryAnalysisDto;
import com.statesnap.engine.RecoveryAdvisor;
import com.statesnap.entity.*;
import com.statesnap.exception.ResourceNotFoundException;
import com.statesnap.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class RecoveryService {

    private final TransactionRepository transactionRepository;
    private final TransactionEventRepository eventRepository;
    private final InferredTransactionStateRepository inferredStateRepository;
    private final EvidenceRecordRepository evidenceRecordRepository;
    private final RecoveryAnalysisRepository recoveryAnalysisRepository;
    private final RecoveryAdvisor recoveryAdvisor;
    private final ObjectMapper objectMapper;

    public RecoveryService(
            TransactionRepository transactionRepository,
            TransactionEventRepository eventRepository,
            InferredTransactionStateRepository inferredStateRepository,
            EvidenceRecordRepository evidenceRecordRepository,
            RecoveryAnalysisRepository recoveryAnalysisRepository,
            RecoveryAdvisor recoveryAdvisor,
            ObjectMapper objectMapper
    ) {
        this.transactionRepository = transactionRepository;
        this.eventRepository = eventRepository;
        this.inferredStateRepository = inferredStateRepository;
        this.evidenceRecordRepository = evidenceRecordRepository;
        this.recoveryAnalysisRepository = recoveryAnalysisRepository;
        this.recoveryAdvisor = recoveryAdvisor;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RecoveryAnalysisDto analyzeRecovery(String transactionId) {
        TransactionRecord transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + transactionId));

        InferredTransactionState currentState = inferredStateRepository
                .findFirstByTransactionIdOrderByReconstructedAtDesc(transactionId)
                .orElse(null);

        List<EvidenceRecord> evidenceList = evidenceRecordRepository
                .findByTransactionIdOrderByDetectedAtAsc(transactionId);

        List<TransactionEvent> events = eventRepository.findByTransactionIdOrdered(transactionId);

        RecoveryAnalysis analysis = recoveryAdvisor.analyze(transaction, currentState, evidenceList, events);

        // Delete previous recovery analysis for this transaction to maintain current audit
        recoveryAnalysisRepository.deleteByTransactionId(transactionId);
        analysis = recoveryAnalysisRepository.save(analysis);

        return mapToDto(analysis);
    }

    @Transactional(readOnly = true)
    public RecoveryAnalysisDto getLatestRecovery(String transactionId) {
        RecoveryAnalysis analysis = recoveryAnalysisRepository
                .findFirstByTransactionIdOrderByAnalyzedAtDesc(transactionId)
                .orElseGet(() -> {
                    // Auto-generate if not yet cached
                    return recoveryAdvisor.analyze(
                            transactionRepository.findById(transactionId).orElse(null),
                            inferredStateRepository.findFirstByTransactionIdOrderByReconstructedAtDesc(transactionId).orElse(null),
                            evidenceRecordRepository.findByTransactionIdOrderByDetectedAtAsc(transactionId),
                            eventRepository.findByTransactionIdOrdered(transactionId)
                    );
                });

        return mapToDto(analysis);
    }

    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public RecoveryAnalysisDto mapToDto(RecoveryAnalysis entity) {
        return RecoveryAnalysisDto.builder()
                .id(entity.getId())
                .suggestedAction(entity.getSuggestedAction())
                .safeToExecute(entity.getSafeToExecute())
                .rationale(entity.getRationale())
                .prerequisites(fromJson(entity.getPrerequisitesJson()))
                .missingPrerequisites(fromJson(entity.getMissingPrerequisitesJson()))
                .risks(fromJson(entity.getRisksJson()))
                .analyzedAt(entity.getAnalyzedAt())
                .build();
    }
}

