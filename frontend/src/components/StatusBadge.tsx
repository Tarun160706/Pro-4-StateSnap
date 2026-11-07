import React from 'react';
import { TransactionStateEnum } from '../types';
import { CheckCircle2, XCircle, AlertTriangle, Clock, RefreshCw, HelpCircle, ShieldAlert } from 'lucide-react';

interface Props {
  state: TransactionStateEnum | string | null | undefined;
  size?: 'sm' | 'md' | 'lg';
  showIcon?: boolean;
}

export const StatusBadge: React.FC<Props> = ({ state, size = 'md', showIcon = true }) => {
  if (!state) {
    return (
      <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded text-xs font-mono font-medium bg-console-850 text-console-muted border border-console-border">
        <HelpCircle className="w-3.5 h-3.5 text-console-dim" />
        UNTRACKED
      </span>
    );
  }

  const s = String(state).toUpperCase();

  let styles = 'bg-slate-900/80 text-slate-300 border-slate-700';
  let Icon = HelpCircle;
  let label = s;

  switch (s) {
    case 'COMMITTED':
      styles = 'bg-emerald-950/40 text-emerald-400 border-emerald-500/40 shadow-[0_0_12px_rgba(16,185,129,0.12)]';
      Icon = CheckCircle2;
      label = 'COMMITTED';
      break;
    case 'FAILED':
      styles = 'bg-rose-950/40 text-rose-400 border-rose-500/40 shadow-[0_0_12px_rgba(244,63,94,0.12)]';
      Icon = XCircle;
      label = 'FAILED';
      break;
    case 'CONFLICTED':
      styles = 'bg-rose-950/80 text-rose-300 border-rose-500 animate-pulse shadow-[0_0_16px_rgba(244,63,94,0.25)]';
      Icon = ShieldAlert;
      label = 'CONFLICTED';
      break;
    case 'PROCESSING':
      styles = 'bg-indigo-950/40 text-indigo-300 border-indigo-500/40 shadow-[0_0_12px_rgba(99,102,241,0.12)]';
      Icon = RefreshCw;
      label = 'PROCESSING';
      break;
    case 'ROLLED_BACK':
      styles = 'bg-purple-950/40 text-purple-300 border-purple-500/40 shadow-[0_0_12px_rgba(168,85,247,0.12)]';
      Icon = RefreshCw;
      label = 'ROLLED_BACK';
      break;
    case 'INITIATED':
      styles = 'bg-amber-950/40 text-amber-300 border-amber-500/40';
      Icon = Clock;
      label = 'INITIATED';
      break;
    case 'RECONSTRUCTED':
      styles = 'bg-cyan-950/40 text-cyan-300 border-cyan-500/40';
      Icon = CheckCircle2;
      label = 'RECONSTRUCTED';
      break;
    case 'INVESTIGATION_PENDING':
      styles = 'bg-amber-950/40 text-amber-400 border-amber-500/50';
      Icon = AlertTriangle;
      label = 'PENDING';
      break;
    default:
      styles = 'bg-console-850 text-console-muted border-console-border';
      Icon = HelpCircle;
      label = s;
  }

  const sizeClasses = {
    sm: 'text-[10px] px-1.5 py-0.5 tracking-wider',
    md: 'text-xs px-2.5 py-1 tracking-wider',
    lg: 'text-sm px-3.5 py-1.5 tracking-wider font-semibold',
  }[size];

  const iconSizes = {
    sm: 'w-3 h-3',
    md: 'w-3.5 h-3.5',
    lg: 'w-4 h-4',
  }[size];

  return (
    <span className={`inline-flex items-center gap-1.5 rounded-sm font-mono font-medium border ${styles} ${sizeClasses}`}>
      {showIcon && <Icon className={`${iconSizes} flex-shrink-0`} />}
      <span>{label}</span>
    </span>
  );
};

