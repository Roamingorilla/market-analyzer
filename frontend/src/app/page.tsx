import { Topbar } from '@/components/layout/Topbar';
import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Dashboard — Market Analyser',
  description: 'Global market overview and quick access to top stocks',
};

const POPULAR_SYMBOLS = ['AAPL', 'MSFT', 'NVDA', 'AMZN', 'GOOGL', 'META', 'TSLA', 'JPM'];

async function getMarketOverview() {
  const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
  try {
    const responses = await Promise.allSettled(
      POPULAR_SYMBOLS.map(sym =>
        fetch(`${API_URL}/api/v1/stocks/${sym}`, { next: { revalidate: 3600 } })
          .then(r => r.json()).then(r => r.data)
      )
    );
    return responses
      .filter(r => r.status === 'fulfilled')
      .map(r => (r as PromiseFulfilledResult<unknown>).value)
      .filter(Boolean);
  } catch {
    return [];
  }
}

export default async function DashboardPage() {
  const stocks = await getMarketOverview() as Array<{
    symbol: string; name: string; price: number; changePct: number; volume: number;
    sector: string | null; dataFreshness: string;
  }>;

  return (
    <div className="app-shell">
      <Topbar />
      <main style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '20px' }}>

        {/* Page heading */}
        <div>
          <h1 style={{ margin: 0, fontSize: '20px', fontWeight: 700, color: 'var(--color-text-primary)' }}>
            Market Overview
          </h1>
          <p style={{ margin: '4px 0 0', fontSize: '13px', color: 'var(--color-text-muted)' }}>
            Popular stocks · EOD data · Cached
          </p>
        </div>

        {/* Market cards grid */}
        {stocks.length > 0 ? (
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))',
            gap: '12px',
          }}>
            {stocks.map((stock: typeof stocks[number]) => (
              <StockCard key={stock.symbol} stock={stock} />
            ))}
          </div>
        ) : (
          <EmptyState />
        )}

        {/* Getting started hint */}
        <div className="panel" style={{ padding: '20px 24px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            <div style={{
              width: 40, height: 40, borderRadius: '10px',
              background: 'linear-gradient(135deg, rgba(59,130,246,0.2), rgba(139,92,246,0.2))',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              border: '1px solid rgba(59,130,246,0.3)',
            }}>
              <span style={{ fontSize: '18px' }}>🔍</span>
            </div>
            <div>
              <div style={{ fontSize: '14px', fontWeight: 600, color: 'var(--color-text-primary)', marginBottom: 2 }}>
                Search for any stock
              </div>
              <div style={{ fontSize: '13px', color: 'var(--color-text-muted)' }}>
                Use the search bar above or press <kbd style={{
                  background: 'var(--color-bg-hover)', border: '1px solid var(--color-border)',
                  borderRadius: 4, padding: '1px 6px', fontSize: '11px', fontFamily: 'monospace',
                }}>⌘K</kbd> to find any global stock
              </div>
            </div>
          </div>
        </div>

      </main>
    </div>
  );
}

function StockCard({ stock }: { stock: ReturnType<typeof getMarketOverview> extends Promise<infer T> ? T extends Array<infer U> ? U : never : never }) {
  const s = stock as { symbol: string; name: string; price: number; changePct: number; sector: string | null; dataFreshness: string };
  const isUp = (s.changePct ?? 0) >= 0;
  return (
    <a href={`/stocks/${s.symbol}`} style={{ textDecoration: 'none' }}>
      <div style={{
        background: 'var(--color-bg-panel)',
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius-lg)',
        padding: '16px',
        cursor: 'pointer',
        transition: 'border-color 0.15s, background 0.15s',
      }}
        onMouseEnter={e => {
          (e.currentTarget as HTMLElement).style.borderColor = 'var(--color-border-light)';
          (e.currentTarget as HTMLElement).style.background = 'var(--color-bg-card)';
        }}
        onMouseLeave={e => {
          (e.currentTarget as HTMLElement).style.borderColor = 'var(--color-border)';
          (e.currentTarget as HTMLElement).style.background = 'var(--color-bg-panel)';
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 12 }}>
          <div>
            <div style={{ fontSize: '15px', fontWeight: 700, color: 'var(--color-text-primary)' }}>{s.symbol}</div>
            <div style={{ fontSize: '11px', color: 'var(--color-text-muted)', marginTop: 2 }}
              title={s.name}>
              {s.name?.length > 20 ? s.name.slice(0, 18) + '…' : s.name}
            </div>
          </div>
          {s.sector && <span className="badge badge-blue" style={{ fontSize: '10px' }}>{s.sector?.slice(0, 10)}</span>}
        </div>

        <div style={{ fontSize: '20px', fontWeight: 700, fontVariantNumeric: 'tabular-nums', marginBottom: 6 }}>
          {s.price != null ? `$${s.price.toFixed(2)}` : '—'}
        </div>

        <div style={{
          display: 'inline-flex', alignItems: 'center', gap: 4,
          fontSize: '13px', fontWeight: 600,
          color: isUp ? 'var(--color-green)' : 'var(--color-red)',
        }}>
          {isUp ? '▲' : '▼'}
          {s.changePct != null ? `${isUp ? '+' : ''}${s.changePct.toFixed(2)}%` : '—'}
        </div>
      </div>
    </a>
  );
}

function EmptyState() {
  return (
    <div className="panel" style={{ padding: '48px 24px', textAlign: 'center' }}>
      <div style={{ fontSize: '32px', marginBottom: 12 }}>📡</div>
      <div style={{ fontSize: '16px', fontWeight: 600, color: 'var(--color-text-primary)', marginBottom: 8 }}>
        Backend not connected
      </div>
      <div style={{ fontSize: '13px', color: 'var(--color-text-muted)', maxWidth: 400, margin: '0 auto' }}>
        Start the Spring Boot backend with <code style={{ fontFamily: 'monospace', color: 'var(--color-accent)' }}>docker-compose up</code> to load market data.
        The system will automatically seed popular stock data from Marketstack.
      </div>
    </div>
  );
}
