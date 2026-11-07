import React, { useState } from 'react';
import { CertaintyLevel } from '../types';
import { ShieldCheck, ShieldAlert, AlertCircle, HelpCircle, Info } from 'lucide-react';

interface Props {
  score: number | null | undefined;
  certainty: CertaintyLevel | null | undefined;
  compact?: boolean;
}

export const CertaintyIndicator: React.FC<Props> = ({ score, certainty, compact = false }) => {
  const [showTooltip, setShowTooltip] = useState<boolean>(false);

  if (score === null || score === undefined || !certainty) {
    return (
      <span className="text-xs font-mono text-console-dim flex items-center gap-1">
        <HelpCircle className="w-3.5 h-3.5" />
        Unassessed
      </span>
    );
  }

  let colorClasses = 'text-slate-400 bg-slate-800/40 border-slate-700';
  let barColor = 'bg-slate-500';
  let Icon = HelpCircle;

  switch (certainty) {
    case 'HIGH_CERTAINTY':
      colorClasses = 'text-emerald-400 bg-emerald-950/30 border-emerald-500/30';
      barColor = 'bg-emerald-500';
      Icon = ShieldCheck;
      break;
    case 'CONDITIONAL_CERTAINTY':
      colorClasses = 'text-cyan-400 bg-cyan-950/30 border-cyan-500/30';
      barColor = 'bg-cyan-500';
      Icon = ShieldCheck;
      break;
    case 'AMBIGUOUS':
      colorClasses = 'text-amber-400 bg-amber-950/30 border-amber-500/30';
      barColor = 'bg-amber-500';
      Icon = AlertCircle;
      break;
    case 'HIGH_UNCERTAINTY':
      colorClasses = 'text-rose-400 bg-rose-950/30 border-rose-500/30';
      barColor = 'bg-rose-500';
      Icon = ShieldAlert;
      break;
  }

  const tooltipContent = (
    <div className="absolute z-50 bottom-full left-1/2 -translate-x-1/2 mb-2 w-72 p-3 rounded bg-console-900 border border-console-border shadow-2xl text-[11px] font-mono text-console-muted leading-relaxed space-y-1.5 pointer-events-none">
      <div className="flex items-center gap-1.5 font-bold text-white border-b border-console-borderSubtle pb-1">
        <Info className="w-3.5 h-3.5 text-indigo-400" />
        <span>Telemetry Confidence Metric</span>
      </div>
      <p>
        <strong className="text-console-text">What this represents:</strong> A deterministic score (base 100%) evaluating telemetry integrity, penalized by missing steps (-22%), contradictory signals (-45%), and incomplete pipelines (-25%).
      </p>
      <p className="text-rose-300/90 border-t border-console-borderSubtle pt-1 text-[10px]">
        ⚠️ <strong>Does NOT guarantee:</strong> This is an audit trace integrity index, NOT an empirical statistical probability of fund arrival or merchant settlement.
      </p>
    </div>
  );

  if (compact) {
    return (
      <div
        className="relative flex items-center gap-2 group cursor-help"
        onMouseEnter={() => setShowTooltip(true)}
        onMouseLeave={() => setShowTooltip(false)}
      >
        <div className="w-16 h-1.5 bg-console-950 rounded-full overflow-hidden border border-console-borderSubtle">
          <div
            className={`h-full ${barColor}`}
            style={{ width: `${Math.min(100, Math.max(0, score))}%` }}
          />
        </div>
        <span className="text-xs font-mono font-medium text-console-text flex items-center gap-0.5">
          {score.toFixed(1)}%
          <HelpCircle className="w-3 h-3 text-console-dim" />
        </span>
        {showTooltip && tooltipContent}
      </div>
    );
  }

  return (
    <div className={`relative p-3 rounded border ${colorClasses} space-y-2`}>
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Icon className="w-4 h-4 flex-shrink-0" />
          <span className="text-xs font-mono font-semibold tracking-wider uppercase">
            {certainty.replace('_', ' ')}
          </span>
        </div>
        <div
          className="flex items-center gap-1 cursor-help group"
          onMouseEnter={() => setShowTooltip(true)}
          onMouseLeave={() => setShowTooltip(false)}
        >
          <span className="text-sm font-mono font-bold">
            {score.toFixed(1)}%
          </span>
          <HelpCircle className="w-3.5 h-3.5 text-console-dim group-hover:text-white transition-colors" />
          {showTooltip && tooltipContent}
        </div>
      </div>
      <div className="w-full h-1.5 bg-console-950 rounded-full overflow-hidden border border-console-borderSubtle">
        <div
          className={`h-full ${barColor} transition-all duration-500`}
          style={{ width: `${Math.min(100, Math.max(0, score))}%` }}
        />
      </div>
    </div>
  );
};
