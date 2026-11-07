export type TransactionStateEnum = 
  | 'INITIATED' 
  | 'PROCESSING' 
  | 'COMMITTED' 
  | 'FAILED' 
  | 'ROLLED_BACK' 
  | 'UNKNOWN' 
  | 'CONFLICTED';

export type CertaintyLevel = 
  | 'HIGH_CERTAINTY' 
  | 'CONDITIONAL_CERTAINTY' 
  | 'AMBIGUOUS' 
  | 'HIGH_UNCERTAINTY';

export type EventIntegrityStatus = 
  | 'VALID' 
  | 'CHECKSUM_MISMATCH' 
  | 'MALFORMED' 
  | 'OUT_OF_SYNC';

export type EvidenceType = 
  | 'VERIFIED_FACT' 
  | 'MISSING_STEP' 
  | 'CONFLICTING_SIGNAL' 
  | 'OUT_OF_ORDER_EVIDENCE' 
  | 'DUPLICATE_SIGNAL';

export type TransitionType = 
  | 'DETERMINISTIC' 
  | 'SPECULATIVE' 
  | 'RECOVERED';

export type ReplayScenarioType = 
  | 'MISSING_EVENT' 
  | 'DUPLICATE_EVENT' 
  | 'DELAYED_EVENT' 
  | 'OUT_OF_ORDER_EVENT' 
  | 'CONFLICTING_EVENT' 
  | 'DROP_AFTER_STEP';

export type MutationType = 
  | 'ORIGINAL' 
  | 'INJECTED_DUPLICATE' 
  | 'DELAYED' 
  | 'REORDERED' 
  | 'INJECTED_CONFLICT' 
  | 'DROPPED';

export type RecoveryActionType = 
  | 'NO_ACTION_REQUIRED' 
  | 'EXECUTE_COMPENSATING_REFUND' 
  | 'RESEND_CAPTURE_PROBE' 
  | 'FORCE_ROLLBACK' 
  | 'MANUAL_RECONCILIATION_REQUIRED' 
  | 'UNSAFE_CONFLICT_DETECTED';

export interface CreateTransactionRequest {
  externalId: string;
  title: string;
  channel?: string;
  amount?: number;
  currency?: string;
  metadataJson?: string;
}

export interface IngestEventRequest {
  eventId: string;
  eventType: string;
  sourceSystem: string;
  timestamp: string;
  sequenceNumber?: number;
  rawPayload: string;
}

export type EvidenceQuality = 
  | 'VERIFIED' 
  | 'INCOMPLETE' 
  | 'CONTRADICTORY' 
  | 'ANOMALOUS' 
  | 'NO_DATA';

export interface SystemStatusDto {
  status: 'ONLINE' | 'DEGRADED' | 'OFFLINE';
  engineVersion: string;
  databaseStatus: string;
  timestamp: string;
}

export interface UserDto {
  id: number;
  email: string;
  fullName: string;
  role: 'ROLE_ENGINEER' | 'ROLE_ADMIN';
}

export interface AuthResponse {
  token: string;
  user: UserDto;
}

export interface TransactionDto {
  id: string;
  externalId: string;
  title: string;
  channel: string;
  amount: number | null;
  currency: string;
  status: string;
  metadataJson: string | null;
  eventCount: number;
  latestState: TransactionStateEnum | null;
  latestCertainty: CertaintyLevel | null;
  latestConfidenceScore: number | null;
  hasConflict: boolean;
  evidenceQuality: EvidenceQuality | null;
  unresolvedIssuesCount: number;
  createdAt: string;
  updatedAt: string;
  investigatorEmail: string | null;
}

export interface TransactionEventDto {
  id: string;
  eventId: string;
  eventType: string;
  sourceSystem: string;
  timestamp: string;
  sequenceNumber: number | null;
  rawPayload: string;
  ingestionTimestamp: string;
  integrityStatus: EventIntegrityStatus;
}

export interface EvidenceRecordDto {
  id: string;
  evidenceType: EvidenceType;
  sourceEventId: string | null;
  description: string;
  details: string | null;
  confidenceWeight: number;
  detectedAt: string;
}

export interface InferredStateDto {
  id: string;
  inferredState: TransactionStateEnum;
  certaintyLevel: CertaintyLevel;
  confidenceScore: number;
  supportingEvents: string[];
  contradictingEvents: string[];
  missingEvidence: string[];
  reasoningExplanation: string;
  reconstructedAt: string;
}

export interface StateTransitionDto {
  id: string;
  fromState: string;
  toState: string;
  triggerEventId: string | null;
  transitionType: TransitionType;
  timestamp: string;
  notes: string | null;
}

export interface ReplayEventDto {
  id: string;
  originalEventId: string;
  eventType: string;
  sourceSystem: string;
  simulatedTimestamp: string;
  simulatedSequenceNumber: number | null;
  rawPayload: string;
  mutationType: MutationType;
}

export interface ReplayExecutionDto {
  id: string;
  originalTransactionId: string;
  scenarioType: ReplayScenarioType;
  scenarioDescription: string;
  targetEventId: string | null;
  delayMillis: number | null;
  baselineState: string;
  baselineCertainty: string;
  simulatedState: string;
  simulatedCertainty: string;
  simulatedConfidenceScore: number | null;
  findings: string;
  differencesSummary: string;
  status: 'PENDING' | 'COMPLETED' | 'FAILED';
  isSandboxed: boolean;
  createdAt: string;
  simulatedEvents: ReplayEventDto[];
}

export interface RecoveryAnalysisDto {
  id: string;
  suggestedAction: RecoveryActionType;
  safeToExecute: boolean;
  rationale: string;
  prerequisites: string[];
  missingPrerequisites: string[];
  risks: string[];
  analyzedAt: string;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}
