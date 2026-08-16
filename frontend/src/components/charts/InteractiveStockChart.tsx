'use client';

import { useEffect, useRef, useState, useCallback } from 'react';
import {
  createChart,
  ColorType,
  CrosshairMode,
  LineStyle,
  type IChartApi,
  type ISeriesApi,
  type CandlestickData,
  type Time,
} from 'lightweight-charts';
import type { HistoricalCandle, TechnicalsResponse } from '@/types/stock';
import {
  CandlestickChart, TrendingUp, BarChart3, Activity,
  Minus, ChevronDown, Pencil, Trash2
} from 'lucide-react';

type ChartType = 'candlestick' | 'line' | 'area';
type TimeframeKey = '1M' | '3M' | '6M' | '1Y' | '3Y' | '5Y' | 'MAX';

interface DrawingTool {
  id: string;
  icon: React.ElementType;
  label: string;
}

const TIMEFRAMES: { label: TimeframeKey; days: number }[] = [
  { label: '1M',  days: 30   },
  { label: '3M',  days: 90   },
  { label: '6M',  days: 180  },
  { label: '1Y',  days: 365  },
  { label: '3Y',  days: 1095 },
  { label: '5Y',  days: 1825 },
  { label: 'MAX', days: 9999 },
];

const INDICATOR_OPTIONS = [
  { key: 'sma20',  label: 'SMA 20',  color: '#3b82f6' },
  { key: 'sma50',  label: 'SMA 50',  color: '#f59e0b' },
  { key: 'sma200', label: 'SMA 200', color: '#ef4444' },
  { key: 'bb20',   label: 'BB 20',   color: '#8b5cf6' },
];

interface Props {
  candles: HistoricalCandle[];
  technicals?: TechnicalsResponse;
  symbol: string;
  isLoading?: boolean;
  onTimeframeChange?: (tf: TimeframeKey) => void;
}

