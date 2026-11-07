import React from 'react';
import { InferredStateDto } from '../types';
import { StatusBadge } from './StatusBadge';
import { CertaintyIndicator } from './CertaintyIndicator';
import { BrainCircuit, CheckCircle2, AlertOctagon, HelpCircle, Layers, ArrowRight } from 'lucide-react';

interface Props {
  inferredState: InferredStateDto | null;
  onSelectEvent: (eventId: string) => void;
  onTriggerReconstruct: () => void;
  isReconstructing: boolean;
}

export const InferenceExplanationPanel: React.FC<Props> = ({
  inferredState,
  onSelectEvent,
  onTriggerReconstruct,
  isReconstructing,
}) => {
  if (!inferredState) {
    return (
      <div className="p-6 text-center border border-dashed border-console-border rounded bg-console-900/40 space-y-3">
        <BrainCircuit className="w-8 h-8 text-console-dim mx-auto" />
        <p className="text-xs font-mono text-console-muted">No state inference generated yet.</p>
        <button
          onClick={onTriggerReconstruct}
          disabled={isReconstructing}
          className="px-3.5 py-1.5 rounded bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-mono font-medium shadow-[0_0_12px_rgba(99,102,241,0.25)] transition-all"
        >
          {isReconstructing ? 'Reconstructing...' : 'Trigger Deterministic Reconstruction'}
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* State & Certainty Header */}
      <div className="p-4 rounded border border-console-border bg-console-900/60 space-y-4">
        <div className="flex items-center justify-between flex-wrap gap-2">
          <div className="space-y-1">
            <span className="text-[10px] font-mono text-console-dim uppercase tracking-wider">
              Reconstructed Lifecycle Conclusion
            </span>
            <div className="flex items-center gap-2">
              <StatusBadge state={inferredState.inferredState} size="lg" />
            </div>
          </div>

          <button
            onClick={onTriggerReconstruct}
            disabled={isReconstructing}
            className="px-3 py-1.5 rounded bg-console-800 hover:bg-console-750 text-indigo-300 border border-indigo-500/30 text-xs font-mono font-medium transition-all"
          >
            {isReconstructing ? 'Re-analyzing...' : 'Re-run Engine'}
          </button>
        </div>

        {/* Defensible Confidence Gauge */}
        <CertaintyIndicator
          score={inferredState.confidenceScore}
          certainty={inferredState.certaintyLevel}
        />
      </div>

      {/* Supporting vs Contradicting Events Trace */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
        {/* Supporting Events */}
        <div className="p-3 rounded border border-emerald-500/20 bg-emerald-950/10 space-y-2">
          <div className="flex items-center gap-1.5 text-xs font-mono font-semibold text-emerald-400">
            <CheckCircle2 className="w-3.5 h-3.5" />
            <span>Supporting Signals ({inferredState.supportingEvents.length})</span>
          </div>
          {inferredState.supportingEvents.length === 0 ? (
            <p className="text-[11px] font-mono text-console-dim">None recorded</p>
          ) : (
            <div className="flex flex-wrap gap-1.5">
              {inferredState.supportingEvents.map((id) => (
                <button
                  key={id}
                  onClick={() => onSelectEvent(id)}
                  className="px-2 py-0.5 rounded bg-emerald-950/60 text-emerald-300 border border-emerald-500/30 text-[10px] font-mono hover:border-emerald-400 transition-colors"
                >
                  {id}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Contradicting Events */}
        <div className="p-3 rounded border border-rose-500/20 bg-rose-950/10 space-y-2">
          <div className="flex items-center gap-1.5 text-xs font-mono font-semibold text-rose-400">
            <AlertOctagon className="w-3.5 h-3.5" />
            <span>Contradicting Signals ({inferredState.contradictingEvents.length})</span>
          </div>
          {inferredState.contradictingEvents.length === 0 ? (
            <p className="text-[11px] font-mono text-console-dim">None detected (cohesive stream)</p>
          ) : (
            <div className="flex flex-wrap gap-1.5">
              {inferredState.contradictingEvents.map((id) => (
                <button
                  key={id}
                  onClick={() => onSelectEvent(id)}
                  className="px-2 py-0.5 rounded bg-rose-950/60 text-rose-300 border border-rose-500/30 text-[10px] font-mono hover:border-rose-400 transition-colors"
                >
                  {id}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Forensic Reasoning Breakdown */}
      <div className="p-4 rounded border border-console-border bg-console-900/60 space-y-2">
        <div className="flex items-center gap-2 text-xs font-mono font-semibold text-console-text uppercase tracking-wider">
          <BrainCircuit className="w-4 h-4 text-indigo-400" />
          <span>Forensic Reasoning Narrative</span>
        </div>

        <div className="text-xs font-mono text-console-muted leading-relaxed whitespace-pre-line p-3 rounded bg-console-950 border border-console-borderSubtle">
          {inferredState.reasoningExplanation}
        </div>

        <div className="text-[10px] font-mono text-console-dim text-right">
          Reconstructed At: {new Date(inferredState.reconstructedAt).toLocaleString()}
        </div>
      </div>
    </div>
  );
};

