# StateSnap API Contract Specification

**Base URL**: `http://localhost:8080/api`  
**Authentication Scheme**: Bearer JWT (`Authorization: Bearer <token>`)

---

## 1. Authentication Endpoints

### 1.1 Register Engineer
- **POST** `/auth/register`
- **Request Body**:
```json
{
  "email": "engineer@statesnap.io",
  "password": "Password#2026!",
  "fullName": "Senior Forensic Engineer"
}
```
- **Response** (201 Created):
```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "user": {
      "id": 1,
      "email": "engineer@statesnap.io",
      "fullName": "Senior Forensic Engineer",
      "role": "ROLE_ENGINEER"
    }
  }
}
```

### 1.2 Login
- **POST** `/auth/login`
- **Request Body**:
```json
{
  "email": "engineer@statesnap.io",
  "password": "Password#2026!"
}
```
- **Response** (200 OK): Same structure as register.

### 1.3 Current User Info
- **GET** `/auth/me`
- **Response** (200 OK):
```json
{
  "success": true,
  "message": "Operation successful",
  "data": {
    "id": 1,
    "email": "engineer@statesnap.io",
    "fullName": "Senior Forensic Engineer",
    "role": "ROLE_ENGINEER"
  }
}
```

---

## 2. Transactions API

### 2.1 Register New Transaction
- **POST** `/transactions`
- **Request Body**:
```json
{
  "externalId": "TX-9901-ACQUIRER",
  "title": "Dual-Acquirer Card Checkout",
  "channel": "STRIPE_CHECKOUT",
  "amount": 499.00,
  "currency": "USD",
  "metadataJson": "{\"ip\": \"10.0.0.1\"}"
}
```
- **Response** (201 Created): Returns `TransactionDto`

### 2.2 List Transactions (with Filtering, Search & Pagination)
- **GET** `/transactions?status={status}&search={keyword}&page={0}&size={15}&sortBy={createdAt}&direction={desc}`
- **Response** (200 OK):
```json
{
  "success": true,
  "message": "Operation successful",
  "data": {
    "content": [
      {
        "id": "fefceb7a-8e85-4d5d-8013-291df2dc0b4d",
        "externalId": "TX-1001-CONFLICT",
        "title": "High-Value Wire Transfer - Cross-System Desync",
        "channel": "SWIFT_CORE_SETTLEMENT",
        "amount": 250000.00,
        "currency": "USD",
        "status": "CONFLICTED",
        "metadataJson": "{\"sourceIp\":\"192.168.1.42\"}",
        "eventCount": 4,
        "latestState": "CONFLICTED",
        "latestCertainty": "HIGH_UNCERTAINTY",
        "latestConfidenceScore": 33.0,
        "hasConflict": true,
        "createdAt": "2026-11-03T07:01:40.851Z",
        "updatedAt": "2026-11-03T07:01:40.851Z",
        "investigatorEmail": "engineer@statesnap.io"
      }
    ],
    "page": 0,
    "size": 15,
    "totalElements": 4,
    "totalPages": 1,
    "last": true
  }
}
```

### 2.3 Get Transaction Details
- **GET** `/transactions/{id}`
- **Response** (200 OK): Returns single `TransactionDto`

---

## 3. Events Ingestion API

### 3.1 Single Event Ingestion
- **POST** `/transactions/{transactionId}/events`
- **Request Body**:
```json
{
  "eventId": "EVT-9901-01",
  "eventType": "PAYMENT_INITIATED",
  "sourceSystem": "CHECKOUT_WEB",
  "timestamp": "2026-11-03T07:00:00Z",
  "sequenceNumber": 1,
  "rawPayload": "{\"orderId\":\"ORD-12345\"}"
}
```

### 3.2 Batch Event Ingestion
- **POST** `/transactions/{transactionId}/events/batch`
- **Request Body**:
```json
{
  "events": [
    {
      "eventId": "EVT-9901-01",
      "eventType": "PAYMENT_INITIATED",
      "sourceSystem": "CHECKOUT_WEB",
      "timestamp": "2026-11-03T07:00:00Z",
      "sequenceNumber": 1,
      "rawPayload": "{\"orderId\":\"ORD-12345\"}"
    }
  ]
}
```

### 3.3 List Ordered Events
- **GET** `/transactions/{transactionId}/events`
- **Response** (200 OK):
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid-string",
      "eventId": "EVT-1001-01",
      "eventType": "PAYMENT_INITIATED",
      "sourceSystem": "PORTAL_GATEWAY",
      "timestamp": "2026-11-03T07:00:01Z",
      "sequenceNumber": 1,
      "rawPayload": "{\"amount\": 250000.00}",
      "ingestionTimestamp": "2026-11-03T07:01:00Z",
      "integrityStatus": "VALID"
    }
  ]
}
```

---

## 4. State Reconstruction & Evidence API

### 4.1 Trigger Deterministic Reconstruction
- **POST** `/transactions/{transactionId}/reconstruct`
- **Response** (200 OK): Returns `InferredStateDto`

### 4.2 Get Latest Inferred State
- **GET** `/transactions/{transactionId}/reconstruction`
- **Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "id": "uuid-string",
    "inferredState": "CONFLICTED",
    "certaintyLevel": "HIGH_UNCERTAINTY",
    "confidenceScore": 33.0,
    "supportingEvents": ["EVT-1001-01", "EVT-1001-02", "EVT-1001-04"],
    "contradictingEvents": ["EVT-1001-03"],
    "missingEvidence": ["Missing prerequisite: AUTH_APPROVED prior to CAPTURE_COMPLETED"],
    "reasoningExplanation": "### State Reconstruction Assessment\n\n**Inferred State**: `CONFLICTED`...",
    "reconstructedAt": "2026-11-03T07:01:40.851Z"
  }
}
```

