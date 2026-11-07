package com.statesnap.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "replay_events", indexes = {
        @Index(name = "idx_replay_evt_exec_id", columnList = "replay_execution_id"),
        @Index(name = "idx_replay_evt_type", columnList = "eventType"),
        @Index(name = "idx_replay_evt_mutation", columnList = "mutationType")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplayEvent {

    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replay_execution_id", nullable = false)
    @JsonIgnore
    private ReplayExecution replayExecution;

    @Column(length = 100)
    private String originalEventId;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, length = 100)
    private String sourceSystem;

    @Column(nullable = false)
    private Instant simulatedTimestamp;

    @Column
    private Long simulatedSequenceNumber;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String rawPayload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private MutationType mutationType = MutationType.ORIGINAL;
}

