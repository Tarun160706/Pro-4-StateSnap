import React from 'react';
import { RecoveryAnalysisDto } from '../types';
import { ShieldCheck, ShieldAlert, AlertTriangle, ArrowRight, CheckCircle2, Lock, Sparkles, RefreshCw } from 'lucide-react';

interface Props {
  recovery: RecoveryAnalysisDto | null;
  onRefresh: () => void;
  isLoading: boolean;
}

export const RecoveryPanel: React.FC<Props> = ({ recovery, onRefresh, isLoading }) => {
  if (!recovery) {
    return (
      <div className="p-8 text-center border border-dashed border-console-border rounded bg-console-900/40 space-y-3">
        <ShieldCheck className="w-8 h-8 text-console-dim mx-auto" />
        <p className="text-xs font-mono text-console-muted">Recovery analysis has not been calculated yet.</p>
        <button
          onClick={onRefresh}
          disabled={isLoading}
          className="px-3.5 py-1.5 rounded bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-mono font-medium transition-all"
        >
          {isLoading ? 'Evaluating...' : 'Run Recovery Advisor'}
        </button>
      </div>
    );
  }

  const isSafe = recovery.safeToExecute;

  return (
    <div className="space-y-4">
      {/* Action Recommendation Banner */}
      <div
        className={`p-4 rounded border ${
          isSafe
            ? 'bg-emerald-950/20 border-emerald-500/40'
            : 'bg-rose-950/30 border-rose-500/50 shadow-[0_0_16px_rgba(244,63,94,0.15)]'
        } space-y-3`}
      >
        <div className="flex items-center justify-between flex-wrap gap-2">
          <div className="flex items-center gap-2">
            {isSafe ? (
              <span className="p-1 rounded bg-emerald-950 text-emerald-400 border border-emerald-500/40">
                <CheckCircle2 className="w-5 h-5" />
              </span>
            ) : (
              <span className="p-1 rounded bg-rose-950 text-rose-400 border border-rose-500/40 animate-pulse">
                <Lock className="w-5 h-5" />
              </span>
            )}
            <div>
              <span className="text-[10px] font-mono text-console-dim uppercase tracking-wider block">
                {isSafe ? 'RECOVERY STATUS: ELIGIBLE RECOMMENDATION' : 'RECOVERY STATUS: EXECUTION STRICTLY LOCKED'}
              </span>
              <span className="text-sm font-mono font-bold text-white tracking-wide">
                {recovery.suggestedAction.replace(/_/g, ' ')}
              </span>
            </div>
          </div>

          <button
            onClick={onRefresh}
            disabled={isLoading}
            className="flex items-center gap-1 px-3 py-1.5 rounded bg-console-850 hover:bg-console-800 text-console-muted hover:text-white border border-console-border text-xs font-mono transition-all"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />
            <span>Re-analyze</span>
          </button>
        </div>

        <p className="text-xs font-mono text-console-muted leading-relaxed bg-console-950/60 p-3 rounded border border-console-borderSubtle">
          {recovery.rationale}
        </p>
      </div>

      {/* Safety Gate Warning Callout */}
      {!isSafe && (
        <div className="p-3.5 rounded bg-rose-950/40 border border-rose-500/60 space-y-1">
          <div className="flex items-center gap-2 text-rose-300 font-mono text-xs font-semibold">
            <ShieldAlert className="w-4 h-4 text-rose-400 flex-shrink-0" />
            <span>Strict Safety Gate Triggered</span>
          </div>
          <p className="text-[11px] font-mono text-rose-200/80 leading-relaxed pl-6">
            StateSnap prevents destructive automated operations on conflicting or unknown transactions. Automated execution is blocked. Manual cross-system audit is required.
          </p>
        </div>
      )}

      {/* Prerequisites Checklist */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
        <div className="p-4 rounded border border-console-border bg-console-900/60 space-y-2">
          <div className="flex items-center gap-2 text-xs font-mono font-semibold text-console-text uppercase tracking-wider">
            <CheckCircle2 className="w-4 h-4 text-indigo-400" />
            <span>Required Conditions & Prerequisites</span>
          </div>
          {recovery.prerequisites.length === 0 ? (
            <p className="text-[11px] font-mono text-console-dim">No special conditions required.</p>
          ) : (
            <ul className="text-xs font-mono text-console-muted space-y-1.5 pl-4 list-disc">
              {recovery.prerequisites.map((p, i) => (
                <li key={i}>{p}</li>
              ))}
            </ul>
          )}
        </div>

        {/* Missing Evidence / Prerequisites */}
        <div className="p-4 rounded border border-console-border bg-console-900/60 space-y-2">
          <div className="flex items-center gap-2 text-xs font-mono font-semibold text-amber-400 uppercase tracking-wider">
            <AlertTriangle className="w-4 h-4 text-amber-400" />
            <span>Missing Evidence Prerequisites</span>
          </div>
          {recovery.missingPrerequisites.length === 0 ? (
            <p className="text-[11px] font-mono text-emerald-400 flex items-center gap-1.5 pt-1">
              <CheckCircle2 className="w-3.5 h-3.5" />
              All requisite proofs verified in logs.
            </p>
          ) : (
            <ul className="text-xs font-mono text-amber-300/80 space-y-1.5 pl-4 list-disc">
              {recovery.missingPrerequisites.map((m, i) => (
                <li key={i}>{m}</li>
              ))}
            </ul>
          )}
        </div>
      </div>

      {/* Hazard & Side-Effect Evaluation */}
      {recovery.risks.length > 0 && (
        <div className="p-4 rounded border border-console-border bg-console-900/60 space-y-2">
          <div className="flex items-center gap-2 text-xs font-mono font-semibold text-rose-400 uppercase tracking-wider">
            <ShieldAlert className="w-4 h-4 text-rose-400" />
            <span>Identified Side-Effect Hazards</span>
          </div>
          <ul className="text-xs font-mono text-rose-300/90 space-y-1.5 pl-4 list-disc">
            {recovery.risks.map((r, i) => (
              <li key={i}>{r}</li>
            ))}
          </ul>
        </div>
      )}

      {/* Non-execution disclaimer */}
      <div className="text-[10px] font-mono text-console-dim text-center py-2 border-t border-console-borderSubtle">
        ℹ️ StateSnap recommendations are advisory for engineering forensic review. Production systems are protected from automated side effects.
      </div>
    </div>
  );
};

