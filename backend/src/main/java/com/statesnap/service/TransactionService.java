package com.statesnap.service;

import com.statesnap.dto.*;
import com.statesnap.entity.EvidenceType;
import com.statesnap.entity.InferredTransactionState;
import com.statesnap.entity.TransactionRecord;
import com.statesnap.entity.User;
import com.statesnap.exception.ConflictException;
import com.statesnap.exception.ResourceNotFoundException;
import com.statesnap.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionEventRepository eventRepository;
    private final InferredTransactionStateRepository inferredStateRepository;
    private final EvidenceRecordRepository evidenceRecordRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            TransactionEventRepository eventRepository,
            InferredTransactionStateRepository inferredStateRepository,
            EvidenceRecordRepository evidenceRecordRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.eventRepository = eventRepository;
        this.inferredStateRepository = inferredStateRepository;
        this.evidenceRecordRepository = evidenceRecordRepository;
    }

    @Transactional
    public TransactionDto createTransaction(CreateTransactionRequest request, User investigator) {
        if (transactionRepository.findByExternalId(request.getExternalId()).isPresent()) {
            throw new ConflictException("Transaction with external ID '" + request.getExternalId() + "' already exists");
        }

        TransactionRecord record = TransactionRecord.builder()
                .id(UUID.randomUUID().toString())
                .externalId(request.getExternalId().trim())
                .title(request.getTitle().trim())
                .channel(request.getChannel() != null ? request.getChannel().trim() : "DEFAULT")
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency().trim().toUpperCase() : "USD")
                .status("INVESTIGATION_PENDING")
                .metadataJson(request.getMetadataJson())
                .investigator(investigator)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        record = transactionRepository.save(record);
        return mapToDto(record);
    }

    @Transactional(readOnly = true)
    public TransactionRecord getTransactionEntity(String id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public TransactionDto getTransaction(String id) {
        TransactionRecord record = getTransactionEntity(id);
        return mapToDto(record);
    }

    @Transactional(readOnly = true)
    public PagedResponse<TransactionDto> listTransactions(
            String status,
            String search,
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        Sort sort = Sort.by(direction != null && direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC,
                sortBy != null ? sortBy : "createdAt");
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sort);

        Page<TransactionRecord> paged;
        boolean hasStatus = status != null && !status.isBlank();
        boolean hasSearch = search != null && !search.isBlank();

        if (hasStatus && hasSearch) {
            paged = transactionRepository.searchByStatusAndKeyword(status.trim(), search.trim(), pageable);
        } else if (hasStatus) {
            paged = transactionRepository.findByStatus(status.trim(), pageable);
        } else if (hasSearch) {
            paged = transactionRepository.searchByKeyword(search.trim(), pageable);
        } else {
            paged = transactionRepository.findAll(pageable);
        }

        return PagedResponse.<TransactionDto>builder()
                .content(paged.getContent().stream().map(this::mapToDto).toList())
                .page(paged.getNumber())
                .size(paged.getSize())
                .totalElements(paged.getTotalElements())
                .totalPages(paged.getTotalPages())
                .last(paged.isLast())
                .build();
    }

    public TransactionDto mapToDto(TransactionRecord entity) {
        long eventCount = eventRepository.countByTransactionId(entity.getId());
        Optional<InferredTransactionState> latestState = inferredStateRepository
                .findFirstByTransactionIdOrderByReconstructedAtDesc(entity.getId());

        List<com.statesnap.entity.EvidenceRecord> evidenceList = evidenceRecordRepository
                .findByTransactionIdOrderByDetectedAtAsc(entity.getId());

        boolean hasConflict = evidenceList.stream()
                .anyMatch(e -> e.getEvidenceType() == EvidenceType.CONFLICTING_SIGNAL);
        boolean hasMissing = evidenceList.stream()
                .anyMatch(e -> e.getEvidenceType() == EvidenceType.MISSING_STEP);
        boolean hasAnomaly = evidenceList.stream()
                .anyMatch(e -> e.getEvidenceType() == EvidenceType.OUT_OF_ORDER_EVIDENCE || e.getEvidenceType() == EvidenceType.DUPLICATE_SIGNAL);

        String evidenceQuality;
        if (eventCount == 0) {
            evidenceQuality = "NO_DATA";
        } else if (hasConflict) {
            evidenceQuality = "CONTRADICTORY";
        } else if (hasMissing) {
            evidenceQuality = "INCOMPLETE";
        } else if (hasAnomaly) {
            evidenceQuality = "ANOMALOUS";
        } else {
            evidenceQuality = "VERIFIED";
        }

        int unresolvedIssues = (int) evidenceList.stream()
                .filter(e -> e.getEvidenceType() != EvidenceType.VERIFIED_FACT)
                .count();

        return TransactionDto.builder()
                .id(entity.getId())
                .externalId(entity.getExternalId())
                .title(entity.getTitle())
                .channel(entity.getChannel())
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .status(entity.getStatus())
                .metadataJson(entity.getMetadataJson())
                .eventCount(eventCount)
                .latestState(latestState.map(InferredTransactionState::getInferredState).orElse(null))
                .latestCertainty(latestState.map(InferredTransactionState::getCertaintyLevel).orElse(null))
                .latestConfidenceScore(latestState.map(InferredTransactionState::getConfidenceScore).orElse(null))
                .hasConflict(hasConflict)
                .evidenceQuality(evidenceQuality)
                .unresolvedIssuesCount(unresolvedIssues)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .investigatorEmail(entity.getInvestigator() != null ? entity.getInvestigator().getEmail() : null)
                .build();
    }
}
