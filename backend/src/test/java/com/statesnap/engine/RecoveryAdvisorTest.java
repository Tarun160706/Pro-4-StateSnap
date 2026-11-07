package com.statesnap.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.statesnap.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecoveryAdvisorTest {

    private RecoveryAdvisor recoveryAdvisor;
    private TransactionRecord mockTransaction;

    @BeforeEach
    void setUp() {
        recoveryAdvisor = new RecoveryAdvisor(new ObjectMapper());
        mockTransaction = TransactionRecord.builder()
                .id("tx-recovery-01")
                .externalId("TX-RECOVERY-01")
                .title("Recovery Test")
                .build();
    }

    @Test
    @DisplayName("Conflicted state strictly gates automated recovery: safeToExecute must be false")
    void testConflictedStateIsUnsafe() {
        InferredTransactionState state = InferredTransactionState.builder()
                .inferredState(TransactionStateEnum.CONFLICTED)
                .certaintyLevel(CertaintyLevel.HIGH_UNCERTAINTY)
                .confidenceScore(15.0)
                .build();

        List<EvidenceRecord> evidence = List.of(
                EvidenceRecord.builder().evidenceType(EvidenceType.CONFLICTING_SIGNAL).description("Dual commit/fail").build()
        );

        RecoveryAnalysis analysis = recoveryAdvisor.analyze(mockTransaction, state, evidence, List.of());

        assertEquals(RecoveryActionType.UNSAFE_CONFLICT_DETECTED, analysis.getSuggestedAction());
        assertFalse(analysis.getSafeToExecute());
        assertTrue(analysis.getRisksJson().contains("Automated refund could cause irreversible loss"));
    }

    @Test
    @DisplayName("Failed state with pre-auth hold recommends compensating refund void")
    void testFailedStateWithAuthHoldRecommendsVoid() {
        InferredTransactionState state = InferredTransactionState.builder()
                .inferredState(TransactionStateEnum.FAILED)
                .certaintyLevel(CertaintyLevel.HIGH_CERTAINTY)
                .confidenceScore(90.0)
                .build();

        List<TransactionEvent> events = List.of(
                TransactionEvent.builder().eventId("e1").eventType("PAYMENT_INITIATED").sourceSystem("PORTAL").timestamp(Instant.now()).rawPayload("{}").build(),
                TransactionEvent.builder().eventId("e2").eventType("AUTH_APPROVED").sourceSystem("GATEWAY").timestamp(Instant.now()).rawPayload("{}").build(),
                TransactionEvent.builder().eventId("e3").eventType("CAPTURE_FAILED").sourceSystem("SETTLEMENT").timestamp(Instant.now()).rawPayload("{}").build()
        );

        RecoveryAnalysis analysis = recoveryAdvisor.analyze(mockTransaction, state, List.of(), events);

        assertEquals(RecoveryActionType.EXECUTE_COMPENSATING_REFUND, analysis.getSuggestedAction());
        assertTrue(analysis.getSafeToExecute());
        assertTrue(analysis.getPrerequisitesJson().contains("Authorization reference ID"));
    }
}

