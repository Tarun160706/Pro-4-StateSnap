package com.statesnap.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "recovery_analyses", indexes = {
        @Index(name = "idx_recovery_tx_id", columnList = "transaction_id"),
        @Index(name = "idx_recovery_action", columnList = "suggestedAction")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecoveryAnalysis {

    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    @JsonIgnore
    private TransactionRecord transaction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RecoveryActionType suggestedAction;

    @Column(nullable = false)
    private Boolean safeToExecute;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String rationale;

    @Column(columnDefinition = "TEXT")
    private String prerequisitesJson;

    @Column(columnDefinition = "TEXT")
    private String missingPrerequisitesJson;

    @Column(columnDefinition = "TEXT")
    private String risksJson;

    @Column(nullable = false)
    @Builder.Default
    private Instant analyzedAt = Instant.now();
}

