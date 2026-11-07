package com.statesnap.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "transaction_events", indexes = {
        @Index(name = "idx_event_tx_id", columnList = "transaction_id"),
        @Index(name = "idx_event_type", columnList = "eventType"),
        @Index(name = "idx_event_source", columnList = "sourceSystem"),
        @Index(name = "idx_event_timestamp", columnList = "timestamp"),
        @Index(name = "idx_event_seq", columnList = "sequenceNumber")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEvent {

    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    @JsonIgnore
    private TransactionRecord transaction;

    @Column(nullable = false, length = 100)
    private String eventId;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, length = 100)
    private String sourceSystem;

    @Column(nullable = false)
    private Instant timestamp;

    @Column
    private Long sequenceNumber;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String rawPayload;

    @Column(nullable = false)
    @Builder.Default
    private Instant ingestionTimestamp = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private EventIntegrityStatus integrityStatus = EventIntegrityStatus.VALID;
}

