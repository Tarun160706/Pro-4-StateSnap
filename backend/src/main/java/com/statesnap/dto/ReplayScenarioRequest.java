package com.statesnap.dto;

import com.statesnap.entity.ReplayScenarioType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplayScenarioRequest {

    @NotNull(message = "Replay scenario type is required")
    private ReplayScenarioType scenarioType;

    private String targetEventId;
    private Long delayMillis;
    private String conflictingPayload;
}

