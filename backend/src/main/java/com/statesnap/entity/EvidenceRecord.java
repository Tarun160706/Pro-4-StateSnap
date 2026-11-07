package com.statesnap.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "evidence_records", indexes = {
        @Index(name = "idx_evidence_tx_id", columnList = "transaction_id"),
        @Index(name = "idx_evidence_type", columnList = "evidenceType"),
        @Index(name = "idx_evidence_source_evt", columnList = "sourceEventId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvidenceRecord {

    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    @JsonIgnore
    private TransactionRecord transaction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EvidenceType evidenceType;

    @Column(length = 100)
    private String sourceEventId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false)
    @Builder.Default
    private Double confidenceWeight = 1.0;

    @Column(nullable = false)
    @Builder.Default
    private Instant detectedAt = Instant.now();
}

