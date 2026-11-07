package com.statesnap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.statesnap.dto.*;
import com.statesnap.entity.ReplayScenarioType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("End-to-end integration test: register -> login -> create tx -> ingest events -> reconstruct -> replay -> recovery")
    void testCompleteTransactionLifecycle() throws Exception {
        // 1. Register User
        RegisterRequest regReq = RegisterRequest.builder()
                .email("test.engineer@statesnap.io")
                .password("Password#2026!")
                .fullName("Test Engineer")
                .build();

        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String token = objectMapper.readTree(regResult.getResponse().getContentAsString())
                .path("data").path("token").asText();

        // 2. Create Transaction
        CreateTransactionRequest txReq = CreateTransactionRequest.builder()
                .externalId("TX-E2E-999")
                .title("E2E Test Checkout Flow")
                .channel("E2E_GATEWAY")
                .amount(new BigDecimal("99.99"))
                .currency("USD")
                .build();

        MvcResult txResult = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(txReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String txId = objectMapper.readTree(txResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // 3. Ingest Batch Events (Initiated, Auth Approved, Capture Completed)
        Instant now = Instant.now();
        BatchIngestRequest batchReq = BatchIngestRequest.builder()
                .events(List.of(
                        IngestEventRequest.builder().eventId("evt-e2e-1").eventType("PAYMENT_INITIATED").sourceSystem("WEB").timestamp(now).sequenceNumber(1L).rawPayload("{\"cart\":1}").build(),
                        IngestEventRequest.builder().eventId("evt-e2e-2").eventType("AUTH_APPROVED").sourceSystem("STRIPE").timestamp(now.plusSeconds(1)).sequenceNumber(2L).rawPayload("{\"auth\":true}").build(),
                        IngestEventRequest.builder().eventId("evt-e2e-3").eventType("CAPTURE_COMPLETED").sourceSystem("LEDGER").timestamp(now.plusSeconds(2)).sequenceNumber(3L).rawPayload("{\"settled\":true}").build()
                ))
                .build();

        mockMvc.perform(post("/api/transactions/" + txId + "/events/batch")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchReq)))
                .andExpect(status().isCreated());

        // 4. Reconstruct Transaction
        mockMvc.perform(post("/api/transactions/" + txId + "/reconstruct")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inferredState").value("COMMITTED"))
                .andExpect(jsonPath("$.data.certaintyLevel").value("HIGH_CERTAINTY"));

        // 5. Inspect Evidence
        mockMvc.perform(get("/api/transactions/" + txId + "/evidence")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        // 6. Execute Sandboxed Replay (simulate conflicting event)
        ReplayScenarioRequest replayReq = ReplayScenarioRequest.builder()
                .scenarioType(ReplayScenarioType.CONFLICTING_EVENT)
                .build();

        mockMvc.perform(post("/api/transactions/" + txId + "/replay")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replayReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.simulatedState").value("CONFLICTED"))
                .andExpect(jsonPath("$.data.isSandboxed").value(true));

        // 7. Recovery Analysis
        mockMvc.perform(get("/api/transactions/" + txId + "/recovery")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.suggestedAction").value("NO_ACTION_REQUIRED"))
                .andExpect(jsonPath("$.data.safeToExecute").value(true));
    }
}

