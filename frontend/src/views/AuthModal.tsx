import React, { useState } from 'react';
import { apiClient } from '../api/client';
import { UserDto } from '../types';
import { Lock, Mail, User, X, KeyRound, ShieldAlert } from 'lucide-react';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (user: UserDto) => void;
}

export const AuthModal: React.FC<Props> = ({ isOpen, onClose, onSuccess }) => {
  const [isRegister, setIsRegister] = useState<boolean>(false);
  const [email, setEmail] = useState<string>('engineer@statesnap.io');
  const [password, setPassword] = useState<string>('Engineer#2026!');
  const [fullName, setFullName] = useState<string>('Lead Forensic Engineer');
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setLoading(true);
      setError(null);
      let res;
      if (isRegister) {
        res = await apiClient.auth.register(email, password, fullName);
      } else {
        res = await apiClient.auth.login(email, password);
      }
      localStorage.setItem('statesnap_token', res.token);
      localStorage.setItem('statesnap_user', JSON.stringify(res.user));
      onSuccess(res.user);
      onClose();
    } catch (err: any) {
      setError(err?.response?.data?.message || err?.message || 'Authentication failed');
    } finally {
      setLoading(false);
    }
  };

  const fillDefaultCredentials = () => {
    setEmail('engineer@statesnap.io');
    setPassword('Engineer#2026!');
    setIsRegister(false);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
      <div className="w-full max-w-md rounded-lg border border-console-border bg-console-900 shadow-2xl p-6 space-y-4">
        
        <div className="flex items-center justify-between border-b border-console-borderSubtle pb-3">
          <div className="flex items-center gap-2">
            <KeyRound className="w-4 h-4 text-indigo-400" />
            <h3 className="text-sm font-mono font-bold text-white">
              {isRegister ? 'Register Forensic Engineer' : 'Engineer Authentication'}
            </h3>
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

        {/* Quick Fill Default Engineer Banner */}
        <div className="p-2.5 rounded bg-console-950 border border-console-borderSubtle flex items-center justify-between">
          <span className="text-[11px] font-mono text-console-muted">Quick Fill Default Engineer:</span>
          <button
            type="button"
            onClick={fillDefaultCredentials}
            className="text-[10px] font-mono px-2 py-0.5 rounded bg-indigo-950 text-indigo-300 border border-indigo-500/30 hover:border-indigo-400 transition-colors"
          >
            Load Seed Credentials
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {isRegister && (
            <div>
              <label className="text-xs font-mono text-console-muted block mb-1">Full Name:</label>
              <div className="relative">
                <User className="w-4 h-4 text-console-dim absolute left-3 top-2.5" />
                <input
                  type="text"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  required
                  placeholder="e.g. Lead Forensic Engineer"
                  className="w-full pl-9 pr-3 py-2 rounded bg-console-950 border border-console-border text-xs font-mono text-white focus:outline-none focus:border-indigo-500"
                />
              </div>
            </div>
          )}

          <div>
            <label className="text-xs font-mono text-console-muted block mb-1">Email:</label>
            <div className="relative">
              <Mail className="w-4 h-4 text-console-dim absolute left-3 top-2.5" />
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                placeholder="engineer@statesnap.io"
                className="w-full pl-9 pr-3 py-2 rounded bg-console-950 border border-console-border text-xs font-mono text-white focus:outline-none focus:border-indigo-500"
              />
            </div>
          </div>

          <div>
            <label className="text-xs font-mono text-console-muted block mb-1">Password:</label>
            <div className="relative">
              <Lock className="w-4 h-4 text-console-dim absolute left-3 top-2.5" />
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                placeholder="••••••••"
                className="w-full pl-9 pr-3 py-2 rounded bg-console-950 border border-console-border text-xs font-mono text-white focus:outline-none focus:border-indigo-500"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-2.5 rounded bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-mono font-medium shadow-[0_0_12px_rgba(99,102,241,0.25)] transition-all"
          >
            {loading ? 'Authenticating...' : isRegister ? 'Register & Enter Console' : 'Sign In to Console'}
          </button>
        </form>

        <div className="text-center pt-2 border-t border-console-borderSubtle">
          <button
            type="button"
            onClick={() => setIsRegister(!isRegister)}
            className="text-xs font-mono text-indigo-400 hover:underline"
          >
            {isRegister ? 'Already registered? Sign in instead' : 'Need an engineer account? Register'}
          </button>
        </div>

      </div>
    </div>
  );
};

