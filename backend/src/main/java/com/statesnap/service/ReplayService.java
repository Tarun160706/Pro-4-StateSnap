package com.statesnap.service;

import com.statesnap.dto.*;
import com.statesnap.engine.ReplayEngine;
import com.statesnap.engine.ReplaySimulationResult;
import com.statesnap.entity.*;
import com.statesnap.exception.ResourceNotFoundException;
import com.statesnap.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ReplayService {

    private final TransactionRepository transactionRepository;
    private final TransactionEventRepository eventRepository;
    private final ReplayExecutionRepository replayExecutionRepository;
    private final ReplayEventRepository replayEventRepository;
    private final ReplayEngine replayEngine;

    public ReplayService(
            TransactionRepository transactionRepository,
            TransactionEventRepository eventRepository,
            ReplayExecutionRepository replayExecutionRepository,
            ReplayEventRepository replayEventRepository,
            ReplayEngine replayEngine
    ) {
        this.transactionRepository = transactionRepository;
        this.eventRepository = eventRepository;
        this.replayExecutionRepository = replayExecutionRepository;
        this.replayEventRepository = replayEventRepository;
        this.replayEngine = replayEngine;
    }

    @Transactional
    public ReplayExecutionDto executeReplay(String transactionId, ReplayScenarioRequest request, User user) {
        TransactionRecord transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + transactionId));

        List<TransactionEvent> events = eventRepository.findByTransactionIdOrdered(transactionId);

        // Run isolated sandbox simulation
        ReplaySimulationResult simResult = replayEngine.simulate(
                transaction,
                events,
                request.getScenarioType(),
                request.getTargetEventId(),
                request.getDelayMillis(),
                request.getConflictingPayload()
        );

        // Create ReplayExecution record
        ReplayExecution execution = ReplayExecution.builder()
                .id(UUID.randomUUID().toString())
                .originalTransactionId(transactionId)
                .executedBy(user)
                .scenarioType(request.getScenarioType())
                .scenarioDescription(simResult.getScenarioDescription())
                .targetEventId(request.getTargetEventId())
                .delayMillis(request.getDelayMillis())
                .baselineState(simResult.getBaselineState())
                .baselineCertainty(simResult.getBaselineCertainty())
                .simulatedState(simResult.getSimulatedState())
                .simulatedCertainty(simResult.getSimulatedCertainty())
                .simulatedConfidenceScore(simResult.getSimulatedConfidenceScore())
                .findingsJson(simResult.getFindings())
                .differencesSummary(simResult.getDifferencesSummary())
                .status(ReplayStatus.COMPLETED)
                .isSandboxed(true)
                .createdAt(Instant.now())
                .build();

        execution = replayExecutionRepository.save(execution);

        // Save isolated simulated replay events
        for (ReplayEvent re : simResult.getSimulatedEvents()) {
            re.setReplayExecution(execution);
        }
        replayEventRepository.saveAll(simResult.getSimulatedEvents());

        return mapToDto(execution, simResult.getSimulatedEvents());
    }

    @Transactional(readOnly = true)
    public ReplayExecutionDto getReplay(String executionId) {
        ReplayExecution execution = replayExecutionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("Replay execution not found with ID: " + executionId));
        List<ReplayEvent> events = replayEventRepository.findByReplayExecutionIdOrderBySimulatedSequenceNumberAscSimulatedTimestampAsc(executionId);
        return mapToDto(execution, events);
    }

    @Transactional(readOnly = true)
    public List<ReplayExecutionDto> listReplaysForTransaction(String transactionId) {
        List<ReplayExecution> list = replayExecutionRepository.findByOriginalTransactionIdOrderByCreatedAtDesc(transactionId);
        return list.stream().map(e -> {
            List<ReplayEvent> events = replayEventRepository.findByReplayExecutionIdOrderBySimulatedSequenceNumberAscSimulatedTimestampAsc(e.getId());
            return mapToDto(e, events);
        }).toList();
    }

    public ReplayExecutionDto mapToDto(ReplayExecution entity, List<ReplayEvent> events) {
        List<ReplayEventDto> eventDtos = events != null ? events.stream().map(this::mapReplayEventToDto).toList() : List.of();
        return ReplayExecutionDto.builder()
                .id(entity.getId())
                .originalTransactionId(entity.getOriginalTransactionId())
                .scenarioType(entity.getScenarioType())
                .scenarioDescription(entity.getScenarioDescription())
                .targetEventId(entity.getTargetEventId())
                .delayMillis(entity.getDelayMillis())
                .baselineState(entity.getBaselineState())
                .baselineCertainty(entity.getBaselineCertainty())
                .simulatedState(entity.getSimulatedState())
                .simulatedCertainty(entity.getSimulatedCertainty())
                .simulatedConfidenceScore(entity.getSimulatedConfidenceScore())
                .findings(entity.getFindingsJson())
                .differencesSummary(entity.getDifferencesSummary())
                .status(entity.getStatus())
                .isSandboxed(entity.getIsSandboxed())
                .createdAt(entity.getCreatedAt())
                .simulatedEvents(eventDtos)
                .build();
    }

    public ReplayEventDto mapReplayEventToDto(ReplayEvent entity) {
        return ReplayEventDto.builder()
                .id(entity.getId())
                .originalEventId(entity.getOriginalEventId())
                .eventType(entity.getEventType())
                .sourceSystem(entity.getSourceSystem())
                .simulatedTimestamp(entity.getSimulatedTimestamp())
                .simulatedSequenceNumber(entity.getSimulatedSequenceNumber())
                .rawPayload(entity.getRawPayload())
                .mutationType(entity.getMutationType())
                .build();
    }
}

