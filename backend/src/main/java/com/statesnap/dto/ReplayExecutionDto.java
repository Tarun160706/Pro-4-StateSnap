package com.statesnap.dto;

import com.statesnap.entity.ReplayScenarioType;
import com.statesnap.entity.ReplayStatus;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplayExecutionDto {
    private String id;
    private String originalTransactionId;
    private ReplayScenarioType scenarioType;
    private String scenarioDescription;
    private String targetEventId;
    private Long delayMillis;
    private String baselineState;
    private String baselineCertainty;
    private String simulatedState;
    private String simulatedCertainty;
    private Double simulatedConfidenceScore;
    private String findings;
    private String differencesSummary;
    private ReplayStatus status;
    private Boolean isSandboxed;
    private Instant createdAt;
    private List<ReplayEventDto> simulatedEvents;
}

