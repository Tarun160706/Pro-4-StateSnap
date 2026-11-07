package com.statesnap.dto;

import com.statesnap.entity.MutationType;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplayEventDto {
    private String id;
    private String originalEventId;
    private String eventType;
    private String sourceSystem;
    private Instant simulatedTimestamp;
    private Long simulatedSequenceNumber;
    private String rawPayload;
    private MutationType mutationType;
}

