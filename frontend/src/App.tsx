import React, { useState, useEffect } from 'react';
import { TransactionDto, UserDto } from './types';
import { apiClient } from './api/client';
import { Navbar } from './components/Navbar';
import { TransactionsOverview } from './views/TransactionsOverview';
import { ForensicWorkspace } from './views/ForensicWorkspace';
import { NewTransactionModal } from './views/NewTransactionModal';
import { AuthModal } from './views/AuthModal';

export function App() {
  const [user, setUser] = useState<UserDto | null>(() => {
    const saved = localStorage.getItem('statesnap_user');
    return saved ? JSON.parse(saved) : null;
  });

  const [activeTab, setActiveTab] = useState<'transactions' | 'workspace' | 'replay' | 'recovery'>('transactions');
  const [selectedTx, setSelectedTx] = useState<TransactionDto | null>(null);
  const [showAuthModal, setShowAuthModal] = useState<boolean>(false);
  const [showNewTxModal, setShowNewTxModal] = useState<boolean>(false);
  const [savedFilter, setSavedFilter] = useState<string>('ALL');
  const [savedSearch, setSavedSearch] = useState<string>('');

  useEffect(() => {
    const checkAuth = async () => {
      const token = localStorage.getItem('statesnap_token');
      if (token) {
        try {
          const u = await apiClient.auth.me();
          setUser(u);
          localStorage.setItem('statesnap_user', JSON.stringify(u));
        } catch {
          // Token expired or invalid
          localStorage.removeItem('statesnap_token');
          localStorage.removeItem('statesnap_user');
          setUser(null);
        }
      }
    };

    checkAuth();

    const handleUnauthorized = () => {
      setUser(null);
      setShowAuthModal(true);
    };

    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('auth:unauthorized', handleUnauthorized);
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('statesnap_token');
    localStorage.removeItem('statesnap_user');
    setUser(null);
  };

  const handleSelectTransaction = (tx: TransactionDto) => {
    setSelectedTx(tx);
    setActiveTab('workspace');
  };

  const handleCreatedTx = (tx: TransactionDto) => {
    setSelectedTx(tx);
    setActiveTab('workspace');
  };

  return (
    <div className="min-h-screen flex flex-col bg-console-950 text-console-text">
      <Navbar
        user={user}
        onOpenAuth={() => setShowAuthModal(true)}
        onLogout={handleLogout}
        onNewTransaction={() => setShowNewTxModal(true)}
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        hasSelectedTx={!!selectedTx}
      />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {activeTab === 'transactions' || !selectedTx ? (
          <TransactionsOverview
            onSelectTransaction={handleSelectTransaction}
            onNewTransaction={() => setShowNewTxModal(true)}
            savedFilter={savedFilter}
            savedSearch={savedSearch}
            onFilterChange={(f, s) => {
              setSavedFilter(f);
              setSavedSearch(s);
            }}
          />
        ) : (
          <ForensicWorkspace
            transaction={selectedTx}
            onBack={() => setActiveTab('transactions')}
            initialTab={activeTab === 'replay' ? 'replay' : activeTab === 'recovery' ? 'recovery' : 'trace'}
          />
        )}
      </main>

      <footer className="border-t border-console-borderSubtle py-4 text-center text-[11px] font-mono text-console-dim">
        <p>StateSnap — Deterministic Transaction State Reconstruction & Fault Forensics Console</p>
      </footer>

      {/* Modals */}
      <AuthModal
        isOpen={showAuthModal}
        onClose={() => setShowAuthModal(false)}
        onSuccess={(u) => setUser(u)}
      />

      <NewTransactionModal
        isOpen={showNewTxModal}
        onClose={() => setShowNewTxModal(false)}
        onCreated={handleCreatedTx}
      />
    </div>
  );
}

export default App;
