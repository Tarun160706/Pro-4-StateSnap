package com.statesnap.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.statesnap.entity.*;
import com.statesnap.repository.*;
import com.statesnap.service.ReconstructionService;
import com.statesnap.service.RecoveryService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionEventRepository eventRepository;
    private final PasswordEncoder passwordEncoder;
    private final ReconstructionService reconstructionService;
    private final RecoveryService recoveryService;

    public DataInitializer(
            UserRepository userRepository,
            TransactionRepository transactionRepository,
            TransactionEventRepository eventRepository,
            PasswordEncoder passwordEncoder,
            ReconstructionService reconstructionService,
            RecoveryService recoveryService
    ) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.eventRepository = eventRepository;
        this.passwordEncoder = passwordEncoder;
        this.reconstructionService = reconstructionService;
        this.recoveryService = recoveryService;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        // Create default engineer user
        User engineer = User.builder()
                .email("engineer@statesnap.io")
                .password(passwordEncoder.encode("Engineer#2026!"))
                .fullName("Lead Forensic Engineer")
                .role(Role.ROLE_ENGINEER)
                .createdAt(Instant.now())
                .build();
        engineer = userRepository.save(engineer);

        Instant baseTime = Instant.now().minus(2, ChronoUnit.HOURS);

        // Case 1: TX-1001-CONFLICT (Severe contradictory signal between gateway and ledger)
        TransactionRecord tx1 = TransactionRecord.builder()
                .id(UUID.randomUUID().toString())
                .externalId("TX-1001-CONFLICT")
                .title("High-Value Wire Transfer - Cross-System Desync")
                .channel("SWIFT_CORE_SETTLEMENT")
                .amount(new BigDecimal("250000.00"))
                .currency("USD")
                .status("INVESTIGATION_PENDING")
                .metadataJson("{\"sourceIp\":\"192.168.1.42\",\"complianceCheck\":\"PASS\",\"clientTier\":\"INSTITUTIONAL\"}")
                .investigator(engineer)
                .createdAt(baseTime)
                .updatedAt(baseTime)
                .build();
        tx1 = transactionRepository.save(tx1);

        eventRepository.save(TransactionEvent.builder()
                .id(UUID.randomUUID().toString()).transaction(tx1)
                .eventId("EVT-1001-01").eventType("PAYMENT_INITIATED").sourceSystem("PORTAL_GATEWAY")
                .timestamp(baseTime.plus(1, ChronoUnit.SECONDS)).sequenceNumber(1L)
                .rawPayload("{\"amount\":250000.00,\"currency\":\"USD\",\"payer\":\"Acc-98124\",\"beneficiary\":\"Acc-10294\"}")
                .integrityStatus(EventIntegrityStatus.VALID).build());

        eventRepository.save(TransactionEvent.builder()
                .id(UUID.randomUUID().toString()).transaction(tx1)
                .eventId("EVT-1001-02").eventType("AUTH_REQUESTED").sourceSystem("ROUTING_SERVICE")
                .timestamp(baseTime.plus(3, ChronoUnit.SECONDS)).sequenceNumber(2L)
                .rawPayload("{\"riskScore\":12,\"idempotencyKey\":\"idemp_wire_250k_01\"}")
                .integrityStatus(EventIntegrityStatus.VALID).build());

        eventRepository.save(TransactionEvent.builder()
                .id(UUID.randomUUID().toString()).transaction(tx1)
                .eventId("EVT-1001-03").eventType("AUTH_FAILED").sourceSystem("CARD_NETWORK_ACQUIRER")
                .timestamp(baseTime.plus(5, ChronoUnit.SECONDS)).sequenceNumber(3L)
                .rawPayload("{\"reason\":\"504_UPSTREAM_TIMEOUT_OR_REVOKED\",\"networkCode\":\"ERR_TIMED_OUT\"}")
                .integrityStatus(EventIntegrityStatus.VALID).build());

        eventRepository.save(TransactionEvent.builder()
                .id(UUID.randomUUID().toString()).transaction(tx1)
                .eventId("EVT-1001-04").eventType("LEDGER_COMMITTED").sourceSystem("CORE_BANKING_LEDGER")
                .timestamp(baseTime.plus(7, ChronoUnit.SECONDS)).sequenceNumber(4L)
                .rawPayload("{\"ledgerEntryId\":\"LEDG-998124\",\"debitPosted\":true,\"balanceUpdate\":\"CONFIRMED\"}")
                .integrityStatus(EventIntegrityStatus.VALID).build());

        reconstructionService.reconstructTransaction(tx1.getId());
        recoveryService.analyzeRecovery(tx1.getId());

        // Case 2: TX-1002-GAP (Downstream Capture completed without upstream Auth approved)
        TransactionRecord tx2 = TransactionRecord.builder()
                .id(UUID.randomUUID().toString())
                .externalId("TX-1002-GAP")
                .title("E-Commerce Order Checkout - Dropped Authorization Callback")
                .channel("STRIPE_CARD_PRESENT")
                .amount(new BigDecimal("184.50"))
                .currency("USD")
                .status("INVESTIGATION_PENDING")
                .metadataJson("{\"cartItems\":3,\"deviceFingerprint\":\"fp_a98e21\"}")
                .investigator(engineer)
                .createdAt(baseTime.plus(20, ChronoUnit.MINUTES))
                .updatedAt(baseTime.plus(20, ChronoUnit.MINUTES))
                .build();
        tx2 = transactionRepository.save(tx2);

        eventRepository.save(TransactionEvent.builder()
                .id(UUID.randomUUID().toString()).transaction(tx2)
                .eventId("EVT-1002-01").eventType("PAYMENT_INITIATED").sourceSystem("CHECKOUT_WEB")
                .timestamp(baseTime.plus(20, ChronoUnit.MINUTES)).sequenceNumber(1L)
                .rawPayload("{\"orderId\":\"ORD-8812\",\"merchantId\":\"m_retail_01\"}")
                .integrityStatus(EventIntegrityStatus.VALID).build());

        eventRepository.save(TransactionEvent.builder()
                .id(UUID.randomUUID().toString()).transaction(tx2)
                .eventId("EVT-1002-02").eventType("CAPTURE_COMPLETED").sourceSystem("PAYMENT_SETTLEMENT_SERVICE")
                .timestamp(baseTime.plus(21, ChronoUnit.MINUTES)).sequenceNumber(2L)
                .rawPayload("{\"settlementBatch\":\"BATCH-2026-11-03\",\"fundsSettled\":184.50}")
                .integrityStatus(EventIntegrityStatus.VALID).build());

        reconstructionService.reconstructTransaction(tx2.getId());
        recoveryService.analyzeRecovery(tx2.getId());

        // Case 3: TX-1003-STALLED (Stalled in-flight authorization)
        TransactionRecord tx3 = TransactionRecord.builder()
                .id(UUID.randomUUID().toString())
                .externalId("TX-1003-STALLED")
                .title("Subscription Auto-Renewal - In-Flight Network Drop")
                .channel("RECURRING_BILLING_ENGINE")
                .amount(new BigDecimal("49.99"))
                .currency("USD")
                .status("INVESTIGATION_PENDING")
                .metadataJson("{\"subscriptionTier\":\"PRO_ANNUAL\"}")
                .investigator(engineer)
                .createdAt(baseTime.plus(45, ChronoUnit.MINUTES))
                .updatedAt(baseTime.plus(45, ChronoUnit.MINUTES))
                .build();
        tx3 = transactionRepository.save(tx3);

        eventRepository.save(TransactionEvent.builder()
                .id(UUID.randomUUID().toString()).transaction(tx3)
                .eventId("EVT-1003-01").eventType("PAYMENT_INITIATED").sourceSystem("BILLING_CRON")
                .timestamp(baseTime.plus(45, ChronoUnit.MINUTES)).sequenceNumber(1L)
                .rawPayload("{\"cycle\":\"2026-11\",\"plan\":\"PRO\"}")
                .integrityStatus(EventIntegrityStatus.VALID).build());

        eventRepository.save(TransactionEvent.builder()
                .id(UUID.randomUUID().toString()).transaction(tx3)
                .eventId("EVT-1003-02").eventType("AUTH_REQUESTED").sourceSystem("PAYMENT_DISPATCHER")
                .timestamp(baseTime.plus(46, ChronoUnit.MINUTES)).sequenceNumber(2L)
                .rawPayload("{\"gatewayEndpoint\":\"https://api.paymenthub.internal/v1/charge\"}")
                .integrityStatus(EventIntegrityStatus.VALID).build());

        reconstructionService.reconstructTransaction(tx3.getId());
        recoveryService.analyzeRecovery(tx3.getId());

        // Case 4: TX-1004-COMMITTED (Healthy complete transaction)
        TransactionRecord tx4 = TransactionRecord.builder()
                .id(UUID.randomUUID().toString())
                .externalId("TX-1004-COMMITTED")
                .title("Merchant Settlement Payout - Verified Milestone Flow")
                .channel("ACH_DIRECT_DEPOSIT")
                .amount(new BigDecimal("12450.00"))
                .currency("USD")
                .status("INVESTIGATION_PENDING")
                .metadataJson("{\"payoutBatch\":\"PAYOUT-902\",\"clearingCycle\":\"SAME_DAY\"}")
                .investigator(engineer)
                .createdAt(baseTime.plus(60, ChronoUnit.MINUTES))
                .updatedAt(baseTime.plus(60, ChronoUnit.MINUTES))
                .build();
        tx4 = transactionRepository.save(tx4);

        eventRepository.save(TransactionEvent.builder()
                .id(UUID.randomUUID().toString()).transaction(tx4)
                .eventId("EVT-1004-01").eventType("PAYMENT_INITIATED").sourceSystem("PAYOUT_SERVICE")
                .timestamp(baseTime.plus(60, ChronoUnit.MINUTES)).sequenceNumber(1L)
                .rawPayload("{\"recipient\":\"Merchant-LLC-412\"}")
                .integrityStatus(EventIntegrityStatus.VALID).build());

        eventRepository.save(TransactionEvent.builder()
                .id(UUID.randomUUID().toString()).transaction(tx4)
                .eventId("EVT-1004-02").eventType("AUTH_REQUESTED").sourceSystem("FED_ROUTING")
                .timestamp(baseTime.plus(61, ChronoUnit.MINUTES)).sequenceNumber(2L)
                .rawPayload("{\"routingNumber\":\"021000021\"}")
                .integrityStatus(EventIntegrityStatus.VALID).build());

        eventRepository.save(TransactionEvent.builder()
                .id(UUID.randomUUID().toString()).transaction(tx4)
                .eventId("EVT-1004-03").eventType("AUTH_APPROVED").sourceSystem("FED_GATEWAY")
                .timestamp(baseTime.plus(62, ChronoUnit.MINUTES)).sequenceNumber(3L)
                .rawPayload("{\"approvalCode\":\"ACH_APP_8812\",\"riskCheck\":\"LOW\"}")
                .integrityStatus(EventIntegrityStatus.VALID).build());

        eventRepository.save(TransactionEvent.builder()
                .id(UUID.randomUUID().toString()).transaction(tx4)
                .eventId("EVT-1004-04").eventType("CAPTURE_COMPLETED").sourceSystem("SETTLEMENT_CORE")
                .timestamp(baseTime.plus(63, ChronoUnit.MINUTES)).sequenceNumber(4L)
                .rawPayload("{\"traceNumber\":\"TRC-9912048\",\"reconciled\":true}")
                .integrityStatus(EventIntegrityStatus.VALID).build());

        reconstructionService.reconstructTransaction(tx4.getId());
        recoveryService.analyzeRecovery(tx4.getId());
    }
}

