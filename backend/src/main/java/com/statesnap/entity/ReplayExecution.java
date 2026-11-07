package com.statesnap.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "replay_executions", indexes = {
        @Index(name = "idx_replay_tx_id", columnList = "originalTransactionId"),
        @Index(name = "idx_replay_scenario", columnList = "scenarioType"),
        @Index(name = "idx_replay_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplayExecution {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 64)
    private String originalTransactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User executedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ReplayScenarioType scenarioType;

    @Column(columnDefinition = "TEXT")
    private String scenarioDescription;

    @Column(length = 100)
    private String targetEventId;

    @Column
    private Long delayMillis;

    @Column(length = 40)
    private String baselineState;

    @Column(length = 40)
    private String baselineCertainty;

    @Column(length = 40)
    private String simulatedState;

    @Column(length = 40)
    private String simulatedCertainty;

    @Column
    private Double simulatedConfidenceScore;

    @Column(columnDefinition = "TEXT")
    private String findingsJson;

    @Column(columnDefinition = "TEXT")
    private String differencesSummary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ReplayStatus status = ReplayStatus.PENDING;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isSandboxed = true;

    @OneToMany(mappedBy = "replayExecution", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private List<ReplayEvent> simulatedEvents = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}

