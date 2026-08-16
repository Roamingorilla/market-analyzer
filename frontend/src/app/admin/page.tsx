'use client';

import { useQuery } from '@tanstack/react-query';
import { Topbar } from '@/components/layout/Topbar';
import { adminApi } from '@/lib/api/stocks';
import { Server, Database, Activity, ShieldAlert } from 'lucide-react';

export default function AdminDashboard() {
  const { data: stats, isLoading } = useQuery({
    queryKey: ['admin-stats'],
    queryFn: () => adminApi.getStats(),
    refetchInterval: 10000, // refresh every 10s
  });

  return (
    <div className="app-shell">
      <Topbar />
      <main style={{ padding: '24px', maxWidth: 1200, margin: '0 auto', width: '100%' }}>
        <div style={{ marginBottom: 24 }}>
          <h1 style={{ margin: 0, fontSize: '24px', fontWeight: 700, color: 'var(--color-text-primary)', display: 'flex', alignItems: 'center', gap: 8 }}>
            <Server size={24} /> System Admin
          </h1>
          <p style={{ margin: '4px 0 0', fontSize: '14px', color: 'var(--color-text-muted)' }}>
            Monitor database sizes, API quotas, and background workers
          </p>
        </div>

        {isLoading ? (
          <div style={{ padding: 48, textAlign: 'center', color: 'var(--color-text-muted)' }}>Loading system stats...</div>
        ) : stats ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
            
            {/* Database Stats */}
            <div className="panel">
              <div className="panel-header">
                <span className="panel-title" style={{ display: 'flex', alignItems: 'center', gap: 6 }}><Database size={14} /> Database Overview</span>
              </div>
              <div className="stat-grid" style={{ gridTemplateColumns: 'repeat(3, 1fr)' }}>
                <StatCard label="Total Stocks" value={stats.totalStocks?.toLocaleString() || '0'} />
                <StatCard label="Total Exchanges" value={stats.totalExchanges?.toLocaleString() || '0'} />
                <StatCard label="Historical Price Bars" value={stats.totalHistoricalBars?.toLocaleString() || '0'} />
              </div>
            </div>

            {/* Quota Stats */}
            <div className="panel">
              <div className="panel-header">
                <span className="panel-title" style={{ display: 'flex', alignItems: 'center', gap: 6 }}><Activity size={14} /> API Quota Usage (Today)</span>
              </div>
              <div className="stat-grid" style={{ gridTemplateColumns: 'repeat(2, 1fr)' }}>
                <QuotaCard 
                  title="Marketstack (Core Data)" 
                  provider="Marketstack"
                  used={stats.marketstackQuota?.quotaUsed || 0} 
                  limit={stats.marketstackQuota?.quotaLimit || 10000} 
                />
                <QuotaCard 
                  title="Alpha Vantage (Fundamentals)" 
                  provider="Alpha Vantage"
                  used={stats.alphaVantageQuota?.quotaUsed || 0} 
                  limit={stats.alphaVantageQuota?.quotaLimit || 25} 
                />
              </div>
            </div>

          </div>
        ) : (
          <div className="panel" style={{ padding: 24, textAlign: 'center', color: 'var(--color-red)' }}>
            <ShieldAlert size={32} style={{ margin: '0 auto 12px' }} />
            <p>Failed to connect to backend admin services. Check if Spring Boot is running.</p>
          </div>
        )}
      </main>
    </div>
  );
}

function StatCard({ label, value }: { label: string, value: string }) {
  return (
    <div className="stat-item">
      <div className="stat-label">{label}</div>
      <div className="stat-value" style={{ fontSize: 24 }}>{value}</div>
    </div>
  );
}

function QuotaCard({ title, provider, used, limit }: { title: string, provider: string, used: number, limit: number }) {
  const pct = Math.min(100, Math.round((used / limit) * 100));
  const isDanger = pct >= 90;
  const isWarning = pct >= 75;
  
  const barColor = isDanger ? 'var(--color-red)' : isWarning ? 'var(--color-yellow)' : 'var(--color-green)';
  
  return (
    <div className="stat-item">
      <div className="stat-label">{title}</div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 8 }}>
        <div className="stat-value" style={{ fontSize: 24 }}>
          {used.toLocaleString()} <span style={{ fontSize: 14, color: 'var(--color-text-muted)', fontWeight: 500 }}>/ {limit.toLocaleString()}</span>
        </div>
        <div style={{ fontSize: 14, fontWeight: 700, color: barColor }}>{pct}%</div>
      </div>
      <div style={{ width: '100%', height: 6, background: 'var(--color-bg-secondary)', borderRadius: 3, overflow: 'hidden' }}>
        <div style={{ height: '100%', background: barColor, width: `${pct}%`, transition: 'width 0.3s ease' }} />
      </div>
      <div style={{ fontSize: 11, color: 'var(--color-text-muted)', marginTop: 8 }}>
        Resets at 00:00 UTC
      </div>
    </div>
  );
}
