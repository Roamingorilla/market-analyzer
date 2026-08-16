'use client';

import type { StockDetail } from '@/types/stock';
import { ArrowUp, ArrowDown, Clock, AlertCircle, CheckCircle2 } from 'lucide-react';
import { format } from 'date-fns';

interface Props {
  stock: StockDetail;
}

export function StockHeader({ stock }: Props) {
  const isUp = (stock.changePct ?? 0) >= 0;
  const priceColor = isUp ? 'var(--color-green)' : 'var(--color-red)';

  return (
    <div style={{
      display: 'flex',
      alignItems: 'flex-start',
      gap: '20px',
      padding: '20px 24px',
      background: 'var(--color-bg-panel)',
      borderBottom: '1px solid var(--color-border)',
    }}>
      {/* Company Logo */}
      <div style={{
        width: 48, height: 48, borderRadius: 10,
        background: 'var(--color-bg-active)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        flexShrink: 0,
        border: '1px solid var(--color-border)',
        overflow: 'hidden',
      }}>
        {stock.logoUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={stock.logoUrl} alt={stock.symbol} style={{ width: 40, height: 40, objectFit: 'contain' }} />
        ) : (
          <span style={{ fontSize: '16px', fontWeight: 800, color: 'var(--color-accent)' }}>
            {stock.symbol.slice(0, 2)}
          </span>
        )}
      </div>

      {/* Name + exchange */}
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 4 }}>
          <h1 style={{
            margin: 0, fontSize: '22px', fontWeight: 800,
            color: 'var(--color-text-primary)', letterSpacing: '-0.02em',
          }}>
            {stock.symbol}
          </h1>
          {stock.exchange && (
            <span className="badge badge-blue">{stock.exchange}</span>
          )}
          {stock.assetType && (
            <span className="badge badge-purple">{stock.assetType}</span>
          )}
          {stock.isStale && (
            <span className="badge badge-yellow">
              <AlertCircle size={10} style={{ marginRight: 3 }} />
              Stale
            </span>
          )}
        </div>
        <div style={{ fontSize: '13px', color: 'var(--color-text-secondary)', marginBottom: 2 }}>
          {stock.name}
        </div>
        <div style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>
          {[stock.sector, stock.industry, stock.country].filter(Boolean).join(' · ')}
        </div>
      </div>

      {/* Price block */}
      <div style={{ textAlign: 'right', flexShrink: 0 }}>
        <div className="price-primary" style={{ marginBottom: 4 }}>
          {stock.currency && <span style={{ fontSize: '16px', fontWeight: 500, color: 'var(--color-text-muted)', marginRight: 4 }}>{stock.currency}</span>}
          {formatPrice(stock.price)}
        </div>

        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: 8, marginBottom: 6 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 4, color: priceColor }}>
            {isUp ? <ArrowUp size={14} /> : <ArrowDown size={14} />}
            <span className="price-secondary">
              {stock.change != null ? (isUp ? '+' : '') + formatPrice(stock.change) : '—'}
            </span>
          </div>
          <span
            className="badge"
            style={{
              background: isUp ? 'var(--color-green-dim)' : 'var(--color-red-dim)',
              color: priceColor,
            }}
          >
            {stock.changePct != null ? `${isUp ? '+' : ''}${stock.changePct.toFixed(2)}%` : '—'}
          </span>
        </div>

        {/* Data freshness */}
        <div className="freshness-indicator" style={{ justifyContent: 'flex-end' }}>
          <div className={`freshness-dot ${freshnessClass(stock.dataFreshness)}`} />
          <span>{freshnessLabel(stock.dataFreshness)}</span>
          {stock.lastPriceDate && (
            <span style={{ color: 'var(--color-text-dim)', marginLeft: 4 }}>
              · {stock.lastPriceDate}
            </span>
          )}
        </div>
      </div>
    </div>
  );
}

function formatPrice(n: number | null | undefined): string {
  if (n == null) return '—';
  return n.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 4 });
}

function freshnessClass(f: string): string {
  switch (f) {
    case 'LIVE': return 'live';
    case 'DELAYED': return 'delayed';
    default: return 'cached';
  }
}

function freshnessLabel(f: string): string {
  switch (f) {
    case 'LIVE': return 'Live';
    case 'DELAYED': return '15-min delay';
    case 'CACHED': return 'Cached EOD';
    case 'HISTORICAL': return 'Historical';
    default: return 'Cached';
  }
}
