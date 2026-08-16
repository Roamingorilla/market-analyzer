'use client';

import { useState, useEffect, useRef } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { stocksApi } from '@/lib/api/stocks';
import type { StockSearchResult } from '@/types/stock';
import {
  Search, TrendingUp, BarChart3, Briefcase, Bell,
  Activity, BookMarked, Settings, Zap, X
} from 'lucide-react';

const NAV_LINKS = [
  { href: '/',          icon: BarChart3,  label: 'Dashboard' },
  { href: '/screener',  icon: Activity,   label: 'Screener'  },
  { href: '/watchlist', icon: BookMarked, label: 'Watchlist' },
  { href: '/portfolio', icon: Briefcase,  label: 'Portfolio' },
];

export function Topbar() {
  const router = useRouter();
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<StockSearchResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);
  const inputRef = useRef<HTMLInputElement>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const debounceRef = useRef<ReturnType<typeof setTimeout>>();

  useEffect(() => {
    if (query.trim().length < 1) {
      setResults([]);
      setOpen(false);
      return;
    }

    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(async () => {
      setLoading(true);
      try {
        const data = await stocksApi.search(query.trim(), 8);
        setResults(data);
        setOpen(data.length > 0);
        setActiveIndex(-1);
      } catch {
        setResults([]);
      } finally {
        setLoading(false);
      }
    }, 200);

    return () => clearTimeout(debounceRef.current);
  }, [query]);

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Keyboard shortcut: Cmd+K / Ctrl+K
  useEffect(() => {
    function handleKey(e: KeyboardEvent) {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault();
        inputRef.current?.focus();
        inputRef.current?.select();
      }
    }
    document.addEventListener('keydown', handleKey);
    return () => document.removeEventListener('keydown', handleKey);
  }, []);

  function handleKeyDown(e: React.KeyboardEvent) {
    if (!open) return;
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setActiveIndex(i => Math.min(i + 1, results.length - 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setActiveIndex(i => Math.max(i - 1, -1));
    } else if (e.key === 'Enter' && activeIndex >= 0) {
      selectResult(results[activeIndex]);
    } else if (e.key === 'Escape') {
      setOpen(false);
    }
  }

  function selectResult(stock: StockSearchResult) {
    setQuery('');
    setOpen(false);
    router.push(`/stocks/${stock.symbol}`);
  }

  return (
    <header style={{
      display: 'flex',
      alignItems: 'center',
      gap: '24px',
      padding: '0 20px',
      height: '52px',
      background: 'var(--color-bg-secondary)',
      borderBottom: '1px solid var(--color-border)',
      position: 'sticky',
      top: 0,
      zIndex: 100,
    }}>
      {/* Logo */}
      <Link href="/" style={{ display: 'flex', alignItems: 'center', gap: '8px', textDecoration: 'none' }}>
        <div style={{
          width: 28, height: 28, borderRadius: '6px',
          background: 'linear-gradient(135deg, #3b82f6, #8b5cf6)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <TrendingUp size={16} color="white" />
        </div>
        <span style={{ fontSize: '14px', fontWeight: 700, color: 'var(--color-text-primary)', letterSpacing: '-0.02em' }}>
          MarketAnalyser
        </span>
      </Link>

      {/* Nav links */}
      <nav style={{ display: 'flex', gap: '2px' }}>
        {NAV_LINKS.map(({ href, icon: Icon, label }) => (
          <Link key={href} href={href} className="nav-item" style={{ fontSize: '13px', padding: '5px 10px' }}>
            <Icon size={14} />
            {label}
          </Link>
        ))}
      </nav>

      {/* Search */}
      <div ref={dropdownRef} style={{ position: 'relative', flex: '0 0 320px', marginLeft: 'auto' }}>
        <div style={{ position: 'relative' }}>
          <Search size={14} style={{
            position: 'absolute', left: 10, top: '50%', transform: 'translateY(-50%)',
            color: 'var(--color-text-muted)',
          }} />
          <input
            ref={inputRef}
            id="global-stock-search"
            className="input"
            type="text"
            placeholder="Search stocks… (⌘K)"
            value={query}
            onChange={e => setQuery(e.target.value)}
            onKeyDown={handleKeyDown}
            onFocus={() => results.length > 0 && setOpen(true)}
            style={{ paddingLeft: '32px', paddingRight: query ? '32px' : '10px' }}
          />
          {query && (
            <button
              onClick={() => { setQuery(''); setOpen(false); }}
              style={{
                position: 'absolute', right: 8, top: '50%', transform: 'translateY(-50%)',
                background: 'none', border: 'none', cursor: 'pointer',
                color: 'var(--color-text-muted)', display: 'flex', alignItems: 'center',
              }}
            >
              <X size={12} />
            </button>
          )}
        </div>

        {/* Dropdown */}
        {open && (
          <div style={{
            position: 'absolute',
            top: 'calc(100% + 6px)',
            left: 0,
            right: 0,
            background: 'var(--color-bg-panel)',
            border: '1px solid var(--color-border-light)',
            borderRadius: 'var(--radius-lg)',
            boxShadow: '0 16px 48px rgba(0,0,0,0.5)',
            overflow: 'hidden',
            zIndex: 200,
          }}>
            {loading && (
              <div style={{ padding: '12px', textAlign: 'center', fontSize: '12px', color: 'var(--color-text-muted)' }}>
                Searching…
              </div>
            )}
            {!loading && results.map((stock, i) => (
              <button
                key={`${stock.symbol}-${stock.exchangeMic}`}
                onClick={() => selectResult(stock)}
                style={{
                  width: '100%',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '10px',
                  padding: '10px 14px',
                  background: i === activeIndex ? 'var(--color-bg-hover)' : 'transparent',
                  border: 'none',
                  cursor: 'pointer',
                  borderBottom: i < results.length - 1 ? '1px solid var(--color-border)' : 'none',
                  textAlign: 'left',
                }}
              >
                <div style={{
                  width: 32, height: 32, borderRadius: 6,
                  background: 'var(--color-bg-active)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  flexShrink: 0,
                }}>
                  <span style={{ fontSize: '10px', fontWeight: 700, color: 'var(--color-accent)' }}>
                    {stock.symbol.slice(0, 3)}
                  </span>
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={{ fontSize: '13px', fontWeight: 700, color: 'var(--color-text-primary)' }}>
                      {stock.symbol}
                    </span>
                    <span style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>
                      {stock.exchange || stock.exchangeMic}
                    </span>
                  </div>
                  <div style={{ fontSize: '12px', color: 'var(--color-text-secondary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {stock.name}
                  </div>
                </div>
                {stock.sector && (
                  <span className="badge badge-blue" style={{ flexShrink: 0 }}>{stock.sector}</span>
                )}
              </button>
            ))}
            {!loading && results.length === 0 && query.length > 1 && (
              <div style={{ padding: '16px', textAlign: 'center', fontSize: '12px', color: 'var(--color-text-muted)' }}>
                No results for "{query}"
              </div>
            )}
          </div>
        )}
      </div>

      {/* Right controls */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
        <button className="btn btn-ghost btn-sm" style={{ padding: '5px', border: 'none' }} title="Alerts">
          <Bell size={16} />
        </button>
        <button className="btn btn-ghost btn-sm" style={{ padding: '5px', border: 'none' }} title="Settings">
          <Settings size={16} />
        </button>
        <div style={{ width: 1, height: 20, background: 'var(--color-border)', margin: '0 4px' }} />
        <div style={{
          display: 'flex', alignItems: 'center', gap: 6,
          padding: '4px 10px',
          background: 'rgba(34,197,94,0.08)',
          border: '1px solid rgba(34,197,94,0.2)',
          borderRadius: 'var(--radius-md)',
        }}>
          <Zap size={11} color="var(--color-green)" />
          <span style={{ fontSize: '11px', fontWeight: 600, color: 'var(--color-green)' }}>LIVE</span>
        </div>
      </div>
    </header>
  );
}
