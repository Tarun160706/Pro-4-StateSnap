package com.statesnap.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTransactionRequest {

    @NotBlank(message = "External transaction ID is required")
    private String externalId;

    @NotBlank(message = "Title is required")
    private String title;

    private String channel;
    private BigDecimal amount;
    private String currency;
    private String metadataJson;
}

