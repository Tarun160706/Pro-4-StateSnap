import React from 'react';
import { EvidenceQuality } from '../types';
import { ShieldCheck, AlertTriangle, ShieldAlert, Layers, HelpCircle } from 'lucide-react';

interface Props {
  quality: EvidenceQuality | string | null | undefined;
  unresolvedCount?: number;
  size?: 'sm' | 'md';
}

export const EvidenceQualityBadge: React.FC<Props> = ({ quality, unresolvedCount = 0, size = 'md' }) => {
  if (!quality) {
    return (
      <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-mono bg-console-850 text-console-dim border border-console-borderSubtle">
        <HelpCircle className="w-3 h-3" />
        NO_DATA
      </span>
    );
  }

  const q = String(quality).toUpperCase();

  let styles = 'bg-console-850 text-console-muted border-console-border';
  let Icon = HelpCircle;
  let label = q;

  switch (q) {
    case 'VERIFIED':
      styles = 'bg-emerald-950/40 text-emerald-400 border-emerald-500/40 shadow-[0_0_8px_rgba(16,185,129,0.1)]';
      Icon = ShieldCheck;
      label = 'VERIFIED';
      break;
    case 'INCOMPLETE':
      styles = 'bg-amber-950/40 text-amber-300 border-amber-500/40 shadow-[0_0_8px_rgba(245,158,11,0.1)]';
      Icon = AlertTriangle;
      label = unresolvedCount > 0 ? `INCOMPLETE (${unresolvedCount} gap)` : 'INCOMPLETE';
      break;
    case 'CONTRADICTORY':
      styles = 'bg-rose-950/50 text-rose-300 border-rose-500/50 shadow-[0_0_10px_rgba(244,63,94,0.15)]';
      Icon = ShieldAlert;
      label = unresolvedCount > 0 ? `CONTRADICTORY (${unresolvedCount} conflict)` : 'CONTRADICTORY';
      break;
    case 'ANOMALOUS':
      styles = 'bg-purple-950/40 text-purple-300 border-purple-500/40';
      Icon = Layers;
      label = unresolvedCount > 0 ? `ANOMALOUS (${unresolvedCount})` : 'ANOMALOUS';
      break;
    default:
      styles = 'bg-console-850 text-console-dim border-console-borderSubtle';
      Icon = HelpCircle;
      label = q;
  }

  const sizeClass = size === 'sm' ? 'text-[10px] px-1.5 py-0.5' : 'text-xs px-2 py-0.5';

  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded font-mono font-medium border ${styles} ${sizeClass}`}
      title={`Evidence Quality: ${q}${unresolvedCount > 0 ? ` with ${unresolvedCount} unresolved anomaly record(s)` : ''}`}
    >
      <Icon className="w-3 h-3 flex-shrink-0" />
      <span>{label}</span>
    </span>
  );
};

