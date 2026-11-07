package com.statesnap.dto;

import com.statesnap.entity.CertaintyLevel;
import com.statesnap.entity.TransactionStateEnum;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDto {
    private String id;
    private String externalId;
    private String title;
    private String channel;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String metadataJson;
    private long eventCount;
    private TransactionStateEnum latestState;
    private CertaintyLevel latestCertainty;
    private Double latestConfidenceScore;
    private boolean hasConflict;
    private String evidenceQuality;
    private int unresolvedIssuesCount;
    private Instant createdAt;
    private Instant updatedAt;
    private String investigatorEmail;
}

