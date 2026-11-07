import React, { useState, useEffect } from 'react';
import { UserDto, SystemStatusDto } from '../types';
import { apiClient } from '../api/client';
import { Terminal, PlusCircle, LogOut, LogIn, Activity } from 'lucide-react';

interface Props {
  user: UserDto | null;
  onOpenAuth: () => void;
  onLogout: () => void;
  onNewTransaction: () => void;
  activeTab: 'transactions' | 'workspace' | 'replay' | 'recovery';
  setActiveTab: (tab: 'transactions' | 'workspace' | 'replay' | 'recovery') => void;
  hasSelectedTx: boolean;
}

export const Navbar: React.FC<Props> = ({
  user,
  onOpenAuth,
  onLogout,
  onNewTransaction,
  activeTab,
  setActiveTab,
  hasSelectedTx
}) => {
  const [systemStatus, setSystemStatus] = useState<SystemStatusDto | null>(null);
  const [statusState, setStatusState] = useState<'ONLINE' | 'DEGRADED' | 'OFFLINE' | 'CHECKING'>('CHECKING');

  const checkStatus = async () => {
    try {
      const res = await apiClient.system.status();
      setSystemStatus(res);
      setStatusState(res.status);
    } catch {
      setStatusState('OFFLINE');
    }
  };

  useEffect(() => {
    checkStatus();
    const interval = setInterval(checkStatus, 20000); // Poll every 20 seconds
    return () => clearInterval(interval);
  }, []);

  return (
    <header className="border-b border-console-border bg-console-900/90 backdrop-blur sticky top-0 z-40">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        
        {/* Left: Brand & Engine badge */}
        <div className="flex items-center gap-4 sm:gap-6">
          <div 
            onClick={() => setActiveTab('transactions')}
            className="flex items-center gap-3 cursor-pointer group"
          >
            <div className="w-9 h-9 rounded bg-indigo-600/20 border border-indigo-500/40 flex items-center justify-center text-indigo-400 group-hover:border-indigo-400 group-hover:shadow-[0_0_12px_rgba(99,102,241,0.3)] transition-all">
              <Terminal className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-mono font-bold tracking-wider text-sm sm:text-base text-white">
                  State<span className="text-indigo-400">Snap</span>
                </span>
                <span className="text-[10px] font-mono px-1.5 py-0.2 bg-console-800 text-console-muted rounded border border-console-border hidden sm:inline-block">
                  v1.0
                </span>
              </div>
              <p className="text-[10px] font-mono text-console-dim uppercase tracking-wider hidden sm:block">
                Transaction Failure Forensics
              </p>
            </div>
          </div>

          {/* Navigation modes */}
          <nav className="hidden md:flex items-center gap-1 pl-4 border-l border-console-border">
            <button
              onClick={() => setActiveTab('transactions')}
              className={`px-3 py-1.5 rounded text-xs font-mono font-medium transition-all ${
                activeTab === 'transactions'
                  ? 'bg-console-800 text-white border border-console-border'
                  : 'text-console-muted hover:text-white hover:bg-console-850'
              }`}
            >
              Investigations
            </button>

            {hasSelectedTx && (
              <>
                <button
                  onClick={() => setActiveTab('workspace')}
                  className={`px-3 py-1.5 rounded text-xs font-mono font-medium transition-all ${
                    activeTab === 'workspace'
                      ? 'bg-console-800 text-white border border-console-border'
                      : 'text-console-muted hover:text-white hover:bg-console-850'
                  }`}
                >
                  Forensic Trace
                </button>
                <button
                  onClick={() => setActiveTab('replay')}
                  className={`px-3 py-1.5 rounded text-xs font-mono font-medium transition-all ${
                    activeTab === 'replay'
                      ? 'bg-cyan-950/40 text-cyan-300 border border-cyan-500/30'
                      : 'text-console-muted hover:text-cyan-300 hover:bg-console-850'
                  }`}
                >
                  Replay Lab
                </button>
                <button
                  onClick={() => setActiveTab('recovery')}
                  className={`px-3 py-1.5 rounded text-xs font-mono font-medium transition-all ${
                    activeTab === 'recovery'
                      ? 'bg-indigo-950/40 text-indigo-300 border border-indigo-500/30'
                      : 'text-console-muted hover:text-indigo-300 hover:bg-console-850'
                  }`}
                >
                  Recovery Advisor
                </button>
              </>
            )}
          </nav>
        </div>

        {/* Right: Live Engine Status & User */}
        <div className="flex items-center gap-3 sm:gap-4">
          
          {/* Real Backend Status Indicator */}
          <div
            title={
              systemStatus
                ? `Engine: ${systemStatus.engineVersion} | DB: ${systemStatus.databaseStatus} | Checked: ${new Date(systemStatus.timestamp).toLocaleTimeString()}`
                : 'Connecting to backend health probe...'
            }
            className="flex items-center gap-2 px-2.5 py-1 rounded bg-console-950 border border-console-borderSubtle text-[11px] font-mono cursor-help"
          >
            {statusState === 'ONLINE' ? (
              <>
                <div className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
                <span className="text-console-dim hidden lg:inline">ENGINE:</span>
                <span className="text-emerald-400 font-medium">RECON-v1 ONLINE</span>
              </>
            ) : statusState === 'DEGRADED' ? (
              <>
                <div className="w-2 h-2 rounded-full bg-amber-500 animate-pulse" />
                <span className="text-console-dim hidden lg:inline">ENGINE:</span>
                <span className="text-amber-400 font-medium">DEGRADED</span>
              </>
            ) : statusState === 'CHECKING' ? (
              <>
                <div className="w-2 h-2 rounded-full bg-cyan-500 animate-pulse" />
                <span className="text-cyan-300 font-medium">CHECKING...</span>
              </>
            ) : (
              <>
                <div className="w-2 h-2 rounded-full bg-rose-500" />
                <span className="text-rose-400 font-medium">OFFLINE</span>
              </>
            )}
          </div>

          <button
            onClick={onNewTransaction}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-mono font-medium shadow-[0_0_12px_rgba(99,102,241,0.25)] transition-all"
          >
            <PlusCircle className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">New Investigation</span>
            <span className="sm:hidden">New</span>
          </button>

          {user ? (
            <div className="flex items-center gap-3 pl-3 border-l border-console-border">
              <div className="text-right hidden sm:block">
                <p className="text-xs font-mono text-white leading-none">{user.fullName}</p>
                <p className="text-[10px] font-mono text-console-dim leading-tight">{user.email}</p>
              </div>
              <button
                onClick={onLogout}
                title="Sign Out"
                className="p-1.5 rounded text-console-muted hover:text-rose-400 hover:bg-console-850 transition-colors"
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          ) : (
            <button
              onClick={onOpenAuth}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded border border-console-border text-xs font-mono text-console-muted hover:text-white hover:bg-console-850 transition-colors"
            >
              <LogIn className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">Engineer Sign-In</span>
            </button>
          )}
        </div>

      </div>

      {/* Mobile Sub-Navigation Bar */}
      {hasSelectedTx && (
        <div className="md:hidden flex items-center justify-around border-t border-console-border bg-console-950 px-2 py-1.5 text-xs font-mono">
          <button
            onClick={() => setActiveTab('transactions')}
            className={`px-2 py-1 rounded ${activeTab === 'transactions' ? 'text-white font-bold bg-console-850' : 'text-console-muted'}`}
          >
            List
          </button>
          <button
            onClick={() => setActiveTab('workspace')}
            className={`px-2 py-1 rounded ${activeTab === 'workspace' ? 'text-white font-bold bg-console-850' : 'text-console-muted'}`}
          >
            Trace
          </button>
          <button
            onClick={() => setActiveTab('replay')}
            className={`px-2 py-1 rounded ${activeTab === 'replay' ? 'text-cyan-300 font-bold bg-cyan-950/40' : 'text-console-muted'}`}
          >
            Replay
          </button>
          <button
            onClick={() => setActiveTab('recovery')}
            className={`px-2 py-1 rounded ${activeTab === 'recovery' ? 'text-indigo-300 font-bold bg-indigo-950/40' : 'text-console-muted'}`}
          >
            Recovery
          </button>
        </div>
      )}
    </header>
  );
};
