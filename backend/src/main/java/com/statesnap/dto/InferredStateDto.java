package com.statesnap.dto;

import com.statesnap.entity.CertaintyLevel;
import com.statesnap.entity.TransactionStateEnum;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InferredStateDto {
    private String id;
    private TransactionStateEnum inferredState;
    private CertaintyLevel certaintyLevel;
    private Double confidenceScore;
    private List<String> supportingEvents;
    private List<String> contradictingEvents;
    private List<String> missingEvidence;
    private String reasoningExplanation;
    private Instant reconstructedAt;
}

