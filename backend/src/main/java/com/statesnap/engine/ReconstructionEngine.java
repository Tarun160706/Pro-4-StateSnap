package com.statesnap.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.statesnap.entity.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
public class ReconstructionEngine {

    private final ObjectMapper objectMapper;

    public ReconstructionEngine(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Deterministically reconstructs transaction state from a list of raw transaction events.
     * Raw event records are NEVER modified.
     */
    public ReconstructionResult reconstruct(TransactionRecord transaction, List<TransactionEvent> rawEvents) {
        List<EvidenceRecord> generatedEvidence = new ArrayList<>();
        List<StateTransition> generatedTransitions = new ArrayList<>();
        List<String> supportingEventIds = new ArrayList<>();
        List<String> contradictingEventIds = new ArrayList<>();
        List<String> missingEvidenceList = new ArrayList<>();
        StringBuilder reasoning = new StringBuilder();

        if (rawEvents == null || rawEvents.isEmpty()) {
            return ReconstructionResult.builder()
                    .inferredState(TransactionStateEnum.UNKNOWN)
                    .certaintyLevel(CertaintyLevel.HIGH_UNCERTAINTY)
                    .confidenceScore(0.0)
                    .supportingEventIds(Collections.emptyList())
                    .contradictingEventIds(Collections.emptyList())
                    .missingEvidenceList(List.of("No transaction events ingested"))
                    .reasoningExplanation("Reconstruction impossible: Zero events recorded for transaction " +
                            (transaction != null ? transaction.getExternalId() : "N/A") + ".")
                    .evidenceRecords(Collections.emptyList())
                    .stateTransitions(Collections.emptyList())
                    .build();
        }

        // 1. Structural Validation & Verified Facts extraction
        for (TransactionEvent evt : rawEvents) {
            boolean structurallyValid = validateEventStructure(evt);
            if (structurallyValid) {
                generatedEvidence.add(EvidenceRecord.builder()
                        .id(UUID.randomUUID().toString())
                        .transaction(transaction)
                        .evidenceType(EvidenceType.VERIFIED_FACT)
                        .sourceEventId(evt.getEventId())
                        .description("Verified log: " + evt.getEventType() + " from source [" + evt.getSourceSystem() + "]")
                        .details("Timestamp: " + evt.getTimestamp() + ", Sequence: " + (evt.getSequenceNumber() != null ? evt.getSequenceNumber() : "N/A"))
                        .confidenceWeight(1.0)
                        .detectedAt(Instant.now())
                        .build());
            } else {
                generatedEvidence.add(EvidenceRecord.builder()
                        .id(UUID.randomUUID().toString())
                        .transaction(transaction)
                        .evidenceType(EvidenceType.OUT_OF_ORDER_EVIDENCE)
                        .sourceEventId(evt.getEventId())
                        .description("Malformed or unparseable event payload detected: " + evt.getEventId())
                        .details(evt.getRawPayload())
                        .confidenceWeight(0.5)
                        .detectedAt(Instant.now())
                        .build());
            }
        }

        // 2. Deterministic Ordering
        // Use reliable sequenceNumber first (if present), followed by timestamp
        List<TransactionEvent> sortedEvents = new ArrayList<>(rawEvents);
        sortedEvents.sort((e1, e2) -> {
            if (e1.getSequenceNumber() != null && e2.getSequenceNumber() != null) {
                int seqComp = e1.getSequenceNumber().compareTo(e2.getSequenceNumber());
                if (seqComp != 0) return seqComp;
            }
            return e1.getTimestamp().compareTo(e2.getTimestamp());
        });

        // 3. Duplicate Detection
        Map<String, List<TransactionEvent>> eventsByType = new HashMap<>();
        Set<String> seenEventIds = new HashSet<>();
        List<TransactionEvent> duplicateEvents = new ArrayList<>();

        for (TransactionEvent evt : sortedEvents) {
            eventsByType.computeIfAbsent(evt.getEventType().toUpperCase(), k -> new ArrayList<>()).add(evt);
            if (!seenEventIds.add(evt.getEventId())) {
                duplicateEvents.add(evt);
                generatedEvidence.add(EvidenceRecord.builder()
                        .id(UUID.randomUUID().toString())
                        .transaction(transaction)
                        .evidenceType(EvidenceType.DUPLICATE_SIGNAL)
                        .sourceEventId(evt.getEventId())
                        .description("Duplicate event ID ingested: " + evt.getEventId() + " (" + evt.getEventType() + ")")
                        .details("Source system: " + evt.getSourceSystem() + ", Timestamp: " + evt.getTimestamp())
                        .confidenceWeight(0.8)
                        .detectedAt(Instant.now())
                        .build());
            }
        }

        // Check for duplicate terminal steps (e.g. multiple distinct AUTH_APPROVED or CAPTURE_COMPLETED)
        for (Map.Entry<String, List<TransactionEvent>> entry : eventsByType.entrySet()) {
            if (entry.getValue().size() > 1 && isTerminalEventType(entry.getKey())) {
                generatedEvidence.add(EvidenceRecord.builder()
                        .id(UUID.randomUUID().toString())
                        .transaction(transaction)
                        .evidenceType(EvidenceType.DUPLICATE_SIGNAL)
                        .sourceEventId(entry.getValue().get(1).getEventId())
                        .description("Multiple terminal events of type " + entry.getKey() + " received (" + entry.getValue().size() + " occurrences)")
                        .details("Potential retry storm, idempotency breach, or concurrent execution detected.")
                        .confidenceWeight(0.7)
                        .detectedAt(Instant.now())
                        .build());
            }
        }

        // 4. Out-of-Order / Clock Skew Detection
        for (int i = 0; i < sortedEvents.size() - 1; i++) {
            TransactionEvent curr = sortedEvents.get(i);
            TransactionEvent next = sortedEvents.get(i + 1);

            // If sequence numbers are ordered but timestamps jump backwards significantly (> 1 second)
            if (curr.getSequenceNumber() != null && next.getSequenceNumber() != null &&
                    curr.getSequenceNumber() < next.getSequenceNumber() &&
                    curr.getTimestamp().isAfter(next.getTimestamp().plusMillis(1000))) {
                generatedEvidence.add(EvidenceRecord.builder()
                        .id(UUID.randomUUID().toString())
                        .transaction(transaction)
                        .evidenceType(EvidenceType.OUT_OF_ORDER_EVIDENCE)
                        .sourceEventId(next.getEventId())
                        .description("Clock skew detected between sequence " + curr.getSequenceNumber() + " and " + next.getSequenceNumber())
                        .details("Event " + curr.getEventId() + " (" + curr.getTimestamp() + ") logged later than sequence successor " +
                                next.getEventId() + " (" + next.getTimestamp() + ")")
                        .confidenceWeight(0.7)
                        .detectedAt(Instant.now())
                        .build());
            }
        }

        // 5. Causal Step & Missing Prerequisite Analysis
        boolean hasInitiated = eventsByType.containsKey("PAYMENT_INITIATED") ||
                eventsByType.containsKey("ORDER_CREATED") ||
                eventsByType.containsKey("TRANSACTION_INITIATED");

        boolean hasAuthRequested = eventsByType.containsKey("AUTH_REQUESTED") ||
                eventsByType.containsKey("PAYMENT_AUTH_STARTED");

        boolean hasAuthApproved = eventsByType.containsKey("AUTH_APPROVED") ||
                eventsByType.containsKey("PAYMENT_AUTH_SUCCESS");

        boolean hasAuthFailed = eventsByType.containsKey("AUTH_FAILED") ||
                eventsByType.containsKey("PAYMENT_AUTH_FAILED") ||
                eventsByType.containsKey("INSUFFICIENT_FUNDS") ||
                eventsByType.containsKey("FRAUD_CHECK_REJECTED");

        boolean hasCaptureTriggered = eventsByType.containsKey("CAPTURE_TRIGGERED") ||
                eventsByType.containsKey("CAPTURE_REQUESTED");

        boolean hasCaptureCompleted = eventsByType.containsKey("CAPTURE_COMPLETED") ||
                eventsByType.containsKey("LEDGER_COMMITTED") ||
                eventsByType.containsKey("PAYMENT_SUCCESS") ||
                eventsByType.containsKey("SETTLEMENT_POSTED");

        boolean hasExplicitFailure = eventsByType.containsKey("TRANSACTION_FAILED") ||
                eventsByType.containsKey("CAPTURE_FAILED") ||
                eventsByType.containsKey("TIMEOUT_EXPIRED") ||
                eventsByType.containsKey("GATEWAY_TIMEOUT");

        boolean hasRollback = eventsByType.containsKey("ROLLBACK_TRIGGERED") ||
                eventsByType.containsKey("ROLLBACK_COMPLETED") ||
                eventsByType.containsKey("REFUND_SUCCESS") ||
                eventsByType.containsKey("COMPENSATING_REFUND_EXECUTED");

        // Missing step checks
        if (hasCaptureCompleted && !hasAuthApproved) {
            missingEvidenceList.add("Missing prerequisite: AUTH_APPROVED prior to CAPTURE_COMPLETED");
            generatedEvidence.add(EvidenceRecord.builder()
                    .id(UUID.randomUUID().toString())
                    .transaction(transaction)
                    .evidenceType(EvidenceType.MISSING_STEP)
                    .description("Capture succeeded but authorization approval record is missing from logs.")
                    .details("Downstream capture logged without upstream authorization proof.")
                    .confidenceWeight(0.6)
                    .detectedAt(Instant.now())
                    .build());
        }

        if (hasAuthRequested && !hasAuthApproved && !hasAuthFailed) {
            missingEvidenceList.add("Unresolved authorization response: AUTH_REQUESTED has neither APPROVED nor FAILED counterpart");
            generatedEvidence.add(EvidenceRecord.builder()
                    .id(UUID.randomUUID().toString())
                    .transaction(transaction)
                    .evidenceType(EvidenceType.MISSING_STEP)
                    .description("Authorization requested but no terminal authorization outcome was logged.")
                    .details("Possible gateway network timeout or dropped callback.")
                    .confidenceWeight(0.5)
                    .detectedAt(Instant.now())
                    .build());
        }

        if (hasInitiated && !hasAuthRequested && !hasCaptureCompleted && !hasExplicitFailure) {
            missingEvidenceList.add("Stalled transaction: Initiated without subsequent processing pipeline events");
            generatedEvidence.add(EvidenceRecord.builder()
                    .id(UUID.randomUUID().toString())
                    .transaction(transaction)
                    .evidenceType(EvidenceType.MISSING_STEP)
                    .description("Transaction initiated but pipeline halted before authorization stage.")
                    .confidenceWeight(0.4)
                    .detectedAt(Instant.now())
                    .build());
        }

        // 6. Conflicting Event Sequences Detection
        boolean conflictDetected = false;
        if (hasCaptureCompleted && (hasAuthFailed || hasExplicitFailure)) {
            conflictDetected = true;
            for (TransactionEvent failEvt : getMatchingEvents(eventsByType, "AUTH_FAILED", "PAYMENT_AUTH_FAILED", "TRANSACTION_FAILED", "CAPTURE_FAILED")) {
                contradictingEventIds.add(failEvt.getEventId());
            }
            for (TransactionEvent succEvt : getMatchingEvents(eventsByType, "CAPTURE_COMPLETED", "LEDGER_COMMITTED", "PAYMENT_SUCCESS")) {
                supportingEventIds.add(succEvt.getEventId());
            }
            generatedEvidence.add(EvidenceRecord.builder()
                    .id(UUID.randomUUID().toString())
                    .transaction(transaction)
                    .evidenceType(EvidenceType.CONFLICTING_SIGNAL)
                    .description("Severe Conflict: Dual contradictory outcomes. Logs contain both COMMITTED confirmation and FAILED confirmation.")
                    .details("Ledger/Capture indicates funds captured, while Gateway/Auth claims failure.")
                    .confidenceWeight(0.9)
                    .detectedAt(Instant.now())
                    .build());
        }

        if (hasAuthApproved && hasAuthFailed) {
            conflictDetected = true;
            for (TransactionEvent f : getMatchingEvents(eventsByType, "AUTH_FAILED", "PAYMENT_AUTH_FAILED")) {
                contradictingEventIds.add(f.getEventId());
            }
            for (TransactionEvent a : getMatchingEvents(eventsByType, "AUTH_APPROVED", "PAYMENT_AUTH_SUCCESS")) {
                supportingEventIds.add(a.getEventId());
            }
            generatedEvidence.add(EvidenceRecord.builder()
                    .id(UUID.randomUUID().toString())
                    .transaction(transaction)
                    .evidenceType(EvidenceType.CONFLICTING_SIGNAL)
                    .description("Conflict: Both AUTH_APPROVED and AUTH_FAILED were recorded.")
                    .details("Potential multi-source divergence between acquirer response and internal handler.")
                    .confidenceWeight(0.85)
                    .detectedAt(Instant.now())
                    .build());
        }

        // 7. State Inference & Transitions Replay
        TransactionStateEnum currentState = TransactionStateEnum.UNKNOWN;
        String prevState = "INITIAL";

        for (TransactionEvent evt : sortedEvents) {
            String type = evt.getEventType().toUpperCase();
            String nextState = prevState;
            TransitionType transType = TransitionType.DETERMINISTIC;

            if (type.contains("INITIATED") || type.contains("CREATED")) {
                nextState = "INITIATED";
                currentState = TransactionStateEnum.INITIATED;
                supportingEventIds.add(evt.getEventId());
            } else if (type.contains("AUTH_REQUESTED") || type.contains("PENDING")) {
                nextState = "PROCESSING";
                currentState = TransactionStateEnum.PROCESSING;
                supportingEventIds.add(evt.getEventId());
            } else if (type.contains("AUTH_APPROVED") || type.contains("RESERVATION_CONFIRMED")) {
                nextState = "PROCESSING";
                currentState = TransactionStateEnum.PROCESSING;
                supportingEventIds.add(evt.getEventId());
            } else if (type.contains("CAPTURE_COMPLETED") || type.contains("COMMITTED") || type.contains("SETTLEMENT_POSTED")) {
                nextState = "COMMITTED";
                currentState = TransactionStateEnum.COMMITTED;
                supportingEventIds.add(evt.getEventId());
            } else if (type.contains("FAILED") || type.contains("TIMEOUT") || type.contains("REJECTED")) {
                nextState = "FAILED";
                currentState = TransactionStateEnum.FAILED;
                supportingEventIds.add(evt.getEventId());
            } else if (type.contains("ROLLBACK") || type.contains("REFUND")) {
                nextState = "ROLLED_BACK";
                currentState = TransactionStateEnum.ROLLED_BACK;
                supportingEventIds.add(evt.getEventId());
            }

            if (!nextState.equals(prevState)) {
                generatedTransitions.add(StateTransition.builder()
                        .id(UUID.randomUUID().toString())
                        .transaction(transaction)
                        .fromState(prevState)
                        .toState(nextState)
                        .triggerEventId(evt.getEventId())
                        .transitionType(transType)
                        .timestamp(evt.getTimestamp())
                        .notes("Transition triggered by " + evt.getEventType() + " from " + evt.getSourceSystem())
                        .build());
                prevState = nextState;
            }
        }

        // Override inferred state if irreconcilable contradiction exists
        if (conflictDetected) {
            currentState = TransactionStateEnum.CONFLICTED;
        }

        // 8. Defensible Confidence Score Calculation
        // Base score starts at 100.0%
        // Deductions are made for verified anomalies, missing signals, conflicts, and clock skew
        double confidence = 100.0;
        int conflictCount = (int) generatedEvidence.stream()
                .filter(e -> e.getEvidenceType() == EvidenceType.CONFLICTING_SIGNAL).count();
        int missingCount = missingEvidenceList.size();
        int outOfOrderCount = (int) generatedEvidence.stream()
                .filter(e -> e.getEvidenceType() == EvidenceType.OUT_OF_ORDER_EVIDENCE).count();
        int duplicateCount = duplicateEvents.size();

        if (conflictCount > 0) {
            confidence -= (conflictCount * 45.0);
        }
        if (missingCount > 0) {
            confidence -= (missingCount * 22.0);
        }
        if (outOfOrderCount > 0) {
            confidence -= (outOfOrderCount * 12.0);
        }
        if (duplicateCount > 0) {
            confidence -= (duplicateCount * 8.0);
        }
        if (currentState == TransactionStateEnum.INITIATED || currentState == TransactionStateEnum.PROCESSING) {
            // Pipeline unfinished: uncertainty penalty
            confidence -= 25.0;
        }

        confidence = Math.max(8.0, Math.min(99.0, confidence));

        // Determine Certainty Level
        CertaintyLevel certaintyLevel;
        if (confidence >= 85.0 && conflictCount == 0 && missingCount == 0) {
            certaintyLevel = CertaintyLevel.HIGH_CERTAINTY;
        } else if (confidence >= 65.0 && conflictCount == 0) {
            certaintyLevel = CertaintyLevel.CONDITIONAL_CERTAINTY;
        } else if (conflictCount > 0) {
            certaintyLevel = CertaintyLevel.HIGH_UNCERTAINTY;
        } else {
            certaintyLevel = CertaintyLevel.AMBIGUOUS;
        }

        // 9. Explainable Reasoning Narrative
        reasoning.append("### State Reconstruction Assessment\n\n");
        reasoning.append("**Inferred State**: `").append(currentState.name()).append("` | ");
        reasoning.append("**Certainty**: `").append(certaintyLevel.name()).append("` (Defensible Confidence: ")
                .append(String.format(Locale.US, "%.1f", confidence)).append("%)\n\n");

        reasoning.append("#### Deterministic Evidence Summary\n");
        reasoning.append("- **Total Ingested Events**: ").append(rawEvents.size()).append(" verified log records.\n");
        reasoning.append("- **Chronological Sequence**: Verified timeline sorted across ")
                .append(sortedEvents.size()).append(" discrete system signals.\n");

        if (conflictCount > 0) {
            reasoning.append("- ⚠️ **Contradictory Signals**: Detected ").append(conflictCount)
                    .append(" direct conflicts in the log trace. Conflicting events: ")
                    .append(String.join(", ", contradictingEventIds)).append(".\n");
        } else {
            reasoning.append("- **Signal Coherence**: No direct contradictory terminal signals detected.\n");
        }

        if (missingCount > 0) {
            reasoning.append("- ⚠️ **Missing Prerequisites**: ").append(missingCount).append(" gap(s) identified:\n");
            for (String gap : missingEvidenceList) {
                reasoning.append("  - ").append(gap).append("\n");
            }
        } else {
            reasoning.append("- **Completeness**: All required lifecycle milestones are present in sequence.\n");
        }

        if (duplicateCount > 0) {
            reasoning.append("- ℹ️ **Duplicate Ingestions**: Detected ").append(duplicateCount).append(" duplicate/redundant event ID(s).\n");
        }

        reasoning.append("\n#### Forensic Deduction\n");
        switch (currentState) {
            case COMMITTED -> reasoning.append("The transaction is inferred to be COMMITTED because a terminal success signal (such as `CAPTURE_COMPLETED` or `LEDGER_COMMITTED`) was verified without uncompensated reversals or contradictory decline signals.");
            case FAILED -> reasoning.append("The transaction is inferred to be FAILED because an explicit terminal failure log (e.g. `AUTH_FAILED`, `TIMEOUT_EXPIRED`, or `CAPTURE_FAILED`) was recorded, and no subsequent capture or settlement was authenticated.");
            case ROLLED_BACK -> reasoning.append("The transaction is inferred to be ROLLED_BACK because a compensatory rollback or refund event was explicitly recorded and settled after authorization.");
            case CONFLICTED -> reasoning.append("The transaction is marked CONFLICTED due to irreconcilable cross-system log divergence (e.g. simultaneous commit and decline signals). Manual investigation is strictly required.");
            case PROCESSING -> reasoning.append("The transaction is currently PROCESSING. Intermediate events were verified, but neither a final commit nor terminal failure signal has been received yet.");
            case INITIATED -> reasoning.append("The transaction is in INITIATED state only. The lifecycle began, but no authorization or processing logs have arrived yet.");
            default -> reasoning.append("Transaction state remains UNKNOWN due to insufficient log telemetry.");
        }

        return ReconstructionResult.builder()
                .inferredState(currentState)
                .certaintyLevel(certaintyLevel)
                .confidenceScore(confidence)
                .supportingEventIds(new ArrayList<>(new LinkedHashSet<>(supportingEventIds)))
                .contradictingEventIds(new ArrayList<>(new LinkedHashSet<>(contradictingEventIds)))
                .missingEvidenceList(missingEvidenceList)
                .reasoningExplanation(reasoning.toString())
                .evidenceRecords(generatedEvidence)
                .stateTransitions(generatedTransitions)
                .build();
    }

    private boolean validateEventStructure(TransactionEvent evt) {
        if (evt.getEventId() == null || evt.getEventId().isBlank()) return false;
        if (evt.getEventType() == null || evt.getEventType().isBlank()) return false;
        if (evt.getSourceSystem() == null || evt.getSourceSystem().isBlank()) return false;
        if (evt.getTimestamp() == null) return false;
        if (evt.getRawPayload() == null || evt.getRawPayload().isBlank()) return false;
        try {
            // Validate that rawPayload is at least syntactically valid JSON
            objectMapper.readTree(evt.getRawPayload());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTerminalEventType(String type) {
        return type.contains("COMMITTED") ||
                type.contains("CAPTURE_COMPLETED") ||
                type.contains("SUCCESS") ||
                type.contains("FAILED") ||
                type.contains("ROLLED_BACK");
    }

    private List<TransactionEvent> getMatchingEvents(Map<String, List<TransactionEvent>> map, String... types) {
        List<TransactionEvent> matches = new ArrayList<>();
        for (String type : types) {
            List<TransactionEvent> evts = map.get(type);
            if (evts != null) {
                matches.addAll(evts);
            }
        }
        return matches;
    }
}

