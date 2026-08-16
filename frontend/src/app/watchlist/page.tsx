'use client';

import { Topbar } from '@/components/layout/Topbar';
import { BookMarked, Plus, AlertCircle } from 'lucide-react';

export default function WatchlistPage() {
  return (
    <div className="app-shell">
      <Topbar />
      <main style={{ padding: '24px', maxWidth: 1200, margin: '0 auto', width: '100%' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
          <div>
            <h1 style={{ margin: 0, fontSize: '24px', fontWeight: 700, color: 'var(--color-text-primary)' }}>
              Watchlists
            </h1>
            <p style={{ margin: '4px 0 0', fontSize: '14px', color: 'var(--color-text-muted)' }}>
              Track your favorite stocks across global markets
            </p>
          </div>
          <button className="btn btn-primary">
            <Plus size={16} /> New Watchlist
          </button>
        </div>

        <div className="panel" style={{ padding: '48px 24px', textAlign: 'center' }}>
          <div style={{ display: 'inline-flex', padding: 16, background: 'var(--color-bg-hover)', borderRadius: '50%', marginBottom: 16 }}>
            <BookMarked size={32} color="var(--color-accent)" />
          </div>
          <h2 style={{ margin: '0 0 8px', fontSize: '18px', fontWeight: 600, color: 'var(--color-text-primary)' }}>
            Authentication Required
          </h2>
          <p style={{ margin: '0 auto 24px', maxWidth: 400, fontSize: '14px', color: 'var(--color-text-muted)', lineHeight: 1.5 }}>
            Watchlist functionality will be available in Phase 2 once user authentication is implemented.
          </p>
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: 6, padding: '8px 12px', background: 'rgba(245,158,11,0.1)', color: 'var(--color-yellow)', borderRadius: 6, fontSize: 13, fontWeight: 500 }}>
            <AlertCircle size={14} /> Phase 1 feature limitation
          </div>
        </div>
      </main>
    </div>
  );
}
