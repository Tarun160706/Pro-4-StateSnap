package com.statesnap.engine;

import com.statesnap.entity.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
public class ReplayEngine {

    private final ReconstructionEngine reconstructionEngine;

    public ReplayEngine(ReconstructionEngine reconstructionEngine) {
        this.reconstructionEngine = reconstructionEngine;
    }

    /**
     * Executes sandboxed fault-injection simulation.
     * Original transaction records and real events are strictly unmodified.
     */
    public ReplaySimulationResult simulate(
            TransactionRecord originalTransaction,
            List<TransactionEvent> originalEvents,
            ReplayScenarioType scenarioType,
            String targetEventId,
            Long delayMillis,
            String conflictingPayload
    ) {
        // 1. Run baseline reconstruction first
        ReconstructionResult baselineResult = reconstructionEngine.reconstruct(originalTransaction, originalEvents);

        // 2. Clone the event stream into isolated sandbox replay events
        List<ReplayEvent> simulatedEvents = new ArrayList<>();
        long seq = 1;
        for (TransactionEvent orig : originalEvents) {
            simulatedEvents.add(ReplayEvent.builder()
                    .id(UUID.randomUUID().toString())
                    .originalEventId(orig.getEventId())
                    .eventType(orig.getEventType())
                    .sourceSystem(orig.getSourceSystem())
                    .simulatedTimestamp(orig.getTimestamp())
                    .simulatedSequenceNumber(orig.getSequenceNumber() != null ? orig.getSequenceNumber() : seq++)
                    .rawPayload(orig.getRawPayload())
                    .mutationType(MutationType.ORIGINAL)
                    .build());
        }

        StringBuilder scenarioDesc = new StringBuilder();
        StringBuilder findings = new StringBuilder();

        // 3. Apply sandboxed fault mutation
        switch (scenarioType) {
            case MISSING_EVENT -> {
                scenarioDesc.append("Simulating dropped network packet / missing webhook callback.");
                if (targetEventId != null && !targetEventId.isBlank()) {
                    simulatedEvents.removeIf(e -> e.getOriginalEventId().equalsIgnoreCase(targetEventId));
                    scenarioDesc.append(" Dropped target event: ").append(targetEventId);
                } else if (!simulatedEvents.isEmpty()) {
                    // Default: drop the intermediate event (e.g. index size / 2)
                    int dropIndex = Math.max(0, simulatedEvents.size() - 2);
                    ReplayEvent dropped = simulatedEvents.remove(dropIndex);
                    scenarioDesc.append(" Dropped event: ").append(dropped.getOriginalEventId())
                            .append(" (").append(dropped.getEventType()).append(")");
                }
            }
            case DUPLICATE_EVENT -> {
                scenarioDesc.append("Simulating duplicate webhook / retry storm delivery.");
                ReplayEvent target = null;
                if (targetEventId != null && !targetEventId.isBlank()) {
                    target = simulatedEvents.stream()
                            .filter(e -> e.getOriginalEventId().equalsIgnoreCase(targetEventId))
                            .findFirst().orElse(null);
                }
                if (target == null && !simulatedEvents.isEmpty()) {
                    target = simulatedEvents.get(simulatedEvents.size() - 1);
                }
                if (target != null) {
                    ReplayEvent duplicate = ReplayEvent.builder()
                            .id(UUID.randomUUID().toString())
                            .originalEventId(target.getOriginalEventId())
                            .eventType(target.getEventType())
                            .sourceSystem(target.getSourceSystem())
                            .simulatedTimestamp(target.getSimulatedTimestamp().plusMillis(delayMillis != null ? delayMillis : 500))
                            .simulatedSequenceNumber(target.getSimulatedSequenceNumber() != null ? target.getSimulatedSequenceNumber() + 1 : null)
                            .rawPayload(target.getRawPayload())
                            .mutationType(MutationType.INJECTED_DUPLICATE)
                            .build();
                    simulatedEvents.add(duplicate);
                    scenarioDesc.append(" Duplicated event: ").append(target.getOriginalEventId());
                }
            }
            case DELAYED_EVENT -> {
                long delay = delayMillis != null && delayMillis > 0 ? delayMillis : 5000L;
                scenarioDesc.append("Simulating network latency delay of ").append(delay).append("ms.");
                for (ReplayEvent e : simulatedEvents) {
                    if (targetEventId == null || e.getOriginalEventId().equalsIgnoreCase(targetEventId)) {
                        e.setSimulatedTimestamp(e.getSimulatedTimestamp().plusMillis(delay));
                        e.setMutationType(MutationType.DELAYED);
                        scenarioDesc.append(" Delayed event: ").append(e.getOriginalEventId());
                        break;
                    }
                }
            }
            case OUT_OF_ORDER_EVENT -> {
                scenarioDesc.append("Simulating message broker out-of-order delivery.");
                if (simulatedEvents.size() >= 2) {
                    // Invert the last two events' sequence or timestamps
                    ReplayEvent first = simulatedEvents.get(simulatedEvents.size() - 2);
                    ReplayEvent second = simulatedEvents.get(simulatedEvents.size() - 1);

                    Instant tempTime = first.getSimulatedTimestamp();
                    first.setSimulatedTimestamp(second.getSimulatedTimestamp());
                    second.setSimulatedTimestamp(tempTime.minusMillis(200));

                    first.setMutationType(MutationType.REORDERED);
                    second.setMutationType(MutationType.REORDERED);

                    scenarioDesc.append(" Inverted causal order of [")
                            .append(first.getEventType()).append("] and [").append(second.getEventType()).append("]");
                }
            }
            case CONFLICTING_EVENT -> {
                scenarioDesc.append("Simulating conflicting cross-system signal (e.g. gateway decline vs settlement commit).");
                boolean injectFail = baselineResult.getInferredState() == TransactionStateEnum.COMMITTED;
                String conflictType = injectFail ? "AUTH_FAILED" : "CAPTURE_COMPLETED";
                String conflictPayload = conflictingPayload != null && !conflictingPayload.isBlank()
                        ? conflictingPayload
                        : "{\"conflictReason\": \"Simulated asynchronous contradictory terminal callback\", \"injected\": true}";

                ReplayEvent conflictEvt = ReplayEvent.builder()
                        .id(UUID.randomUUID().toString())
                        .originalEventId("sim_conflict_" + UUID.randomUUID().toString().substring(0, 8))
                        .eventType(conflictType)
                        .sourceSystem("SANDBOX_EXTERNAL_GATEWAY")
                        .simulatedTimestamp(Instant.now())
                        .simulatedSequenceNumber(seq + 10)
                        .rawPayload(conflictPayload)
                        .mutationType(MutationType.INJECTED_CONFLICT)
                        .build();

                simulatedEvents.add(conflictEvt);
                scenarioDesc.append(" Injected conflicting signal: ").append(conflictType);
            }
            case DROP_AFTER_STEP -> {
                scenarioDesc.append("Simulating abrupt system crash/halt after step.");
                if (targetEventId != null && !targetEventId.isBlank()) {
                    int targetIdx = -1;
                    for (int i = 0; i < simulatedEvents.size(); i++) {
                        if (simulatedEvents.get(i).getOriginalEventId().equalsIgnoreCase(targetEventId)) {
                            targetIdx = i;
                            break;
                        }
                    }
                    if (targetIdx >= 0 && targetIdx < simulatedEvents.size() - 1) {
                        simulatedEvents = new ArrayList<>(simulatedEvents.subList(0, targetIdx + 1));
                    }
                } else if (simulatedEvents.size() > 1) {
                    simulatedEvents = new ArrayList<>(simulatedEvents.subList(0, 1));
                }
                scenarioDesc.append(" Dropped all trailing events after cutoff step.");
            }
        }

        // 4. Convert simulated events to transient TransactionEvent objects to run reconstruction engine
        List<TransactionEvent> mutatedTransientEvents = new ArrayList<>();
        for (ReplayEvent re : simulatedEvents) {
            mutatedTransientEvents.add(TransactionEvent.builder()
                    .id(re.getId())
                    .transaction(originalTransaction)
                    .eventId(re.getOriginalEventId() != null ? re.getOriginalEventId() : re.getId())
                    .eventType(re.getEventType())
                    .sourceSystem(re.getSourceSystem())
                    .timestamp(re.getSimulatedTimestamp())
                    .sequenceNumber(re.getSimulatedSequenceNumber())
                    .rawPayload(re.getRawPayload())
                    .integrityStatus(EventIntegrityStatus.VALID)
                    .build());
        }

        // 5. Reconstruct simulated state
        ReconstructionResult simulatedResult = reconstructionEngine.reconstruct(originalTransaction, mutatedTransientEvents);

        // 6. Generate comparative differences summary
        boolean stateChanged = baselineResult.getInferredState() != simulatedResult.getInferredState();
        findings.append("### Fault-Injection Replay Findings\n\n");
        findings.append("- **Scenario Executed**: `").append(scenarioType.name()).append("`\n");
        findings.append("- **Baseline State**: `").append(baselineResult.getInferredState().name())
                .append("` (Certainty: ").append(baselineResult.getCertaintyLevel().name()).append(")\n");
        findings.append("- **Simulated State**: `").append(simulatedResult.getInferredState().name())
                .append("` (Certainty: ").append(simulatedResult.getCertaintyLevel().name()).append(")\n");
        findings.append("- **State Drift Detected**: `").append(stateChanged ? "YES - CRITICAL DIVERGENCE" : "NO - RESILIENT").append("`\n\n");

        if (stateChanged) {
            findings.append("⚠️ **Impact Assessment**: Under this simulated fault, the transaction state drifted from `")
                    .append(baselineResult.getInferredState().name()).append("` to `")
                    .append(simulatedResult.getInferredState().name())
                    .append("`. This demonstrates vulnerability to uncoordinated callbacks or missing signals.\n");
        } else {
            findings.append("✅ **Resilience Assessment**: The state reconstruction proved resilient to this mutation, maintaining `")
                    .append(baselineResult.getInferredState().name()).append("`.\n");
        }

        return ReplaySimulationResult.builder()
                .scenarioDescription(scenarioDesc.toString())
                .baselineState(baselineResult.getInferredState().name())
                .baselineCertainty(baselineResult.getCertaintyLevel().name())
                .simulatedState(simulatedResult.getInferredState().name())
                .simulatedCertainty(simulatedResult.getCertaintyLevel().name())
                .simulatedConfidenceScore(simulatedResult.getConfidenceScore())
                .findings(findings.toString())
                .differencesSummary(stateChanged
                        ? "Divergence: baseline=" + baselineResult.getInferredState() + ", simulated=" + simulatedResult.getInferredState()
                        : "Consistent: state remained " + baselineResult.getInferredState())
                .simulatedEvents(simulatedEvents)
                .build();
    }
}

