package com.statesnap.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemStatusDto {
    private String status; // "ONLINE", "DEGRADED", "OFFLINE"
    private String engineVersion;
    private String databaseStatus;
    private Instant timestamp;
}

