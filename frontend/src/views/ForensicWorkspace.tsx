import React, { useState, useEffect } from 'react';
import {
  EvidenceRecordDto,
  InferredStateDto,
  RecoveryAnalysisDto,
  ReplayExecutionDto,
  ReplayScenarioType,
  StateTransitionDto,
  TransactionDto,
  TransactionEventDto
} from '../types';
import { apiClient } from '../api/client';
import { StatusBadge } from '../components/StatusBadge';
import { EvidenceQualityBadge } from '../components/EvidenceQualityBadge';
import { EventTimeline } from '../components/EventTimeline';
import { InferenceExplanationPanel } from '../components/InferenceExplanationPanel';
import { EvidenceExplorer } from '../components/EvidenceExplorer';
import { RecoveryPanel } from '../components/RecoveryPanel';
import { ReplayLab } from '../components/ReplayLab';
import {
  ArrowLeft,
  RefreshCw,
  PlusCircle,
  FlaskConical,
  ShieldCheck,
  Layers,
  Clock,
  Code,
  SlidersHorizontal,
  X
} from 'lucide-react';

interface Props {
  transaction: TransactionDto;
  onBack: () => void;
  initialTab?: 'trace' | 'replay' | 'recovery';
}

export const ForensicWorkspace: React.FC<Props> = ({
  transaction,
  onBack,
  initialTab = 'trace',
}) => {
  const [activeTab, setActiveTab] = useState<'trace' | 'replay' | 'recovery'>(initialTab);
  const [events, setEvents] = useState<TransactionEventDto[]>([]);
  const [inferredState, setInferredState] = useState<InferredStateDto | null>(null);
  const [evidenceList, setEvidenceList] = useState<EvidenceRecordDto[]>([]);
  const [transitions, setTransitions] = useState<StateTransitionDto[]>([]);
  const [recovery, setRecovery] = useState<RecoveryAnalysisDto | null>(null);
  const [replays, setReplays] = useState<ReplayExecutionDto[]>([]);

  const [loading, setLoading] = useState<boolean>(true);
  const [isReconstructing, setIsReconstructing] = useState<boolean>(false);
  const [isReplaying, setIsReplaying] = useState<boolean>(false);
  const [isAnalyzingRecovery, setIsAnalyzingRecovery] = useState<boolean>(false);
  const [selectedEventId, setSelectedEventId] = useState<string | null>(null);

  // Ingest Event Modal State
  const [showIngestModal, setShowIngestModal] = useState<boolean>(false);
  const [newEventId, setNewEventId] = useState<string>('');
  const [newEventType, setNewEventType] = useState<string>('AUTH_APPROVED');
  const [newSourceSystem, setNewSourceSystem] = useState<string>('PAYMENT_GATEWAY');
  const [newRawPayload, setNewRawPayload] = useState<string>('{"status": "SUCCESS", "authCode": "APP_9921"}');
  const [isIngesting, setIsIngesting] = useState<boolean>(false);

  const loadAllData = async () => {
    try {
      setLoading(true);
      const [eventsRes, replaysRes] = await Promise.all([
        apiClient.events.list(transaction.id),
        apiClient.replay.list(transaction.id),
      ]);
      setEvents(eventsRes);
      setReplays(replaysRes);

      try {
        const [reconRes, evidenceRes, transRes, recoveryRes] = await Promise.all([
          apiClient.reconstruction.getLatest(transaction.id),
          apiClient.reconstruction.getEvidence(transaction.id),
          apiClient.reconstruction.getTransitions(transaction.id),
          apiClient.recovery.get(transaction.id),
        ]);
        setInferredState(reconRes);
        setEvidenceList(evidenceRes);
        setTransitions(transRes);
        setRecovery(recoveryRes);
      } catch (err) {
        // May not be reconstructed yet, that's fine
      }
    } catch (err) {
      console.error('Failed to load transaction trace data:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAllData();
  }, [transaction.id]);

  const handleSelectEvent = (id: string) => {
    setSelectedEventId(id);
    const el = document.getElementById(`evt-${id}`);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  };

  const handleTriggerReconstruct = async () => {
    try {
      setIsReconstructing(true);
      const res = await apiClient.reconstruction.reconstruct(transaction.id);
      setInferredState(res);
      const [evidenceRes, transRes, recoveryRes] = await Promise.all([
        apiClient.reconstruction.getEvidence(transaction.id),
        apiClient.reconstruction.getTransitions(transaction.id),
        apiClient.recovery.analyze(transaction.id),
      ]);
      setEvidenceList(evidenceRes);
      setTransitions(transRes);
      setRecovery(recoveryRes);
    } catch (err) {
      console.error('Reconstruction failed:', err);
    } finally {
      setIsReconstructing(false);
    }
  };

  const handleExecuteReplay = async (
    scenario: ReplayScenarioType,
    targetEventId?: string,
    delayMillis?: number,
    conflictingPayload?: string
  ) => {
    try {
      setIsReplaying(true);
      const res = await apiClient.replay.execute(transaction.id, {
        scenarioType: scenario,
        targetEventId,
        delayMillis,
        conflictingPayload,
      });
      setReplays((prev) => [res, ...prev]);
    } catch (err) {
      console.error('Replay execution failed:', err);
    } finally {
      setIsReplaying(false);
    }
  };

  const handleRefreshRecovery = async () => {
    try {
      setIsAnalyzingRecovery(true);
      const res = await apiClient.recovery.analyze(transaction.id);
      setRecovery(res);
    } catch (err) {
      console.error('Recovery analysis failed:', err);
    } finally {
      setIsAnalyzingRecovery(false);
    }
  };

  const handleIngestSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setIsIngesting(true);
      const eventId = newEventId.trim() || `EVT-${Date.now().toString().slice(-6)}`;
      await apiClient.events.ingest(transaction.id, {
        eventId,
        eventType: newEventType.trim().toUpperCase(),
        sourceSystem: newSourceSystem.trim(),
        timestamp: new Date().toISOString(),
        rawPayload: newRawPayload.trim() || '{}',
      });
      setShowIngestModal(false);
      setNewEventId('');
      await loadAllData();
      await handleTriggerReconstruct();
    } catch (err) {
      console.error('Event ingestion failed:', err);
    } finally {
      setIsIngesting(false);
    }
  };

  return (
    <div className="space-y-6">
      
      {/* Workspace Header Bar */}
      <div className="p-4 rounded border border-console-border bg-console-900/80 backdrop-blur space-y-4">
        
        <div className="flex items-center justify-between flex-wrap gap-4">
          
          <div className="flex items-center gap-3">
            <button
              onClick={onBack}
              title="Return to investigations list"
              className="p-1.5 rounded bg-console-850 hover:bg-console-800 text-console-muted hover:text-white border border-console-border transition-colors"
            >
              <ArrowLeft className="w-4 h-4" />
            </button>

            <div>
              <div className="flex items-center gap-2.5 flex-wrap">
                <h1 className="text-base font-mono font-bold text-white tracking-wide">
                  {transaction.externalId}
                </h1>
                <StatusBadge state={inferredState?.inferredState || transaction.status} size="md" />
                <EvidenceQualityBadge
                  quality={transaction.evidenceQuality}
                  unresolvedCount={transaction.unresolvedIssuesCount}
                  size="md"
                />
                <span className="text-xs font-mono text-console-dim px-2 py-0.5 rounded bg-console-950 border border-console-borderSubtle">
                  {transaction.channel || 'DEFAULT'}
                </span>
              </div>
              <p className="text-xs font-mono text-console-muted mt-0.5">{transaction.title}</p>
            </div>
          </div>

          {/* Quick Actions */}
          <div className="flex items-center gap-2">
            <button
              onClick={() => setShowIngestModal(true)}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded bg-console-850 hover:bg-console-800 text-indigo-300 border border-indigo-500/30 text-xs font-mono transition-all"
            >
              <PlusCircle className="w-3.5 h-3.5" />
              <span>Ingest Log Event</span>
            </button>

            <button
              onClick={handleTriggerReconstruct}
              disabled={isReconstructing}
              className="flex items-center gap-1.5 px-3.5 py-1.5 rounded bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-mono font-medium shadow-[0_0_12px_rgba(99,102,241,0.25)] transition-all"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isReconstructing ? 'animate-spin' : ''}`} />
              <span>{isReconstructing ? 'Reconstructing...' : 'Reconstruct State'}</span>
            </button>
          </div>

        </div>

        {/* View Mode Tabs */}
        <div className="flex items-center gap-2 border-t border-console-borderSubtle pt-3">
          <button
            onClick={() => setActiveTab('trace')}
            className={`flex items-center gap-2 px-3 py-1.5 rounded text-xs font-mono font-semibold transition-all ${
              activeTab === 'trace'
                ? 'bg-console-800 text-white border border-console-border'
                : 'text-console-muted hover:text-white hover:bg-console-850'
            }`}
          >
            <Layers className="w-3.5 h-3.5 text-indigo-400" />
            <span>Forensic Event Trace</span>
            <span className="text-[10px] px-1.5 py-0.2 rounded bg-console-950 text-console-dim">
              {events.length}
            </span>
          </button>

          <button
            onClick={() => setActiveTab('replay')}
            className={`flex items-center gap-2 px-3 py-1.5 rounded text-xs font-mono font-semibold transition-all ${
              activeTab === 'replay'
                ? 'bg-cyan-950/60 text-cyan-300 border border-cyan-500/40 shadow-[0_0_12px_rgba(6,182,212,0.15)]'
                : 'text-console-muted hover:text-cyan-300 hover:bg-console-850'
            }`}
          >
            <FlaskConical className="w-3.5 h-3.5 text-cyan-400" />
            <span>Sandboxed Replay Lab</span>
            {replays.length > 0 && (
              <span className="text-[10px] px-1.5 py-0.2 rounded bg-cyan-950 text-cyan-300">
                {replays.length}
              </span>
            )}
          </button>

          <button
            onClick={() => setActiveTab('recovery')}
            className={`flex items-center gap-2 px-3 py-1.5 rounded text-xs font-mono font-semibold transition-all ${
              activeTab === 'recovery'
                ? 'bg-indigo-950/60 text-indigo-300 border border-indigo-500/40'
                : 'text-console-muted hover:text-indigo-300 hover:bg-console-850'
            }`}
          >
            <ShieldCheck className="w-3.5 h-3.5 text-indigo-400" />
            <span>Recovery Advisor</span>
          </button>
        </div>

      </div>

      {/* Main Workspace Body */}
      {loading ? (
        <div className="p-16 text-center text-xs font-mono text-console-dim space-y-2">
          <RefreshCw className="w-6 h-6 animate-spin mx-auto text-indigo-400" />
          <p>Compiling forensic telemetry and state assertions...</p>
        </div>
      ) : activeTab === 'replay' ? (
        <ReplayLab
          transaction={transaction}
          events={events}
          replayExecutions={replays}
          onExecuteReplay={handleExecuteReplay}
          isExecuting={isReplaying}
        />
      ) : activeTab === 'recovery' ? (
        <RecoveryPanel
          recovery={recovery}
          onRefresh={handleRefreshRecovery}
          isLoading={isAnalyzingRecovery}
        />
      ) : (
        /* Dual Panel Forensic Investigation Layout */
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          
          {/* Left Column: Event Stream Timeline (7 Cols) */}
          <div className="lg:col-span-7 space-y-4">
            <EventTimeline
              events={events}
              evidenceList={evidenceList}
              selectedEventId={selectedEventId}
              onSelectEvent={handleSelectEvent}
            />
          </div>

          {/* Right Column: Inferred State & Evidence Explorer (5 Cols) */}
          <div className="lg:col-span-5 space-y-6">
            <InferenceExplanationPanel
              inferredState={inferredState}
              onSelectEvent={handleSelectEvent}
              onTriggerReconstruct={handleTriggerReconstruct}
              isReconstructing={isReconstructing}
            />

            <EvidenceExplorer
              evidenceList={evidenceList}
              onSelectEvent={handleSelectEvent}
            />
          </div>

        </div>
      )}

      {/* Ingest Event Modal Dialog */}
      {showIngestModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
          <div className="w-full max-w-lg rounded-lg border border-console-border bg-console-900 shadow-2xl p-6 space-y-4">
            <div className="flex items-center justify-between border-b border-console-borderSubtle pb-3">
              <div className="flex items-center gap-2">
                <Code className="w-4 h-4 text-indigo-400" />
                <h3 className="text-sm font-mono font-bold text-white">Ingest Raw Telemetric Event</h3>
              </div>
              <button
                onClick={() => setShowIngestModal(false)}
                className="text-console-dim hover:text-white"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleIngestSubmit} className="space-y-4">
              <div>
                <label className="text-xs font-mono text-console-muted block mb-1">
                  Event ID (e.g. EVT-AUTH-882):
                </label>
                <input
                  type="text"
                  value={newEventId}
                  onChange={(e) => setNewEventId(e.target.value)}
                  placeholder="Auto-generated if blank"
                  className="w-full px-3 py-2 rounded bg-console-950 border border-console-border text-xs font-mono text-white focus:outline-none focus:border-indigo-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-mono text-console-muted block mb-1">
                    Event Type:
                  </label>
                  <select
                    value={newEventType}
                    onChange={(e) => setNewEventType(e.target.value)}
                    className="w-full px-3 py-2 rounded bg-console-950 border border-console-border text-xs font-mono text-white focus:outline-none focus:border-indigo-500"
                  >
                    <option value="PAYMENT_INITIATED">PAYMENT_INITIATED</option>
                    <option value="AUTH_REQUESTED">AUTH_REQUESTED</option>
                    <option value="AUTH_APPROVED">AUTH_APPROVED</option>
                    <option value="AUTH_FAILED">AUTH_FAILED</option>
                    <option value="CAPTURE_TRIGGERED">CAPTURE_TRIGGERED</option>
                    <option value="CAPTURE_COMPLETED">CAPTURE_COMPLETED</option>
                    <option value="LEDGER_COMMITTED">LEDGER_COMMITTED</option>
                    <option value="REFUND_REQUESTED">REFUND_REQUESTED</option>
                    <option value="ROLLBACK_COMPLETED">ROLLBACK_COMPLETED</option>
                    <option value="TIMEOUT_EXPIRED">TIMEOUT_EXPIRED</option>
                  </select>
                </div>

                <div>
                  <label className="text-xs font-mono text-console-muted block mb-1">
                    Source System:
                  </label>
                  <input
                    type="text"
                    value={newSourceSystem}
                    onChange={(e) => setNewSourceSystem(e.target.value)}
                    required
                    className="w-full px-3 py-2 rounded bg-console-950 border border-console-border text-xs font-mono text-white focus:outline-none focus:border-indigo-500"
                  />
                </div>
              </div>

              <div>
                <label className="text-xs font-mono text-console-muted block mb-1">
                  Raw JSON Payload:
                </label>
                <textarea
                  rows={4}
                  value={newRawPayload}
                  onChange={(e) => setNewRawPayload(e.target.value)}
                  className="w-full px-3 py-2 rounded bg-console-950 border border-console-border text-xs font-mono text-emerald-300 focus:outline-none focus:border-indigo-500"
                />
              </div>

              <div className="flex items-center justify-end gap-2 pt-2 border-t border-console-borderSubtle">
                <button
                  type="button"
                  onClick={() => setShowIngestModal(false)}
                  className="px-3 py-1.5 rounded text-xs font-mono text-console-muted hover:text-white"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isIngesting}
                  className="px-4 py-1.5 rounded bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-mono font-medium shadow-[0_0_12px_rgba(99,102,241,0.25)]"
                >
                  {isIngesting ? 'Ingesting...' : 'Ingest & Reconstruct'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
};

