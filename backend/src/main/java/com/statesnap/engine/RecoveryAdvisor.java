package com.statesnap.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.statesnap.entity.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class RecoveryAdvisor {

    private final ObjectMapper objectMapper;

    public RecoveryAdvisor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RecoveryAnalysis analyze(
            TransactionRecord transaction,
            InferredTransactionState currentState,
            List<EvidenceRecord> evidenceList,
            List<TransactionEvent> events
    ) {
        if (currentState == null) {
            return RecoveryAnalysis.builder()
                    .id(UUID.randomUUID().toString())
                    .transaction(transaction)
                    .suggestedAction(RecoveryActionType.MANUAL_RECONCILIATION_REQUIRED)
                    .safeToExecute(false)
                    .rationale("Transaction state has not been reconstructed yet. Run reconstruction before recovery analysis.")
                    .prerequisitesJson("[\"Execute deterministic state reconstruction\"]")
                    .missingPrerequisitesJson("[\"Inferred state artifact\"]")
                    .risksJson("[\"Unknown state: executing recovery blindly may create unrecoverable side effects\"]")
                    .analyzedAt(Instant.now())
                    .build();
        }

        TransactionStateEnum state = currentState.getInferredState();
        RecoveryActionType action;
        boolean safe;
        StringBuilder rationale = new StringBuilder();
        List<String> prerequisites = new ArrayList<>();
        List<String> missingPrerequisites = new ArrayList<>();
        List<String> risks = new ArrayList<>();

        boolean hasAuthApproval = events.stream().anyMatch(e ->
                e.getEventType().equalsIgnoreCase("AUTH_APPROVED") ||
                e.getEventType().equalsIgnoreCase("PAYMENT_AUTH_SUCCESS"));

        boolean hasCaptureCompleted = events.stream().anyMatch(e ->
                e.getEventType().equalsIgnoreCase("CAPTURE_COMPLETED") ||
                e.getEventType().equalsIgnoreCase("LEDGER_COMMITTED"));

        boolean hasConflict = evidenceList.stream().anyMatch(e ->
                e.getEvidenceType() == EvidenceType.CONFLICTING_SIGNAL) || state == TransactionStateEnum.CONFLICTED;

        if (hasConflict) {
            action = RecoveryActionType.UNSAFE_CONFLICT_DETECTED;
            safe = false;
            rationale.append("CRITICAL: Contradictory evidence detected in cross-system event streams. ")
                    .append("Automated recovery is strictly blocked to avoid double refunds, duplicate debits, or ledger desynchronization.");
            prerequisites.add("Manual cross-system ledger reconciliation");
            prerequisites.add("Direct bank/processor settlement export review");
            prerequisites.add("Senior engineer or ops sign-off");
            missingPrerequisites.add("Independent processor settlement confirmation");
            risks.add("Automated refund could cause irreversible loss if capture succeeded out-of-band");
            risks.add("Retrying capture could double-bill the cardholder");
        } else if (state == TransactionStateEnum.COMMITTED) {
            action = RecoveryActionType.NO_ACTION_REQUIRED;
            safe = true;
            rationale.append("Transaction is verified COMMITTED. All required lifecycle stages succeeded. No compensating operations are needed.");
            prerequisites.add("Preserve immutable audit log");
        } else if (state == TransactionStateEnum.ROLLED_BACK) {
            action = RecoveryActionType.NO_ACTION_REQUIRED;
            safe = true;
            rationale.append("Transaction is verified ROLLED_BACK. Compensating refund or cancellation was already finalized.");
        } else if (state == TransactionStateEnum.FAILED) {
            if (hasAuthApproval && !hasCaptureCompleted) {
                action = RecoveryActionType.EXECUTE_COMPENSATING_REFUND;
                safe = true;
                rationale.append("Cardholder authorization hold succeeded, but subsequent capture or settlement failed/timed out. ")
                        .append("Recommended action: Issue an authorization void / release hold to free customer funds.");
                prerequisites.add("Authorization reference ID from gateway logs");
                prerequisites.add("Idempotency key for void request");
                prerequisites.add("Confirmation that capture window has elapsed");
                risks.add("Risk of void failure if capture already settled asynchronously");
            } else {
                action = RecoveryActionType.NO_ACTION_REQUIRED;
                safe = true;
                rationale.append("Transaction failed cleanly at or before authorization. No customer funds were captured or reserved.");
            }
        } else if (state == TransactionStateEnum.PROCESSING || state == TransactionStateEnum.INITIATED) {
            action = RecoveryActionType.RESEND_CAPTURE_PROBE;
            safe = true;
            rationale.append("Transaction is stalled in flight. Out-of-band status query probe is recommended to determine downstream outcome before taking destructive action.");
            prerequisites.add("Active processor query API health");
            prerequisites.add("Transaction reference token");
            missingPrerequisites.add("Final terminal response from payment gateway");
            risks.add("Premature timeout could abandon a transaction currently completing downstream");
        } else {
            action = RecoveryActionType.MANUAL_RECONCILIATION_REQUIRED;
            safe = false;
            rationale.append("Transaction state is UNKNOWN or ambiguous. Telemetry logs are incomplete.");
            prerequisites.add("Obtain raw gateway logs");
            missingPrerequisites.add("Missing core event trace");
            risks.add("Any automated recovery on UNKNOWN state is hazardous");
        }

        return RecoveryAnalysis.builder()
                .id(UUID.randomUUID().toString())
                .transaction(transaction)
                .suggestedAction(action)
                .safeToExecute(safe)
                .rationale(rationale.toString())
                .prerequisitesJson(toJson(prerequisites))
                .missingPrerequisitesJson(toJson(missingPrerequisites))
                .risksJson(toJson(risks))
                .analyzedAt(Instant.now())
                .build();
    }

    private String toJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}

