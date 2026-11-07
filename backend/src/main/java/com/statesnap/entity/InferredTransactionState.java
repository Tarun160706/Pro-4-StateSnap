package com.statesnap.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "inferred_transaction_states", indexes = {
        @Index(name = "idx_inferred_tx_id", columnList = "transaction_id"),
        @Index(name = "idx_inferred_state", columnList = "inferredState"),
        @Index(name = "idx_inferred_certainty", columnList = "certaintyLevel")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InferredTransactionState {

    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    @JsonIgnore
    private TransactionRecord transaction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TransactionStateEnum inferredState;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CertaintyLevel certaintyLevel;

    @Column(nullable = false)
    private Double confidenceScore;

    @Column(columnDefinition = "TEXT")
    private String supportingEventsJson;

    @Column(columnDefinition = "TEXT")
    private String contradictingEventsJson;

    @Column(columnDefinition = "TEXT")
    private String missingEvidenceJson;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reasoningExplanation;

    @Column(nullable = false)
    @Builder.Default
    private Instant reconstructedAt = Instant.now();
}

