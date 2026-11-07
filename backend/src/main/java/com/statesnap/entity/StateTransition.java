package com.statesnap.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "state_transitions", indexes = {
        @Index(name = "idx_transition_tx_id", columnList = "transaction_id"),
        @Index(name = "idx_transition_type", columnList = "transitionType"),
        @Index(name = "idx_transition_time", columnList = "timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StateTransition {

    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    @JsonIgnore
    private TransactionRecord transaction;

    @Column(nullable = false, length = 40)
    private String fromState;

    @Column(nullable = false, length = 40)
    private String toState;

    @Column(length = 100)
    private String triggerEventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private TransitionType transitionType = TransitionType.DETERMINISTIC;

    @Column(nullable = false)
    @Builder.Default
    private Instant timestamp = Instant.now();

    @Column(length = 255)
    private String notes;
}

