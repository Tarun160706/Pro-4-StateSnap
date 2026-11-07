import React, { useState } from 'react';
import { EvidenceRecordDto, EvidenceType } from '../types';
import { ShieldCheck, AlertTriangle, ShieldAlert, Layers, Hash, Clock, FileSearch } from 'lucide-react';

interface Props {
  evidenceList: EvidenceRecordDto[];
  onSelectEvent: (eventId: string) => void;
}

export const EvidenceExplorer: React.FC<Props> = ({ evidenceList, onSelectEvent }) => {
  const [filter, setFilter] = useState<string>('ALL');

  const filtered = evidenceList.filter(e => {
    if (filter === 'ALL') return true;
    return e.evidenceType === filter;
  });

  const verifiedCount = evidenceList.filter(e => e.evidenceType === 'VERIFIED_FACT').length;
  const missingCount = evidenceList.filter(e => e.evidenceType === 'MISSING_STEP').length;
  const conflictCount = evidenceList.filter(e => e.evidenceType === 'CONFLICTING_SIGNAL').length;

  return (
    <div className="space-y-4">
      {/* Filters and counts */}
      <div className="flex items-center justify-between flex-wrap gap-2">
        <div className="flex items-center gap-1.5 text-xs font-mono font-semibold text-console-text uppercase tracking-wider">
          <Layers className="w-4 h-4 text-indigo-400" />
          <span>Evidence Explorer ({evidenceList.length})</span>
        </div>

        <div className="flex items-center gap-1 p-0.5 rounded bg-console-950 border border-console-borderSubtle text-[11px] font-mono">
          <button
            onClick={() => setFilter('ALL')}
            className={`px-2 py-0.5 rounded transition-all ${
              filter === 'ALL' ? 'bg-console-800 text-white' : 'text-console-dim hover:text-console-muted'
            }`}
          >
            All ({evidenceList.length})
          </button>
          <button
            onClick={() => setFilter('VERIFIED_FACT')}
            className={`px-2 py-0.5 rounded transition-all ${
              filter === 'VERIFIED_FACT' ? 'bg-emerald-950/60 text-emerald-300 border border-emerald-500/40' : 'text-console-dim hover:text-emerald-300'
            }`}
          >
            Verified ({verifiedCount})
          </button>
          <button
            onClick={() => setFilter('MISSING_STEP')}
            className={`px-2 py-0.5 rounded transition-all ${
              filter === 'MISSING_STEP' ? 'bg-amber-950/60 text-amber-300 border border-amber-500/40' : 'text-console-dim hover:text-amber-300'
            }`}
          >
            Gaps ({missingCount})
          </button>
          <button
            onClick={() => setFilter('CONFLICTING_SIGNAL')}
            className={`px-2 py-0.5 rounded transition-all ${
              filter === 'CONFLICTING_SIGNAL' ? 'bg-rose-950/60 text-rose-300 border border-rose-500/40' : 'text-console-dim hover:text-rose-300'
            }`}
          >
            Conflicts ({conflictCount})
          </button>
        </div>
      </div>

      {/* Evidence items */}
      {filtered.length === 0 ? (
        <div className="p-6 text-center border border-dashed border-console-border rounded bg-console-900/40 text-console-dim text-xs font-mono">
          <FileSearch className="w-6 h-6 mx-auto mb-2 text-console-dim" />
          No evidence records found matching this category.
        </div>
      ) : (
        <div className="space-y-2 max-h-[380px] overflow-y-auto pr-1">
          {filtered.map((item) => {
            let iconClass = 'text-emerald-400';
            let IconComponent = ShieldCheck;
            let cardBg = 'bg-console-900/60 border-console-border';

            if (item.evidenceType === 'MISSING_STEP') {
              iconClass = 'text-amber-400';
              IconComponent = AlertTriangle;
              cardBg = 'bg-amber-950/20 border-amber-500/30';
            } else if (item.evidenceType === 'CONFLICTING_SIGNAL') {
              iconClass = 'text-rose-400';
              IconComponent = ShieldAlert;
              cardBg = 'bg-rose-950/20 border-rose-500/30';
            }

            return (
              <div
                key={item.id}
                className={`p-3 rounded border ${cardBg} transition-all space-y-1.5`}
              >
                <div className="flex items-start justify-between gap-2">
                  <div className="flex items-center gap-2">
                    <IconComponent className={`w-4 h-4 ${iconClass} flex-shrink-0`} />
                    <span className="text-[10px] font-mono font-bold uppercase tracking-wider text-console-muted">
                      {item.evidenceType.replace('_', ' ')}
                    </span>
                  </div>

                  {item.sourceEventId && (
                    <button
                      onClick={() => onSelectEvent(item.sourceEventId!)}
                      className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-console-950 text-indigo-300 border border-indigo-500/20 hover:border-indigo-400 transition-colors flex items-center gap-1"
                    >
                      <Hash className="w-2.5 h-2.5" />
                      {item.sourceEventId}
                    </button>
                  )}
                </div>

                <p className="text-xs font-mono text-white leading-relaxed">
                  {item.description}
                </p>

                {item.details && (
                  <p className="text-[11px] font-mono text-console-dim leading-relaxed bg-console-950/60 p-2 rounded border border-console-borderSubtle">
                    {item.details}
                  </p>
                )}

                <div className="flex items-center justify-between text-[10px] font-mono text-console-dim pt-1">
                  <span>Weight: {(item.confidenceWeight * 100).toFixed(0)}%</span>
                  <span className="flex items-center gap-1">
                    <Clock className="w-2.5 h-2.5" />
                    {new Date(item.detectedAt).toLocaleTimeString()}
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

