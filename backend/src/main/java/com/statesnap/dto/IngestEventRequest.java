package com.statesnap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngestEventRequest {

    @NotBlank(message = "Event ID is required")
    private String eventId;

    @NotBlank(message = "Event type is required")
    private String eventType;

    @NotBlank(message = "Source system is required")
    private String sourceSystem;

    @NotNull(message = "Timestamp is required")
    private Instant timestamp;

    private Long sequenceNumber;

    @NotBlank(message = "Raw payload is required")
    private String rawPayload;
}

