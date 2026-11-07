import React, { useState } from 'react';
import { apiClient } from '../api/client';
import { TransactionDto } from '../types';
import { PlusCircle, X, ShieldAlert } from 'lucide-react';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onCreated: (tx: TransactionDto) => void;
}

export const NewTransactionModal: React.FC<Props> = ({ isOpen, onClose, onCreated }) => {
  const [externalId, setExternalId] = useState<string>('');
  const [title, setTitle] = useState<string>('');
  const [channel, setChannel] = useState<string>('PAYMENT_GATEWAY');
  const [amount, setAmount] = useState<string>('');
  const [currency, setCurrency] = useState<string>('USD');
  const [metadataJson, setMetadataJson] = useState<string>('{}');
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!externalId.trim() || !title.trim()) {
      setError('External Reference ID and Title are required');
      return;
    }

    try {
      setSubmitting(true);
      setError(null);
      const parsedAmount = amount ? parseFloat(amount) : undefined;
      const tx = await apiClient.transactions.create({
        externalId: externalId.trim(),
        title: title.trim(),
        channel: channel.trim(),
        amount: parsedAmount,
        currency: currency.trim().toUpperCase(),
        metadataJson: metadataJson.trim() || '{}',
      });
      onCreated(tx);
      onClose();
    } catch (err: any) {
      setError(err?.response?.data?.message || err?.message || 'Failed to register transaction');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
      <div className="w-full max-w-lg rounded-lg border border-console-border bg-console-900 shadow-2xl p-6 space-y-4">
        
        <div className="flex items-center justify-between border-b border-console-borderSubtle pb-3">
          <div className="flex items-center gap-2">
            <PlusCircle className="w-4 h-4 text-indigo-400" />
            <h3 className="text-sm font-mono font-bold text-white">Register New Transaction Trace</h3>
          </div>
          <button onClick={onClose} className="text-console-dim hover:text-white">
            <X className="w-4 h-4" />
          </button>
        </div>

        {error && (
          <div className="p-3 rounded bg-rose-950/40 border border-rose-500/50 text-rose-300 text-xs font-mono flex items-center gap-2">
            <ShieldAlert className="w-4 h-4 flex-shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="text-xs font-mono text-console-muted block mb-1">
              External Reference ID * (e.g. TX-ACH-88912):
            </label>
            <input
              type="text"
              value={externalId}
              onChange={(e) => setExternalId(e.target.value)}
              placeholder="Unique external transaction ID"
              required
              className="w-full px-3 py-2 rounded bg-console-950 border border-console-border text-xs font-mono text-white focus:outline-none focus:border-indigo-500"
            />
          </div>

          <div>
            <label className="text-xs font-mono text-console-muted block mb-1">
              Investigation Title *:
            </label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="e.g. Dual Acquirer Card Checkout Failure"
              required
              className="w-full px-3 py-2 rounded bg-console-950 border border-console-border text-xs font-mono text-white focus:outline-none focus:border-indigo-500"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs font-mono text-console-muted block mb-1">
                Channel / Subsystem:
              </label>
              <input
                type="text"
                value={channel}
                onChange={(e) => setChannel(e.target.value)}
                placeholder="e.g. SWIFT_SETTLEMENT"
                className="w-full px-3 py-2 rounded bg-console-950 border border-console-border text-xs font-mono text-white focus:outline-none focus:border-indigo-500"
              />
            </div>

            <div>
              <label className="text-xs font-mono text-console-muted block mb-1">
                Amount & Currency:
              </label>
              <div className="flex gap-2">
                <input
                  type="number"
                  step="0.01"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  placeholder="0.00"
                  className="w-full px-3 py-2 rounded bg-console-950 border border-console-border text-xs font-mono text-white focus:outline-none focus:border-indigo-500"
                />
                <input
                  type="text"
                  value={currency}
                  onChange={(e) => setCurrency(e.target.value)}
                  className="w-20 px-2 py-2 rounded bg-console-950 border border-console-border text-xs font-mono text-white text-center focus:outline-none focus:border-indigo-500 uppercase"
                />
              </div>
            </div>
          </div>

          <div>
            <label className="text-xs font-mono text-console-muted block mb-1">
              Context Metadata (JSON):
            </label>
            <textarea
              rows={2}
              value={metadataJson}
              onChange={(e) => setMetadataJson(e.target.value)}
              className="w-full px-3 py-2 rounded bg-console-950 border border-console-border text-xs font-mono text-emerald-300 focus:outline-none focus:border-indigo-500"
            />
          </div>

          <div className="flex items-center justify-end gap-2 pt-2 border-t border-console-borderSubtle">
            <button
              type="button"
              onClick={onClose}
              className="px-3 py-1.5 rounded text-xs font-mono text-console-muted hover:text-white"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="px-4 py-1.5 rounded bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-mono font-medium shadow-[0_0_12px_rgba(99,102,241,0.25)]"
            >
              {submitting ? 'Creating...' : 'Register Transaction'}
            </button>
          </div>
        </form>

      </div>
    </div>
  );
};

