package com.statesnap.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.statesnap.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReconstructionEngineTest {

    private ReconstructionEngine engine;
    private TransactionRecord mockTransaction;

    @BeforeEach
    void setUp() {
        engine = new ReconstructionEngine(new ObjectMapper());
        mockTransaction = TransactionRecord.builder()
                .id("tx-test-01")
                .externalId("TX-TEST-01")
                .title("Test Transaction")
                .build();
    }

    @Test
    @DisplayName("Healthy milestone sequence reconstructs to COMMITTED with HIGH_CERTAINTY")
    void testHealthySequenceReconstructsToCommitted() {
        Instant now = Instant.now();
        List<TransactionEvent> events = List.of(
                TransactionEvent.builder().eventId("e1").eventType("PAYMENT_INITIATED").sourceSystem("PORTAL").timestamp(now).sequenceNumber(1L).rawPayload("{}").build(),
                TransactionEvent.builder().eventId("e2").eventType("AUTH_REQUESTED").sourceSystem("ROUTER").timestamp(now.plusSeconds(1)).sequenceNumber(2L).rawPayload("{}").build(),
                TransactionEvent.builder().eventId("e3").eventType("AUTH_APPROVED").sourceSystem("ACQUIRER").timestamp(now.plusSeconds(2)).sequenceNumber(3L).rawPayload("{}").build(),
                TransactionEvent.builder().eventId("e4").eventType("CAPTURE_COMPLETED").sourceSystem("LEDGER").timestamp(now.plusSeconds(3)).sequenceNumber(4L).rawPayload("{}").build()
        );

        ReconstructionResult result = engine.reconstruct(mockTransaction, events);

        assertEquals(TransactionStateEnum.COMMITTED, result.getInferredState());
        assertEquals(CertaintyLevel.HIGH_CERTAINTY, result.getCertaintyLevel());
        assertTrue(result.getConfidenceScore() >= 85.0);
        assertTrue(result.getContradictingEventIds().isEmpty());
        assertTrue(result.getMissingEvidenceList().isEmpty());
    }

    @Test
    @DisplayName("Conflicting signals (AUTH_FAILED + LEDGER_COMMITTED) reconstructs to CONFLICTED with HIGH_UNCERTAINTY")
    void testConflictingSignalsReconstructsToConflicted() {
        Instant now = Instant.now();
        List<TransactionEvent> events = List.of(
                TransactionEvent.builder().eventId("e1").eventType("PAYMENT_INITIATED").sourceSystem("PORTAL").timestamp(now).sequenceNumber(1L).rawPayload("{}").build(),
                TransactionEvent.builder().eventId("e2").eventType("AUTH_FAILED").sourceSystem("GATEWAY").timestamp(now.plusSeconds(2)).sequenceNumber(2L).rawPayload("{}").build(),
                TransactionEvent.builder().eventId("e3").eventType("LEDGER_COMMITTED").sourceSystem("LEDGER").timestamp(now.plusSeconds(3)).sequenceNumber(3L).rawPayload("{}").build()
        );

        ReconstructionResult result = engine.reconstruct(mockTransaction, events);

        assertEquals(TransactionStateEnum.CONFLICTED, result.getInferredState());
        assertEquals(CertaintyLevel.HIGH_UNCERTAINTY, result.getCertaintyLevel());
        assertFalse(result.getContradictingEventIds().isEmpty());
        assertTrue(result.getEvidenceRecords().stream()
                .anyMatch(e -> e.getEvidenceType() == EvidenceType.CONFLICTING_SIGNAL));
    }

    @Test
    @DisplayName("Capture completed without Auth approved detects MISSING_STEP evidence")
    void testMissingStepDetection() {
        Instant now = Instant.now();
        List<TransactionEvent> events = List.of(
                TransactionEvent.builder().eventId("e1").eventType("PAYMENT_INITIATED").sourceSystem("PORTAL").timestamp(now).sequenceNumber(1L).rawPayload("{}").build(),
                TransactionEvent.builder().eventId("e2").eventType("CAPTURE_COMPLETED").sourceSystem("LEDGER").timestamp(now.plusSeconds(2)).sequenceNumber(2L).rawPayload("{}").build()
        );

        ReconstructionResult result = engine.reconstruct(mockTransaction, events);

        assertFalse(result.getMissingEvidenceList().isEmpty());
        assertTrue(result.getEvidenceRecords().stream()
                .anyMatch(e -> e.getEvidenceType() == EvidenceType.MISSING_STEP));
    }

    @Test
    @DisplayName("Duplicate event IDs detect DUPLICATE_SIGNAL evidence")
    void testDuplicateEventDetection() {
        Instant now = Instant.now();
        List<TransactionEvent> events = List.of(
                TransactionEvent.builder().eventId("e1").eventType("PAYMENT_INITIATED").sourceSystem("PORTAL").timestamp(now).sequenceNumber(1L).rawPayload("{}").build(),
                TransactionEvent.builder().eventId("e1").eventType("PAYMENT_INITIATED").sourceSystem("PORTAL").timestamp(now.plusMillis(200)).sequenceNumber(1L).rawPayload("{}").build()
        );

        ReconstructionResult result = engine.reconstruct(mockTransaction, events);

        assertTrue(result.getEvidenceRecords().stream()
                .anyMatch(e -> e.getEvidenceType() == EvidenceType.DUPLICATE_SIGNAL));
    }
}

