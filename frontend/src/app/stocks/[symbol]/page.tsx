'use client';

import { useState, useEffect } from 'react';
import { useParams } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { Topbar } from '@/components/layout/Topbar';
import { StockHeader } from '@/components/stock/StockHeader';
import { FundamentalsPanel, PriceSummaryPanel } from '@/components/stock/StockPanels';
import { InteractiveStockChart } from '@/components/charts/InteractiveStockChart';
import { stocksApi } from '@/lib/api/stocks';
import { format, subDays, subMonths, subYears } from 'date-fns';

type TimeframeKey = '1M' | '3M' | '6M' | '1Y' | '3Y' | '5Y' | 'MAX';

const TIMEFRAME_MAP: Record<TimeframeKey, () => Date> = {
  '1M':  () => subDays(new Date(), 30),
  '3M':  () => subMonths(new Date(), 3),
  '6M':  () => subMonths(new Date(), 6),
  '1Y':  () => subYears(new Date(), 1),
  '3Y':  () => subYears(new Date(), 3),
  '5Y':  () => subYears(new Date(), 5),
  'MAX': () => new Date('2000-01-01'),
};

export default function StockDetailPage() {
  const params = useParams();
  const symbol = (params?.symbol as string)?.toUpperCase() ?? '';
  const [timeframe, setTimeframe] = useState<TimeframeKey>('1Y');

  const from = format(TIMEFRAME_MAP[timeframe](), 'yyyy-MM-dd');
  const to   = format(new Date(), 'yyyy-MM-dd');

  // Stock detail query
  const { data: stock, isLoading: stockLoading, error: stockError } = useQuery({
    queryKey: ['stock', symbol],
    queryFn: () => stocksApi.getDetail(symbol),
    enabled: !!symbol,
    staleTime: 5 * 60 * 1000, // 5 min
  });

  // Historical data query
  const { data: candles = [], isFetching: candlesLoading } = useQuery({
    queryKey: ['historical', symbol, from, to],
    queryFn: () => stocksApi.getHistorical(symbol, from, to),
    enabled: !!symbol,
    staleTime: 60 * 60 * 1000, // 1hr
  });

  // Technicals query (only when we have candle data)
  const { data: technicals } = useQuery({
    queryKey: ['technicals', symbol],
    queryFn: () => stocksApi.getTechnicals(symbol),
    enabled: !!symbol && candles.length > 0,
    staleTime: 60 * 60 * 1000,
  });

  if (stockError) {
    return (
      <div className="app-shell">
        <Topbar />
        <main style={{ padding: '48px', textAlign: 'center' }}>
          <div style={{ fontSize: '32px', marginBottom: 12 }}>🔍</div>
          <div style={{ fontSize: '18px', fontWeight: 600, color: 'var(--color-text-primary)', marginBottom: 8 }}>
            Stock not found
          </div>
          <div style={{ color: 'var(--color-text-muted)', fontSize: '13px' }}>
            "{symbol}" could not be found. Please check the symbol and try again.
          </div>
        </main>
      </div>
    );
  }

  return (
    <div className="app-shell">
      <Topbar />
      <main>
        {stockLoading ? (
          <StockHeaderSkeleton />
        ) : stock ? (
          <StockHeader stock={stock} />
        ) : null}

        <div style={{
          display: 'grid',
          gridTemplateColumns: '1fr 320px',
          gap: '16px',
          padding: '16px',
        }}>
          {/* Left: Chart */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <InteractiveStockChart
              candles={candles}
              technicals={technicals}
              symbol={symbol}
              isLoading={candlesLoading}
              onTimeframeChange={setTimeframe}
            />

            {/* Technical Analysis Panel */}
            {technicals && <TechnicalSummaryPanel technicals={technicals} />}
          </div>

          {/* Right: Info panels */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {stockLoading ? (
              <>
                <div className="skeleton" style={{ height: 200, borderRadius: 12 }} />
                <div className="skeleton" style={{ height: 200, borderRadius: 12 }} />
              </>
            ) : stock ? (
              <>
                <PriceSummaryPanel stock={stock} />
                <FundamentalsPanel stock={stock} />
                {stock.description && <CompanyDescPanel description={stock.description} website={stock.website} />}
              </>
            ) : null}
          </div>
        </div>
      </main>
    </div>
  );
}

function TechnicalSummaryPanel({ technicals }: { technicals: NonNullable<ReturnType<typeof useQuery>['data']> }) {
  const t = technicals as import('@/types/stock').TechnicalsResponse;
  const lastIdx = t.dates.length - 1;

  const rsi = t.rsi14?.[lastIdx];
  const macdHist = t.macd?.histogram[lastIdx];

  const rsiSignal = rsi != null
    ? rsi > 70 ? { label: 'Overbought', color: 'var(--color-red)' }
    : rsi < 30 ? { label: 'Oversold',   color: 'var(--color-green)' }
    : { label: 'Neutral', color: 'var(--color-text-muted)' }
    : null;

  return (
    <div className="panel">
      <div className="panel-header">
        <span className="panel-title">Technical Indicators</span>
        <span style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>
          Computed from cached price data · No API cost
        </span>
      </div>
      <div className="stat-grid" style={{ gridTemplateColumns: 'repeat(4, 1fr)' }}>
        <div className="stat-item">
          <div className="stat-label">RSI 14</div>
          <div className="stat-value" style={{ color: rsiSignal?.color }}>
            {rsi != null ? rsi.toFixed(1) : 'N/A'}
            {rsiSignal && <span style={{ fontSize: '10px', marginLeft: 6 }}>({rsiSignal.label})</span>}
          </div>
        </div>
        <div className="stat-item">
          <div className="stat-label">MACD Hist</div>
          <div className="stat-value" style={{ color: (macdHist ?? 0) >= 0 ? 'var(--color-green)' : 'var(--color-red)' }}>
            {macdHist != null ? macdHist.toFixed(3) : 'N/A'}
          </div>
        </div>
        <div className="stat-item">
          <div className="stat-label">SMA 20</div>
          <div className="stat-value mono">{fmt(t.sma20?.[lastIdx])}</div>
        </div>
        <div className="stat-item">
          <div className="stat-label">SMA 200</div>
          <div className="stat-value mono">{fmt(t.sma200?.[lastIdx])}</div>
        </div>
      </div>
    </div>
  );
}

function CompanyDescPanel({ description, website }: { description: string; website?: string | null }) {
  const [expanded, setExpanded] = useState(false);
  const short = description.slice(0, 200);
  return (
    <div className="panel">
      <div className="panel-header">
        <span className="panel-title">About</span>
      </div>
      <div style={{ padding: '12px 16px' }}>
        <p style={{ margin: 0, fontSize: '13px', color: 'var(--color-text-secondary)', lineHeight: 1.6 }}>
          {expanded ? description : short}{!expanded && description.length > 200 && '…'}
        </p>
        {description.length > 200 && (
          <button
            onClick={() => setExpanded(e => !e)}
            style={{ marginTop: 8, fontSize: '12px', color: 'var(--color-accent)', background: 'none', border: 'none', cursor: 'pointer', padding: 0 }}
          >
            {expanded ? 'Show less' : 'Read more'}
          </button>
        )}
        {website && (
          <a href={website} target="_blank" rel="noopener noreferrer"
            style={{ display: 'block', marginTop: 10, fontSize: '12px', color: 'var(--color-accent)' }}>
            {website}
          </a>
        )}
      </div>
    </div>
  );
}

function StockHeaderSkeleton() {
  return (
    <div style={{ padding: '20px 24px', borderBottom: '1px solid var(--color-border)' }}>
      <div style={{ display: 'flex', gap: 16 }}>
        <div className="skeleton" style={{ width: 48, height: 48, borderRadius: 10 }} />
        <div style={{ flex: 1 }}>
          <div className="skeleton" style={{ width: 120, height: 24, marginBottom: 8, borderRadius: 4 }} />
          <div className="skeleton" style={{ width: 200, height: 14, borderRadius: 4 }} />
        </div>
        <div style={{ textAlign: 'right' }}>
          <div className="skeleton" style={{ width: 140, height: 32, marginBottom: 8, borderRadius: 4 }} />
          <div className="skeleton" style={{ width: 80, height: 20, borderRadius: 4, marginLeft: 'auto' }} />
        </div>
      </div>
    </div>
  );
}

function fmt(v: number | null | undefined): string {
  if (v == null) return 'N/A';
  return v.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}
