package com.statesnap.dto;

import com.statesnap.entity.EvidenceType;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvidenceRecordDto {
    private String id;
    private EvidenceType evidenceType;
    private String sourceEventId;
    private String description;
    private String details;
    private Double confidenceWeight;
    private Instant detectedAt;
}

