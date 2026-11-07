package com.statesnap.dto;

import com.statesnap.entity.EventIntegrityStatus;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEventDto {
    private String id;
    private String eventId;
    private String eventType;
    private String sourceSystem;
    private Instant timestamp;
    private Long sequenceNumber;
    private String rawPayload;
    private Instant ingestionTimestamp;
    private EventIntegrityStatus integrityStatus;
}

