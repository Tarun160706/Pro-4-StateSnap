import React, { useState } from 'react';
import { TransactionEventDto, EvidenceRecordDto } from '../types';
import { Clock, Hash, Code, AlertTriangle, ShieldAlert, CheckCircle2, Copy, Check } from 'lucide-react';

interface Props {
  events: TransactionEventDto[];
  evidenceList: EvidenceRecordDto[];
  selectedEventId: string | null;
  onSelectEvent: (eventId: string) => void;
}

export const EventTimeline: React.FC<Props> = ({
  events,
  evidenceList,
  selectedEventId,
  onSelectEvent,
}) => {
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [viewMode, setViewMode] = useState<'sequence' | 'time'>('sequence');

  const copyPayload = (payload: string, id: string) => {
    navigator.clipboard.writeText(payload);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  // Find missing step evidence
  const missingSteps = evidenceList.filter(e => e.evidenceType === 'MISSING_STEP');
  const conflictEvidence = evidenceList.filter(e => e.evidenceType === 'CONFLICTING_SIGNAL');

  if (events.length === 0) {
    return (
      <div className="p-8 text-center border border-dashed border-console-border rounded bg-console-900/40">
        <Clock className="w-8 h-8 text-console-dim mx-auto mb-2" />
        <p className="text-xs font-mono text-console-muted">No transaction events ingested yet.</p>
        <p className="text-[11px] font-mono text-console-dim mt-1">Ingest raw logs via API or ingest dialog to start reconstruction.</p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Header & Controls */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span className="text-xs font-mono font-semibold text-console-text uppercase tracking-wider">
            Telemetric Event Stream ({events.length})
          </span>
          {conflictEvidence.length > 0 && (
            <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-rose-950/60 text-rose-300 border border-rose-500/50 flex items-center gap-1">
              <ShieldAlert className="w-3 h-3 text-rose-400" />
              {conflictEvidence.length} Conflict(s)
            </span>
          )}
        </div>

        <div className="flex items-center gap-1 p-0.5 rounded bg-console-950 border border-console-borderSubtle text-[11px] font-mono">
          <button
            onClick={() => setViewMode('sequence')}
            className={`px-2 py-0.5 rounded transition-all ${
              viewMode === 'sequence' ? 'bg-console-800 text-white' : 'text-console-dim hover:text-console-muted'
            }`}
          >
            Sequence Order
          </button>
          <button
            onClick={() => setViewMode('time')}
            className={`px-2 py-0.5 rounded transition-all ${
              viewMode === 'time' ? 'bg-console-800 text-white' : 'text-console-dim hover:text-console-muted'
            }`}
          >
            Clock Time
          </button>
        </div>
      </div>

      {/* Missing Step Callout Banner if present */}
      {missingSteps.length > 0 && (
        <div className="p-3 rounded bg-amber-950/30 border border-amber-500/40 space-y-1.5">
          <div className="flex items-center gap-2 text-amber-300 font-mono text-xs font-semibold">
            <AlertTriangle className="w-4 h-4 text-amber-400 flex-shrink-0" />
            <span>Missing Milestone Evidence Detected in Timeline</span>
          </div>
          <ul className="text-[11px] font-mono text-amber-200/80 space-y-1 pl-6 list-disc">
            {missingSteps.map((m, idx) => (
              <li key={idx}>{m.description}</li>
            ))}
          </ul>
        </div>
      )}

      {/* Timeline items list */}
      <div className="relative pl-6 space-y-3 before:content-[''] before:absolute before:left-2.5 before:top-3 before:bottom-3 before:w-0.5 before:bg-console-border">
        {events.map((evt, index) => {
          const isSelected = selectedEventId === evt.eventId;
          const hasConflict = conflictEvidence.some(c => 
            c.details?.includes(evt.eventId) || c.description.includes(evt.eventType)
          );

          return (
            <div
              key={evt.id}
              id={`evt-${evt.eventId}`}
              onClick={() => onSelectEvent(evt.eventId)}
              className={`relative p-3.5 rounded border transition-all cursor-pointer ${
                isSelected
                  ? 'bg-console-800 border-indigo-500/80 shadow-[0_0_16px_rgba(99,102,241,0.25)] ring-2 ring-indigo-500/40'
                  : hasConflict
                  ? 'bg-rose-950/20 border-rose-500/30 hover:border-rose-500/60'
                  : 'bg-console-900/60 border-console-border hover:border-console-600'
              }`}
            >
              {/* Timeline node dot */}
              <div
                className={`absolute -left-[1.85rem] top-4 w-3.5 h-3.5 rounded-full border-2 transition-all ${
                  hasConflict
                    ? 'bg-rose-500 border-rose-300 ring-4 ring-rose-500/20'
                    : isSelected
                    ? 'bg-indigo-500 border-indigo-200 ring-4 ring-indigo-500/20'
                    : 'bg-console-950 border-console-dim'
                }`}
              />

              <div className="flex items-start justify-between gap-3">
                <div className="space-y-1">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="text-xs font-mono font-bold text-white tracking-wide">
                      {evt.eventType}
                    </span>
                    <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-console-950 text-indigo-300 border border-indigo-500/20">
                      {evt.sourceSystem}
                    </span>
                    {evt.sequenceNumber !== null && (
                      <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-console-950 text-console-muted border border-console-border">
                        #{evt.sequenceNumber}
                      </span>
                    )}
                  </div>

                  <div className="flex items-center gap-3 text-[11px] font-mono text-console-dim">
                    <span className="flex items-center gap-1">
                      <Hash className="w-3 h-3 text-console-dim" />
                      {evt.eventId}
                    </span>
                    <span>•</span>
                    <span className="flex items-center gap-1">
                      <Clock className="w-3 h-3 text-console-dim" />
                      {new Date(evt.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit', fractionalSecondDigits: 3 })}
                    </span>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      copyPayload(evt.rawPayload, evt.id);
                    }}
                    title="Copy Raw JSON Payload"
                    className="p-1 rounded text-console-dim hover:text-white hover:bg-console-750 transition-colors"
                  >
                    {copiedId === evt.id ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                  </button>
                </div>
              </div>

              {/* Raw Payload Preview when selected */}
              {isSelected && (
                <div className="mt-3 pt-3 border-t border-console-borderSubtle">
                  <div className="flex items-center justify-between text-[10px] font-mono text-console-dim mb-1">
                    <span className="flex items-center gap-1 text-indigo-300 font-semibold">
                      <Code className="w-3 h-3" />
                      IMMUTABLE RAW TELEMETRY PAYLOAD
                    </span>
                    <span>Ingested: {new Date(evt.ingestionTimestamp).toLocaleTimeString()}</span>
                  </div>
                  <pre className="p-2.5 rounded bg-console-950 text-[11px] font-mono text-emerald-300/90 border border-console-borderSubtle overflow-x-auto max-h-48 leading-relaxed">
                    {(() => {
                      try {
                        return JSON.stringify(JSON.parse(evt.rawPayload), null, 2);
                      } catch {
                        return evt.rawPayload;
                      }
                    })()}
                  </pre>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};

