import React, { useState } from 'react';
import {
  ReplayExecutionDto,
  ReplayScenarioType,
  TransactionDto,
  TransactionEventDto
} from '../types';
import { StatusBadge } from './StatusBadge';
import {
  Play,
  FlaskConical,
  ShieldCheck,
  ShieldAlert,
  ArrowRight,
  Clock,
  Layers,
  Zap,
  Lock,
  AlertTriangle
} from 'lucide-react';

interface Props {
  transaction: TransactionDto;
  events: TransactionEventDto[];
  replayExecutions: ReplayExecutionDto[];
  onExecuteReplay: (
    scenario: ReplayScenarioType,
    targetEventId?: string,
    delayMillis?: number,
    conflictingPayload?: string
  ) => Promise<void>;
  isExecuting: boolean;
}

export const ReplayLab: React.FC<Props> = ({
  transaction,
  events,
  replayExecutions,
  onExecuteReplay,
  isExecuting,
}) => {
  const [selectedScenario, setSelectedScenario] = useState<ReplayScenarioType>('CONFLICTING_EVENT');
  const [targetEventId, setTargetEventId] = useState<string>('');
  const [delayMillis, setDelayMillis] = useState<number>(3000);
  const [simulationError, setSimulationError] = useState<string | null>(null);
  const [activeReplay, setActiveReplay] = useState<ReplayExecutionDto | null>(
    replayExecutions.length > 0 ? replayExecutions[0] : null
  );

  const handleSimulate = async () => {
    try {
      setSimulationError(null);
      await onExecuteReplay(
        selectedScenario,
        targetEventId || undefined,
        delayMillis,
        undefined
      );
    } catch (err: any) {
      setSimulationError(err?.response?.data?.message || err?.message || 'Fault-injection simulation failed');
    }
  };

  // Sync latest execution when updated
  React.useEffect(() => {
    if (replayExecutions.length > 0) {
      setActiveReplay(replayExecutions[0]);
    }
  }, [replayExecutions]);

  const scenarios: { type: ReplayScenarioType; label: string; desc: string; iconColor: string }[] = [
    {
      type: 'CONFLICTING_EVENT',
      label: 'Cross-System Conflict',
      desc: 'Inject an asynchronous contradictory terminal callback (e.g. decline after commit).',
      iconColor: 'text-rose-400',
    },
    {
      type: 'MISSING_EVENT',
      label: 'Dropped Webhook / Gap',
      desc: 'Simulate a lost packet or unrecorded intermediary authorization approval.',
      iconColor: 'text-amber-400',
    },
    {
      type: 'OUT_OF_ORDER_EVENT',
      label: 'Out-of-Order Delivery',
      desc: 'Invert message broker sequencing (downstream capture arrives before auth).',
      iconColor: 'text-purple-400',
    },
    {
      type: 'DUPLICATE_EVENT',
      label: 'Duplicate / Retry Storm',
      desc: 'Simulate concurrent retries delivering redundant terminal callbacks.',
      iconColor: 'text-cyan-400',
    },
    {
      type: 'DELAYED_EVENT',
      label: 'Network Latency Skew',
      desc: 'Introduce artificial latency to test race condition handling.',
      iconColor: 'text-blue-400',
    },
    {
      type: 'DROP_AFTER_STEP',
      label: 'Pipeline Crash / Abrupt Halt',
      desc: 'Drop all subsequent events after a chosen milestone to simulate server failure.',
      iconColor: 'text-slate-400',
    },
  ];

  return (
    <div className="space-y-6">
      {/* Sandbox Security Header */}
      <div className="p-4 rounded border border-cyan-500/40 bg-cyan-950/20 flex items-center justify-between flex-wrap gap-3 shadow-[0_0_16px_rgba(6,182,212,0.1)]">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded bg-cyan-950 text-cyan-400 border border-cyan-500/40 flex items-center justify-center">
            <FlaskConical className="w-5 h-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="text-xs font-mono font-bold text-white tracking-wider">
                SANDBOXED FAULT-INJECTION LAB
              </span>
              <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-cyan-950 text-cyan-300 border border-cyan-500/40 flex items-center gap-1">
                <Lock className="w-3 h-3 text-cyan-400" />
                100% ISOLATED
              </span>
            </div>
            <p className="text-[11px] font-mono text-cyan-200/80">
              Simulations run strictly in an ephemeral memory sandbox. Real database records, production ledgers, and payment gateways are completely untouched.
            </p>
          </div>
        </div>

        <div className="text-right">
          <span className="text-[10px] font-mono text-console-dim block">Target Transaction</span>
          <span className="text-xs font-mono font-bold text-white">{transaction.externalId}</span>
        </div>
      </div>

      {simulationError && (
        <div className="p-3 rounded bg-rose-950/50 border border-rose-500/60 text-rose-300 text-xs font-mono flex items-center gap-2">
          <ShieldAlert className="w-4 h-4 text-rose-400 flex-shrink-0" />
          <span>Simulation Error: {simulationError}</span>
        </div>
      )}

      {/* Scenario Configuration Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Left 1/3: Fault Selector */}
        <div className="space-y-3">
          <span className="text-xs font-mono font-semibold text-console-text uppercase tracking-wider block">
            Select Failure Scenario
          </span>

          <div className="space-y-2">
            {scenarios.map((sc) => (
              <div
                key={sc.type}
                onClick={() => setSelectedScenario(sc.type)}
                className={`p-3 rounded border cursor-pointer transition-all ${
                  selectedScenario === sc.type
                    ? 'bg-console-800 border-cyan-500/60 shadow-[0_0_12px_rgba(6,182,212,0.15)] ring-1 ring-cyan-500/30'
                    : 'bg-console-900/60 border-console-border hover:border-console-700'
                }`}
              >
                <div className="flex items-center justify-between mb-1">
                  <span className={`text-xs font-mono font-bold ${sc.iconColor}`}>
                    {sc.label}
                  </span>
                  <Zap className={`w-3.5 h-3.5 ${sc.iconColor}`} />
                </div>
                <p className="text-[11px] font-mono text-console-dim leading-relaxed">
                  {sc.desc}
                </p>
              </div>
            ))}
          </div>

          {/* Delay Millis Configuration */}
          {(selectedScenario === 'DELAYED_EVENT' || selectedScenario === 'DUPLICATE_EVENT') && (
            <div className="pt-2 space-y-1">
              <label className="text-[11px] font-mono text-console-muted block">
                Simulated Delay: {delayMillis} ms
              </label>
              <input
                type="range"
                min="500"
                max="15000"
                step="500"
                value={delayMillis}
                onChange={(e) => setDelayMillis(parseInt(e.target.value))}
                className="w-full accent-cyan-500 bg-console-950"
              />
              <div className="flex justify-between text-[10px] font-mono text-console-dim">
                <span>0.5s</span>
                <span>5.0s</span>
                <span>15.0s</span>
              </div>
            </div>
          )}

          {/* Target Event Filter (Optional) */}
          {events.length > 0 && (
            <div className="pt-2 space-y-2">
              <label className="text-[11px] font-mono text-console-muted block">
                Target Specific Event (Optional):
              </label>
              <select
                value={targetEventId}
                onChange={(e) => setTargetEventId(e.target.value)}
                className="w-full px-2.5 py-1.5 rounded bg-console-950 border border-console-border text-xs font-mono text-white focus:outline-none focus:border-cyan-500"
              >
                <option value="">Default (Engine Automatic Selection)</option>
                {events.map((e) => (
                  <option key={e.eventId} value={e.eventId}>
                    {e.eventId} — {e.eventType} ({e.sourceSystem})
                  </option>
                ))}
              </select>
            </div>
          )}

          {/* Trigger Simulation Button */}
          <button
            onClick={handleSimulate}
            disabled={isExecuting}
            className="w-full py-2.5 rounded bg-cyan-600 hover:bg-cyan-500 text-white text-xs font-mono font-bold tracking-wider uppercase flex items-center justify-center gap-2 shadow-[0_0_16px_rgba(6,182,212,0.25)] transition-all"
          >
            <Play className={`w-4 h-4 fill-current ${isExecuting ? 'animate-spin' : ''}`} />
            <span>{isExecuting ? 'Injecting Fault...' : 'Run Sandboxed Simulation'}</span>
          </button>
        </div>

        {/* Right 2/3: Simulation Results & State Drift Diff */}
        <div className="lg:col-span-2 space-y-4">
          <div className="flex items-center justify-between">
            <span className="text-xs font-mono font-semibold text-console-text uppercase tracking-wider">
              Replay Outcome & Divergence Analysis
            </span>
            {replayExecutions.length > 1 && (
              <span className="text-[11px] font-mono text-console-dim">
                History: {replayExecutions.length} runs
              </span>
            )}
          </div>

          {!activeReplay ? (
            <div className="p-12 text-center border border-dashed border-console-border rounded bg-console-900/40 space-y-2">
              <FlaskConical className="w-8 h-8 text-console-dim mx-auto" />
              <p className="text-xs font-mono text-console-muted">No replay simulations executed for this session.</p>
              <p className="text-[11px] font-mono text-console-dim">Select a fault scenario on the left and click "Run Sandboxed Simulation".</p>
            </div>
          ) : (
            <div className="space-y-4">
              
              {/* Divergence Header Card */}
              <div className="p-4 rounded border border-console-border bg-console-900/70 space-y-3">
                <div className="flex items-center justify-between text-xs font-mono text-console-dim border-b border-console-borderSubtle pb-2">
                  <span>Scenario: <strong className="text-cyan-300">{activeReplay.scenarioType}</strong></span>
                  <span>Executed: {new Date(activeReplay.createdAt).toLocaleTimeString()}</span>
                </div>

                {/* State Drift Comparison */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-1">
                  
                  {/* Baseline Real State */}
                  <div className="p-3 rounded bg-console-950 border border-console-borderSubtle space-y-1.5">
                    <span className="text-[10px] font-mono text-console-dim uppercase tracking-wider block">
                      Actual Baseline State (Unmodified)
                    </span>
                    <div className="flex items-center gap-2">
                      <StatusBadge state={activeReplay.baselineState} size="md" />
                      <span className="text-xs font-mono text-console-muted">({activeReplay.baselineCertainty})</span>
                    </div>
                  </div>

                  {/* Simulated State */}
                  <div className={`p-3 rounded bg-console-950 border space-y-1.5 ${
                    activeReplay.baselineState !== activeReplay.simulatedState
                      ? 'border-rose-500/50 shadow-[0_0_12px_rgba(244,63,94,0.15)] ring-1 ring-rose-500/30'
                      : 'border-emerald-500/30'
                  }`}>
                    <span className="text-[10px] font-mono text-console-dim uppercase tracking-wider block">
                      Simulated State (Under Simulated Fault)
                    </span>
                    <div className="flex items-center gap-2">
                      <StatusBadge state={activeReplay.simulatedState} size="md" />
                      <span className="text-xs font-mono text-console-muted">({activeReplay.simulatedCertainty})</span>
                    </div>
                  </div>

                </div>

                {/* Divergence Alert Callout if state drifted */}
                {activeReplay.baselineState !== activeReplay.simulatedState ? (
                  <div className="p-2.5 rounded bg-rose-950/40 border border-rose-500/40 text-rose-300 text-xs font-mono flex items-center gap-2">
                    <AlertTriangle className="w-4 h-4 flex-shrink-0 text-rose-400" />
                    <span><strong>Critical State Drift Detected:</strong> Transaction transitioned from {activeReplay.baselineState} to {activeReplay.simulatedState} under this fault condition.</span>
                  </div>
                ) : (
                  <div className="p-2.5 rounded bg-emerald-950/30 border border-emerald-500/30 text-emerald-300 text-xs font-mono flex items-center gap-2">
                    <ShieldCheck className="w-4 h-4 flex-shrink-0 text-emerald-400" />
                    <span><strong>Resilient State:</strong> Reconstruction maintained {activeReplay.baselineState} despite injected fault mutation.</span>
                  </div>
                )}

                {/* Differences Summary Pill */}
                <div className="p-2 rounded bg-console-850 border border-console-border text-xs font-mono flex items-center gap-2">
                  <span className="font-semibold text-console-muted">Diff:</span>
                  <span className="text-cyan-300">{activeReplay.differencesSummary}</span>
                </div>
              </div>

              {/* Detailed Findings Markdown Narrative */}
              <div className="p-4 rounded border border-console-border bg-console-900/60 space-y-2">
                <span className="text-xs font-mono font-semibold text-console-text uppercase tracking-wider block">
                  Reconstruction Engine Diagnostic Findings
                </span>
                <div className="text-xs font-mono text-console-muted whitespace-pre-line p-3 rounded bg-console-950 border border-console-borderSubtle leading-relaxed">
                  {activeReplay.findings}
                </div>
              </div>

              {/* Simulated Mutated Event Stream */}
              {activeReplay.simulatedEvents.length > 0 && (
                <div className="space-y-2">
                  <span className="text-xs font-mono font-semibold text-console-text uppercase tracking-wider block">
                    Mutated Event Stream ({activeReplay.simulatedEvents.length})
                  </span>

                  <div className="space-y-2 max-h-56 overflow-y-auto pr-1">
                    {activeReplay.simulatedEvents.map((se) => {
                      const isMutated = se.mutationType !== 'ORIGINAL';
                      return (
                        <div
                          key={se.id}
                          className={`p-2.5 rounded border text-xs font-mono flex items-center justify-between ${
                            isMutated
                              ? 'bg-rose-950/20 border-rose-500/40 text-rose-300'
                              : 'bg-console-950 border-console-borderSubtle text-console-muted'
                          }`}
                        >
                          <div className="flex items-center gap-2">
                            <span className="font-bold text-white">{se.eventType}</span>
                            <span className="text-[10px] text-console-dim">[{se.sourceSystem}]</span>
                            {se.simulatedSequenceNumber && (
                              <span className="text-[10px] text-console-dim">#{se.simulatedSequenceNumber}</span>
                            )}
                          </div>

                          <div className="flex items-center gap-2">
                            <span className={`text-[10px] px-1.5 py-0.5 rounded border ${
                              isMutated
                                ? 'bg-rose-950 text-rose-300 border-rose-500/40'
                                : 'bg-console-850 text-console-dim border-console-border'
                            }`}>
                              {se.mutationType}
                            </span>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}

            </div>
          )}
        </div>

      </div>
    </div>
  );
};
