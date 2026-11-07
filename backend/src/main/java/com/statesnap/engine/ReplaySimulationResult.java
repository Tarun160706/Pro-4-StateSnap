package com.statesnap.engine;

import com.statesnap.entity.ReplayEvent;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ReplaySimulationResult {
    private final String scenarioDescription;
    private final String baselineState;
    private final String baselineCertainty;
    private final String simulatedState;
    private final String simulatedCertainty;
    private final Double simulatedConfidenceScore;
    private final String findings;
    private final String differencesSummary;
    private final List<ReplayEvent> simulatedEvents;
}

