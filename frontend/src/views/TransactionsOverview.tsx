import React, { useState, useEffect } from 'react';
import { TransactionDto } from '../types';
import { apiClient } from '../api/client';
import { StatusBadge } from '../components/StatusBadge';
import { EvidenceQualityBadge } from '../components/EvidenceQualityBadge';
import { CertaintyIndicator } from '../components/CertaintyIndicator';
import {
  Search,
  ShieldAlert,
  CheckCircle2,
  Clock,
  Layers,
  ArrowRight,
  RefreshCw,
  HelpCircle,
  Hash
} from 'lucide-react';

interface Props {
  onSelectTransaction: (tx: TransactionDto) => void;
  onNewTransaction: () => void;
  savedFilter?: string;
  savedSearch?: string;
  onFilterChange?: (filter: string, search: string) => void;
}

export const TransactionsOverview: React.FC<Props> = ({
  onSelectTransaction,
  onNewTransaction,
  savedFilter = 'ALL',
  savedSearch = '',
  onFilterChange,
}) => {
  const [transactions, setTransactions] = useState<TransactionDto[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<string>(savedFilter);
  const [searchQuery, setSearchQuery] = useState<string>(savedSearch);
  const [page, setPage] = useState<number>(0);
  const [totalElements, setTotalElements] = useState<number>(0);

  const fetchTransactions = async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await apiClient.transactions.list(statusFilter, searchQuery, page, 15);
      setTransactions(res.content);
      setTotalElements(res.totalElements);
      if (onFilterChange) {
        onFilterChange(statusFilter, searchQuery);
      }
    } catch (err: any) {
      setError(err?.response?.data?.message || err?.message || 'Failed to load transactions');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTransactions();
  }, [statusFilter, page]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    fetchTransactions();
  };

  // Stats calculation
  const conflictCount = transactions.filter(t => t.hasConflict || t.latestState === 'CONFLICTED').length;
  const committedCount = transactions.filter(t => t.latestState === 'COMMITTED').length;
  const pendingCount = transactions.filter(t => t.status === 'INVESTIGATION_PENDING' || t.latestState === 'PROCESSING').length;

  return (
    <div className="space-y-6">
      
      {/* Metrics Row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        
        <div className="p-4 rounded border border-console-border bg-console-900/60 space-y-1">
          <div className="flex items-center justify-between text-console-muted">
            <span className="text-[11px] font-mono uppercase tracking-wider font-semibold">Total Trace Logs</span>
            <Layers className="w-4 h-4 text-indigo-400" />
          </div>
          <p className="text-2xl font-mono font-bold text-white">{totalElements}</p>
          <span className="text-[10px] font-mono text-console-dim">Aggregated across payment channels</span>
        </div>

        <div className="p-4 rounded border border-rose-500/30 bg-rose-950/20 space-y-1">
          <div className="flex items-center justify-between text-rose-300">
            <span className="text-[11px] font-mono uppercase tracking-wider font-semibold">Conflicted Signals</span>
            <ShieldAlert className="w-4 h-4 text-rose-400" />
          </div>
          <p className="text-2xl font-mono font-bold text-rose-300">{conflictCount}</p>
          <span className="text-[10px] font-mono text-rose-300/80">Cross-system desync detected</span>
        </div>

        <div className="p-4 rounded border border-emerald-500/30 bg-emerald-950/20 space-y-1">
          <div className="flex items-center justify-between text-emerald-300">
            <span className="text-[11px] font-mono uppercase tracking-wider font-semibold">Committed State</span>
            <CheckCircle2 className="w-4 h-4 text-emerald-400" />
          </div>
          <p className="text-2xl font-mono font-bold text-emerald-300">{committedCount}</p>
          <span className="text-[10px] font-mono text-emerald-300/80">Terminal success milestone</span>
        </div>

        <div className="p-4 rounded border border-amber-500/30 bg-amber-950/20 space-y-1">
          <div className="flex items-center justify-between text-amber-300">
            <span className="text-[11px] font-mono uppercase tracking-wider font-semibold">Incomplete / Stalled</span>
            <Clock className="w-4 h-4 text-amber-400" />
          </div>
          <p className="text-2xl font-mono font-bold text-amber-300">{pendingCount}</p>
          <span className="text-[10px] font-mono text-amber-300/80">Gaps or in-flight pipelines</span>
        </div>

      </div>

      {/* Action Bar & Filters */}
      <div className="p-4 rounded border border-console-border bg-console-900/60 flex flex-col md:flex-row gap-4 items-stretch md:items-center justify-between">
        
        {/* Search Input */}
        <form onSubmit={handleSearchSubmit} className="relative flex-1 max-w-md">
          <Search className="w-4 h-4 text-console-muted absolute left-3 top-3" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search external ID, title, or payment channel..."
            className="w-full pl-9 pr-4 py-2 rounded bg-console-950 border border-console-border text-xs font-mono text-white placeholder-console-dim focus:outline-none focus:border-indigo-500 transition-colors"
          />
        </form>

        {/* Status Filter Pills */}
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1 md:pb-0 text-xs font-mono">
          {['ALL', 'CONFLICTED', 'RECONSTRUCTED', 'INVESTIGATION_PENDING'].map((status) => (
            <button
              key={status}
              onClick={() => {
                setStatusFilter(status);
                setPage(0);
              }}
              className={`px-3 py-1.5 rounded transition-all whitespace-nowrap font-medium ${
                statusFilter === status
                  ? 'bg-console-800 text-white border border-console-border shadow-sm ring-1 ring-console-border'
                  : 'text-console-muted hover:text-white hover:bg-console-850'
              }`}
            >
              {status.replace(/_/g, ' ')}
            </button>
          ))}

          <button
            onClick={fetchTransactions}
            title="Refresh Table Data"
            className="p-1.5 rounded text-console-muted hover:text-white hover:bg-console-850 border border-console-borderSubtle transition-colors ml-1"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>

      </div>

      {/* Forensic Table */}
      <div className="rounded border border-console-border bg-console-900/60 overflow-hidden shadow-xl">
        {loading ? (
          <div className="p-12 text-center text-xs font-mono text-console-dim space-y-2">
            <RefreshCw className="w-6 h-6 animate-spin mx-auto text-indigo-400" />
            <p>Scanning telemetric transaction records...</p>
          </div>
        ) : error ? (
          <div className="p-8 text-center text-xs font-mono text-rose-400 space-y-2">
            <ShieldAlert className="w-6 h-6 mx-auto" />
            <p>{error}</p>
            <button
              onClick={fetchTransactions}
              className="px-3 py-1.5 rounded bg-console-800 text-white text-xs hover:bg-console-750"
            >
              Retry
            </button>
          </div>
        ) : transactions.length === 0 ? (
          <div className="p-12 text-center border border-dashed border-console-border rounded m-4 space-y-3">
            <Layers className="w-8 h-8 text-console-dim mx-auto" />
            <p className="text-xs font-mono text-console-muted">No transactions found matching your criteria.</p>
            <button
              onClick={onNewTransaction}
              className="px-3.5 py-1.5 rounded bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-mono"
            >
              Register First Transaction
            </button>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs font-mono">
              <thead className="bg-console-950 border-b border-console-border text-slate-300 uppercase tracking-wider text-[11px] font-semibold">
                <tr>
                  <th className="px-4 py-3">External Reference</th>
                  <th className="px-4 py-3">Investigation Title & Channel</th>
                  <th className="px-4 py-3">Amount</th>
                  <th className="px-4 py-3">Events</th>
                  <th className="px-4 py-3" title="Deterministic state machine inference">
                    Inferred State
                  </th>
                  <th className="px-4 py-3" title="Audit log completeness and contradiction quality">
                    Evidence Quality
                  </th>
                  <th className="px-4 py-3" title="Defensible evidence integrity score">
                    Confidence
                  </th>
                  <th className="px-4 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-console-borderSubtle">
                {transactions.map((tx) => (
                  <tr
                    key={tx.id}
                    tabIndex={0}
                    role="button"
                    onClick={() => onSelectTransaction(tx)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault();
                        onSelectTransaction(tx);
                      }
                    }}
                    className="hover:bg-console-850/80 focus:bg-console-850/80 focus:outline-none cursor-pointer transition-colors group"
                  >
                    <td className="px-4 py-3.5 font-bold text-white whitespace-nowrap">
                      <span className="group-hover:text-indigo-400 transition-colors flex items-center gap-1.5">
                        <Hash className="w-3.5 h-3.5 text-console-dim" />
                        {tx.externalId}
                      </span>
                    </td>

                    <td className="px-4 py-3.5 max-w-xs">
                      <p className="text-console-text font-medium truncate" title={tx.title}>
                        {tx.title}
                      </p>
                      <span className="text-[10px] text-console-dim uppercase tracking-wider font-mono">
                        {tx.channel || 'DEFAULT'}
                      </span>
                    </td>

                    <td className="px-4 py-3.5 whitespace-nowrap">
                      {tx.amount !== null && tx.amount !== undefined ? (
                        <span className="text-white font-medium">
                          {tx.currency} {Number(tx.amount).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                        </span>
                      ) : (
                        <span className="text-console-dim font-mono">N/A</span>
                      )}
                    </td>

                    <td className="px-4 py-3.5 whitespace-nowrap">
                      <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded bg-console-950 text-console-muted border border-console-border text-[11px]">
                        {tx.eventCount} logs
                      </span>
                    </td>

                    <td className="px-4 py-3.5 whitespace-nowrap">
                      <StatusBadge state={tx.latestState || tx.status} size="sm" />
                    </td>

                    <td className="px-4 py-3.5 whitespace-nowrap">
                      <EvidenceQualityBadge
                        quality={tx.evidenceQuality}
                        unresolvedCount={tx.unresolvedIssuesCount}
                        size="sm"
                      />
                    </td>

                    <td className="px-4 py-3.5 whitespace-nowrap">
                      <CertaintyIndicator
                        score={tx.latestConfidenceScore}
                        certainty={tx.latestCertainty}
                        compact
                      />
                    </td>

                    <td className="px-4 py-3.5 text-right whitespace-nowrap">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          onSelectTransaction(tx);
                        }}
                        className="px-3 py-1 rounded bg-console-800 group-hover:bg-indigo-600 text-console-muted group-hover:text-white border border-console-border group-hover:border-indigo-500 text-xs font-mono font-medium transition-all inline-flex items-center gap-1.5 shadow-sm"
                      >
                        <span>Investigate</span>
                        <ArrowRight className="w-3.5 h-3.5" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

    </div>
  );
};