### 4.3 Get Evidence Records
- **GET** `/transactions/{transactionId}/evidence`
- **Response** (200 OK):
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid-string",
      "evidenceType": "VERIFIED_FACT",
      "sourceEventId": "EVT-1001-01",
      "description": "Verified log: PAYMENT_INITIATED from source [PORTAL_GATEWAY]",
      "details": "Timestamp: ..., Sequence: 1",
      "confidenceWeight": 1.0,
      "detectedAt": "2026-11-03T07:01:40.851Z"
    },
    {
      "id": "uuid-string-2",
      "evidenceType": "CONFLICTING_SIGNAL",
      "sourceEventId": null,
      "description": "Severe Conflict: Dual contradictory outcomes...",
      "details": "Ledger/Capture indicates funds captured, while Gateway/Auth claims failure.",
      "confidenceWeight": 0.9,
      "detectedAt": "2026-11-03T07:01:40.851Z"
    }
  ]
}
```

### 4.4 Get State Transitions Timeline
- **GET** `/transactions/{transactionId}/transitions`
- **Response** (200 OK):
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "fromState": "INITIAL",
      "toState": "INITIATED",
      "triggerEventId": "EVT-1001-01",
      "transitionType": "DETERMINISTIC",
      "timestamp": "2026-11-03T07:00:01Z",
      "notes": "Transition triggered by PAYMENT_INITIATED"
    }
  ]
}
```

---

## 5. Sandboxed Fault-Injection Replay API

### 5.1 Execute Sandboxed Replay
- **POST** `/transactions/{transactionId}/replay`
- **Request Body**:
```json
{
  "scenarioType": "CONFLICTING_EVENT",
  "targetEventId": "EVT-1004-03",
  "delayMillis": 3000,
  "conflictingPayload": "{\"reason\": \"Simulated acquirer decline\"}"
}
```
*Valid `scenarioType` values*: `MISSING_EVENT`, `DUPLICATE_EVENT`, `DELAYED_EVENT`, `OUT_OF_ORDER_EVENT`, `CONFLICTING_EVENT`, `DROP_AFTER_STEP`

- **Response** (201 Created):
```json
{
  "success": true,
  "data": {
    "id": "replay-exec-uuid",
    "originalTransactionId": "f5884d7c-37f5-4aa0-84e6-6bd62f51c27f",
    "scenarioType": "CONFLICTING_EVENT",
    "scenarioDescription": "Simulating conflicting cross-system signal...",
    "targetEventId": "EVT-1004-03",
    "delayMillis": 3000,
    "baselineState": "COMMITTED",
    "baselineCertainty": "HIGH_CERTAINTY",
    "simulatedState": "CONFLICTED",
    "simulatedCertainty": "HIGH_UNCERTAINTY",
    "simulatedConfidenceScore": 33.0,
    "findings": "### Fault-Injection Replay Findings...",
    "differencesSummary": "Divergence: baseline=COMMITTED, simulated=CONFLICTED",
    "status": "COMPLETED",
    "isSandboxed": true,
    "createdAt": "2026-11-03T07:04:00Z",
    "simulatedEvents": [
      {
        "id": "uuid",
        "originalEventId": "EVT-1004-01",
        "eventType": "PAYMENT_INITIATED",
        "sourceSystem": "PAYOUT_SERVICE",
        "simulatedTimestamp": "2026-11-03T07:00:00Z",
        "simulatedSequenceNumber": 1,
        "rawPayload": "{...}",
        "mutationType": "ORIGINAL"
      },
      {
        "id": "uuid-injected",
        "originalEventId": "sim_conflict_a81",
        "eventType": "AUTH_FAILED",
        "sourceSystem": "SANDBOX_EXTERNAL_GATEWAY",
        "simulatedTimestamp": "2026-11-03T07:04:00Z",
        "simulatedSequenceNumber": 14,
        "rawPayload": "{...}",
        "mutationType": "INJECTED_CONFLICT"
      }
    ]
  }
}
```

### 5.2 List Replay Executions
- **GET** `/transactions/{transactionId}/replays`
- **Response** (200 OK): Array of `ReplayExecutionDto`

### 5.3 Get Specific Replay Execution
- **GET** `/replay/{executionId}`
- **Response** (200 OK): Single `ReplayExecutionDto`

---

## 6. Recovery Analysis API

### 6.1 Get Latest Recovery Analysis
- **GET** `/transactions/{transactionId}/recovery`
- **Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "suggestedAction": "UNSAFE_CONFLICT_DETECTED",
    "safeToExecute": false,
    "rationale": "CRITICAL: Contradictory evidence detected in cross-system event streams. Automated recovery is strictly blocked...",
    "prerequisites": [
      "Manual cross-system ledger reconciliation",
      "Direct bank/processor settlement export review",
      "Senior engineer or ops sign-off"
    ],
    "missingPrerequisites": [
      "Independent processor settlement confirmation"
    ],
    "risks": [
      "Automated refund could cause irreversible loss if capture succeeded out-of-band",
      "Retrying capture could double-bill the cardholder"
    ],
    "analyzedAt": "2026-11-03T07:01:40.851Z"
  }
}
```
*Valid `suggestedAction` values*: `NO_ACTION_REQUIRED`, `EXECUTE_COMPENSATING_REFUND`, `RESEND_CAPTURE_PROBE`, `FORCE_ROLLBACK`, `MANUAL_RECONCILIATION_REQUIRED`, `UNSAFE_CONFLICT_DETECTED`

### 6.2 Trigger Fresh Recovery Analysis
- **POST** `/transactions/{transactionId}/recovery/analyze`
- **Response** (200 OK): Returns freshly analyzed `RecoveryAnalysisDto`

