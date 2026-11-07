package com.statesnap.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.statesnap.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReplayEngineTest {

    private ReplayEngine replayEngine;
    private TransactionRecord mockTransaction;
    private List<TransactionEvent> originalEvents;

    @BeforeEach
    void setUp() {
        ReconstructionEngine reconstructionEngine = new ReconstructionEngine(new ObjectMapper());
        replayEngine = new ReplayEngine(reconstructionEngine);

        mockTransaction = TransactionRecord.builder()
                .id("tx-orig-01")
                .externalId("TX-ORIG-01")
                .title("Original Transaction")
                .build();

        Instant now = Instant.now();
        originalEvents = List.of(
                TransactionEvent.builder().eventId("e1").eventType("PAYMENT_INITIATED").sourceSystem("PORTAL").timestamp(now).sequenceNumber(1L).rawPayload("{}").build(),
                TransactionEvent.builder().eventId("e2").eventType("AUTH_REQUESTED").sourceSystem("ROUTER").timestamp(now.plusSeconds(1)).sequenceNumber(2L).rawPayload("{}").build(),
                TransactionEvent.builder().eventId("e3").eventType("AUTH_APPROVED").sourceSystem("ACQUIRER").timestamp(now.plusSeconds(2)).sequenceNumber(3L).rawPayload("{}").build(),
                TransactionEvent.builder().eventId("e4").eventType("CAPTURE_COMPLETED").sourceSystem("LEDGER").timestamp(now.plusSeconds(3)).sequenceNumber(4L).rawPayload("{}").build()
        );
    }

    @Test
    @DisplayName("Injecting conflicting event produces state divergence in sandbox without altering original events")
    void testConflictingFaultInjection() {
        int originalSize = originalEvents.size();

        ReplaySimulationResult result = replayEngine.simulate(
                mockTransaction,
                originalEvents,
                ReplayScenarioType.CONFLICTING_EVENT,
                null,
                null,
                null
        );

        // Original events collection must remain completely unmodified
        assertEquals(originalSize, originalEvents.size());
        assertEquals("COMMITTED", result.getBaselineState());
        assertEquals("CONFLICTED", result.getSimulatedState());
        assertTrue(result.getDifferencesSummary().contains("Divergence"));
        assertTrue(result.getSimulatedEvents().stream()
                .anyMatch(e -> e.getMutationType() == MutationType.INJECTED_CONFLICT));
    }

    @Test
    @DisplayName("Dropping event simulates missing callback accurately")
    void testMissingEventFaultInjection() {
        ReplaySimulationResult result = replayEngine.simulate(
                mockTransaction,
                originalEvents,
                ReplayScenarioType.MISSING_EVENT,
                "e3", // Drop AUTH_APPROVED
                null,
                null
        );

        assertEquals("COMMITTED", result.getBaselineState());
        // Verify e3 is absent from simulated events
        assertTrue(result.getSimulatedEvents().stream().noneMatch(e -> "e3".equals(e.getOriginalEventId())));
    }
}

