package com.statesnap.dto;

import com.statesnap.entity.TransitionType;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StateTransitionDto {
    private String id;
    private String fromState;
    private String toState;
    private String triggerEventId;
    private TransitionType transitionType;
    private Instant timestamp;
    private String notes;
}

