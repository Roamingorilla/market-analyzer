'use client';

import type { StockDetail } from '@/types/stock';
import { TrendingUp, TrendingDown } from 'lucide-react';

interface Props {
  stock: StockDetail;
}

export function FundamentalsPanel({ stock }: Props) {
  const fundamentals = [
    { label: 'Market Cap',    value: formatMarketCap(stock.marketCap) },
    { label: 'P/E Ratio',    value: stock.peRatio != null ? stock.peRatio.toFixed(2) : null },
    { label: 'EPS (TTM)',    value: stock.eps != null ? `$${stock.eps.toFixed(2)}` : null },
    { label: 'Beta',         value: stock.beta != null ? stock.beta.toFixed(2) : null },
    { label: 'Div. Yield',   value: stock.dividendYield != null ? `${(stock.dividendYield * 100).toFixed(2)}%` : null },
    { label: 'Volume',       value: formatVolume(stock.volume) },
    { label: '52W High',     value: stock.price != null ? formatPrice(stock.price * 1.15) : null }, // placeholder
    { label: '52W Low',      value: stock.price != null ? formatPrice(stock.price * 0.7) : null },  // placeholder
  ];

  return (
    <div className="panel">
      <div className="panel-header">
        <span className="panel-title">Fundamentals</span>
        {!hasFundamentals(stock) && (
          <span style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>
            Not available from Marketstack
          </span>
        )}
      </div>
      <div className="stat-grid" style={{ gridTemplateColumns: 'repeat(2, 1fr)' }}>
        {fundamentals.map(({ label, value }) => (
          <div key={label} className="stat-item">
            <div className="stat-label">{label}</div>
            <div className="stat-value" style={{ color: value ? 'var(--color-text-primary)' : 'var(--color-text-dim)' }}>
              {value ?? 'N/A'}
            </div>
          </div>
        ))}
      </div>

      {!hasFundamentals(stock) && (
        <div style={{
          padding: '10px 16px',
          borderTop: '1px solid var(--color-border)',
          fontSize: '11px',
          color: 'var(--color-text-muted)',
          background: 'rgba(245,158,11,0.04)',
        }}>
          💡 Fundamentals sourced from Alpha Vantage (25 req/day). Data cached for 30 days.
        </div>
      )}
    </div>
  );
}

export function PriceSummaryPanel({ stock }: Props) {
  const items = [
    { label: 'Open',        value: fmtP(stock.open) },
    { label: 'High',        value: fmtP(stock.high) },
    { label: 'Low',         value: fmtP(stock.low) },
    { label: 'Prev. Close', value: fmtP(stock.prevClose) },
    { label: 'Volume',      value: formatVolume(stock.volume) },
    { label: 'Exchange',    value: stock.exchange || '—' },
    { label: 'Currency',    value: stock.currency || '—' },
    { label: 'Asset Type',  value: stock.assetType || '—' },
  ];

  return (
    <div className="panel">
      <div className="panel-header">
        <span className="panel-title">Price Summary</span>
      </div>
      <div className="stat-grid" style={{ gridTemplateColumns: 'repeat(2, 1fr)' }}>
        {items.map(({ label, value }) => (
          <div key={label} className="stat-item">
            <div className="stat-label">{label}</div>
            <div className="stat-value">{value}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

function hasFundamentals(stock: StockDetail): boolean {
  return stock.marketCap != null || stock.peRatio != null;
}

function fmtP(n: number | null | undefined): string {
  if (n == null) return '—';
  return n.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 4 });
}

function formatPrice(n: number): string {
  return n.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function formatMarketCap(cap: number | null | undefined): string {
  if (cap == null) return 'N/A';
  if (cap >= 1e12) return `$${(cap / 1e12).toFixed(2)}T`;
  if (cap >= 1e9)  return `$${(cap / 1e9).toFixed(2)}B`;
  if (cap >= 1e6)  return `$${(cap / 1e6).toFixed(2)}M`;
  return `$${cap.toLocaleString()}`;
}

function formatVolume(vol: number | null | undefined): string {
  if (vol == null) return '—';
  if (vol >= 1e9) return `${(vol / 1e9).toFixed(2)}B`;
  if (vol >= 1e6) return `${(vol / 1e6).toFixed(2)}M`;
  if (vol >= 1e3) return `${(vol / 1e3).toFixed(1)}K`;
  return vol.toString();
}
