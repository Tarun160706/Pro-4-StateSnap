package com.statesnap.dto;

import com.statesnap.entity.RecoveryActionType;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecoveryAnalysisDto {
    private String id;
    private RecoveryActionType suggestedAction;
    private Boolean safeToExecute;
    private String rationale;
    private List<String> prerequisites;
    private List<String> missingPrerequisites;
    private List<String> risks;
    private Instant analyzedAt;
}

