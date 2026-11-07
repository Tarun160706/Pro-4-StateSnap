package com.statesnap.engine;

import com.statesnap.entity.*;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ReconstructionResult {
    private final TransactionStateEnum inferredState;
    private final CertaintyLevel certaintyLevel;
    private final Double confidenceScore;
    private final List<String> supportingEventIds;
    private final List<String> contradictingEventIds;
    private final List<String> missingEvidenceList;
    private final String reasoningExplanation;
    private final List<EvidenceRecord> evidenceRecords;
    private final List<StateTransition> stateTransitions;
}

