package com.statesnap.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchIngestRequest {

    @NotEmpty(message = "Events list cannot be empty")
    @Valid
    private List<IngestEventRequest> events;
}