export function InteractiveStockChart({
  candles, technicals, symbol, isLoading, onTimeframeChange,
}: Props) {
  const chartContainerRef = useRef<HTMLDivElement>(null);
  const chartRef = useRef<IChartApi | null>(null);
  const candlestickRef = useRef<ISeriesApi<'Candlestick'> | null>(null);
  const lineRef = useRef<ISeriesApi<'Line'> | null>(null);
  const areaRef = useRef<ISeriesApi<'Area'> | null>(null);
  const volumeRef = useRef<ISeriesApi<'Histogram'> | null>(null);
  const smaRefs = useRef<Record<string, ISeriesApi<'Line'>>>({});

  const [chartType, setChartType] = useState<ChartType>('candlestick');
  const [timeframe, setTimeframe] = useState<TimeframeKey>('1Y');
  const [activeIndicators, setActiveIndicators] = useState<Set<string>>(new Set(['sma20', 'sma50']));
  const [showVolume, setShowVolume] = useState(true);
  const [crosshairData, setCrosshairData] = useState<{
    time?: string; open?: number; high?: number; low?: number; close?: number; volume?: number;
  }>({});

  // Chart theme matching our dark design
  const chartTheme = {
    background: { type: ColorType.Solid, color: '#111827' },
    textColor: '#64748b',
    grid: { vertLines: { color: '#1e2d4a' }, horzLines: { color: '#1e2d4a' } },
    crosshair: { mode: CrosshairMode.Normal },
    rightPriceScale: { borderColor: '#1e2d4a' },
    timeScale: { borderColor: '#1e2d4a', timeVisible: true, secondsVisible: false },
  };

  // Initialize chart
  useEffect(() => {
    if (!chartContainerRef.current) return;

    const chart = createChart(chartContainerRef.current, {
      ...chartTheme,
      width: chartContainerRef.current.clientWidth,
      height: 460,
      layout: { background: chartTheme.background, textColor: chartTheme.textColor },
      grid: chartTheme.grid,
      crosshair: chartTheme.crosshair,
      rightPriceScale: chartTheme.rightPriceScale,
      timeScale: chartTheme.timeScale,
    });

    chartRef.current = chart;

    // Candlestick series
    const cs = chart.addCandlestickSeries({
      upColor: '#22c55e',
      downColor: '#ef4444',
      borderUpColor: '#22c55e',
      borderDownColor: '#ef4444',
      wickUpColor: '#22c55e',
      wickDownColor: '#ef4444',
    });
    candlestickRef.current = cs;

    // Volume series (in a separate pane)
    const vol = chart.addHistogramSeries({
      priceFormat: { type: 'volume' },
      priceScaleId: 'volume',
    });
    chart.priceScale('volume').applyOptions({
      scaleMargins: { top: 0.8, bottom: 0 },
      borderVisible: false,
    });
    volumeRef.current = vol;

    // Crosshair subscription
    chart.subscribeCrosshairMove((param) => {
      if (!param.point || !param.time) { setCrosshairData({}); return; }
      const data = param.seriesData.get(cs) as CandlestickData | undefined;
      if (data) {
        setCrosshairData({
          time: String(param.time),
          open: data.open, high: data.high, low: data.low, close: data.close,
        });
      }
    });

    // Resize observer
    const observer = new ResizeObserver(() => {
      if (chartContainerRef.current && chartRef.current) {
        chartRef.current.applyOptions({ width: chartContainerRef.current.clientWidth });
      }
    });
    observer.observe(chartContainerRef.current);

    return () => {
      observer.disconnect();
      chart.remove();
      chartRef.current = null;
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Load candle data
  useEffect(() => {
    if (!candles.length || !candlestickRef.current || !volumeRef.current || !chartRef.current) return;

    const sorted = [...candles].sort((a, b) => a.date.localeCompare(b.date));

    const csData: CandlestickData[] = sorted.map(c => ({
      time: c.date as Time,
      open: c.open,
      high: c.high,
      low: c.low,
      close: c.close,
    }));

    const volData = sorted.map(c => ({
      time: c.date as Time,
      value: c.volume,
      color: c.close >= c.open ? 'rgba(34,197,94,0.4)' : 'rgba(239,68,68,0.4)',
    }));

    candlestickRef.current.setData(csData);
    volumeRef.current.setData(volData);
    chartRef.current.timeScale().fitContent();
  }, [candles]);

  // Load technical overlays
  useEffect(() => {
    if (!technicals || !chartRef.current) return;

    const dates = technicals.dates;
    const colors: Record<string, string> = {
      sma20: '#3b82f6', sma50: '#f59e0b', sma200: '#ef4444', 'bb20-upper': '#8b5cf6', 'bb20-lower': '#8b5cf6',
    };

    // Remove old indicator series
    Object.values(smaRefs.current).forEach(s => {
      try { chartRef.current?.removeSeries(s); } catch {}
    });
    smaRefs.current = {};

    // Add new ones
    INDICATOR_OPTIONS.forEach(({ key }) => {
      if (!activeIndicators.has(key)) return;

      const data = (technicals as Record<string, unknown>)[key];
      if (!data) return;

      if (key.startsWith('sma')) {
        const arr = data as (number | null)[];
        const series = chartRef.current!.addLineSeries({
          color: colors[key],
          lineWidth: 1,
          priceLineVisible: false,
          lastValueVisible: false,
        });
        series.setData(
          arr.map((v, i) => ({ time: dates[i] as Time, value: v! })).filter(d => d.value != null)
        );
        smaRefs.current[key] = series;
      } else if (key === 'bb20' && typeof data === 'object' && data !== null) {
        const bb = data as { upper: (number|null)[]; middle: (number|null)[]; lower: (number|null)[] };
        ['upper', 'lower'].forEach(band => {
          const arr = bb[band as 'upper' | 'lower'];
          const series = chartRef.current!.addLineSeries({
            color: '#8b5cf6',
            lineWidth: 1,
            lineStyle: LineStyle.Dashed,
            priceLineVisible: false,
            lastValueVisible: false,
          });
          series.setData(arr.map((v, i) => ({ time: dates[i] as Time, value: v! })).filter(d => d.value != null));
          smaRefs.current[`${key}-${band}`] = series;
        });
      }
    });
  }, [technicals, activeIndicators]);

  function toggleIndicator(key: string) {
    setActiveIndicators(prev => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key); else next.add(key);
      return next;
    });
  }

  function handleTimeframe(tf: TimeframeKey) {
    setTimeframe(tf);
    onTimeframeChange?.(tf);
  }

  const priceColor = crosshairData.close && crosshairData.open
    ? crosshairData.close >= crosshairData.open ? 'var(--color-green)' : 'var(--color-red)'
    : 'var(--color-text-primary)';

  return (
    <div className="chart-container">
      {/* Chart toolbar */}
      <div style={{
        display: 'flex', alignItems: 'center', gap: '8px',
        padding: '10px 14px',
        borderBottom: '1px solid var(--color-border)',
        flexWrap: 'wrap',
      }}>
        {/* Chart type */}
        <div className="tab-bar" style={{ gap: 2 }}>
          <button className={`tab-item ${chartType === 'candlestick' ? 'active' : ''}`}
            onClick={() => setChartType('candlestick')} title="Candlestick">
            <CandlestickChart size={13} />
          </button>
          <button className={`tab-item ${chartType === 'line' ? 'active' : ''}`}
            onClick={() => setChartType('line')} title="Line">
            <TrendingUp size={13} />
          </button>
          <button className={`tab-item ${chartType === 'area' ? 'active' : ''}`}
            onClick={() => setChartType('area')} title="Area">
            <Activity size={13} />
          </button>
        </div>

        <div style={{ width: 1, height: 20, background: 'var(--color-border)' }} />

        {/* Timeframes */}
        <div className="tab-bar">
          {TIMEFRAMES.map(({ label }) => (
            <button
              key={label}
              className={`tab-item ${timeframe === label ? 'active' : ''}`}
              onClick={() => handleTimeframe(label)}
            >
              {label}
            </button>
          ))}
        </div>

        <div style={{ width: 1, height: 20, background: 'var(--color-border)' }} />

        {/* Indicator toggles */}
        {INDICATOR_OPTIONS.map(({ key, label, color }) => (
          <button
            key={key}
            className={`btn btn-xs ${activeIndicators.has(key) ? '' : 'btn-ghost'}`}
            style={{
              borderColor: activeIndicators.has(key) ? color : undefined,
              color: activeIndicators.has(key) ? color : undefined,
              background: activeIndicators.has(key) ? `${color}18` : undefined,
            }}
            onClick={() => toggleIndicator(key)}
          >
            {label}
          </button>
        ))}

        {/* Crosshair data */}
        {crosshairData.close && (
          <div style={{ marginLeft: 'auto', display: 'flex', gap: 12, fontSize: '12px' }}>
            {[
              { l: 'O', v: crosshairData.open },
              { l: 'H', v: crosshairData.high },
              { l: 'L', v: crosshairData.low },
              { l: 'C', v: crosshairData.close },
            ].map(({ l, v }) => (
              <span key={l} style={{ color: 'var(--color-text-muted)' }}>
                {l}: <span style={{ color: priceColor, fontWeight: 600 }}>{v?.toFixed(2)}</span>
              </span>
            ))}
          </div>
        )}
      </div>

      {/* Chart */}
      <div style={{ position: 'relative' }}>
        {isLoading && (
          <div style={{
            position: 'absolute', inset: 0, background: 'rgba(9,14,26,0.7)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            zIndex: 10,
          }}>
            <div style={{ fontSize: '13px', color: 'var(--color-text-muted)' }}>Loading chart data…</div>
          </div>
        )}
        <div ref={chartContainerRef} style={{ width: '100%' }} />
      </div>
    </div>
  );
}
