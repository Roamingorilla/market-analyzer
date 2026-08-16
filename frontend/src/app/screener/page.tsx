'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Topbar } from '@/components/layout/Topbar';
import { stocksApi } from '@/lib/api/stocks';
import apiClient from '@/lib/api/client';
import { Filter, SlidersHorizontal, Search } from 'lucide-react';
import type { StockSearchResult } from '@/types/stock';

export default function ScreenerPage() {
  const [sector, setSector] = useState('');
  const [exchange, setExchange] = useState('');
  const [country, setCountry] = useState('');
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: ['screener', sector, exchange, country, page],
    queryFn: async () => {
      const res = await apiClient.get('/screener', {
        params: { sector: sector || undefined, exchange: exchange || undefined, country: country || undefined, page, size: 20 }
      });
      return res.data;
    },
    staleTime: 5 * 60 * 1000,
  });

  const stocks: StockSearchResult[] = data?.content || [];
  const totalElements = data?.totalElements || 0;
  const totalPages = data?.totalPages || 0;

  return (
    <div className="app-shell">
      <Topbar />
      <main style={{ padding: '24px', display: 'flex', gap: '24px', height: 'calc(100vh - 52px)', overflow: 'hidden' }}>
        
        {/* Sidebar Filters */}
        <div className="panel" style={{ width: 280, display: 'flex', flexDirection: 'column', flexShrink: 0 }}>
          <div className="panel-header">
            <span className="panel-title" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <Filter size={14} /> Filters
            </span>
          </div>
          <div style={{ padding: 16, display: 'flex', flexDirection: 'column', gap: 16, overflowY: 'auto' }}>
            <div>
              <label style={{ display: 'block', fontSize: 12, fontWeight: 600, color: 'var(--color-text-muted)', marginBottom: 6 }}>Sector</label>
              <select className="input" value={sector} onChange={e => { setSector(e.target.value); setPage(0); }}>
                <option value="">Any Sector</option>
                <option value="Technology">Technology</option>
                <option value="Finance">Finance</option>
                <option value="Healthcare">Healthcare</option>
                <option value="Consumer Services">Consumer Services</option>
                <option value="Energy">Energy</option>
              </select>
            </div>
            <div>
              <label style={{ display: 'block', fontSize: 12, fontWeight: 600, color: 'var(--color-text-muted)', marginBottom: 6 }}>Exchange</label>
              <select className="input" value={exchange} onChange={e => { setExchange(e.target.value); setPage(0); }}>
                <option value="">Any Exchange</option>
                <option value="NASDAQ">NASDAQ</option>
                <option value="NYSE">NYSE</option>
                <option value="LSE">London Stock Exchange</option>
                <option value="BSE">BSE</option>
                <option value="NSE">NSE</option>
              </select>
            </div>
            <div>
              <label style={{ display: 'block', fontSize: 12, fontWeight: 600, color: 'var(--color-text-muted)', marginBottom: 6 }}>Country</label>
              <select className="input" value={country} onChange={e => { setCountry(e.target.value); setPage(0); }}>
                <option value="">Any Country</option>
                <option value="USA">United States</option>
                <option value="UK">United Kingdom</option>
                <option value="India">India</option>
              </select>
            </div>
          </div>
        </div>

        {/* Results Area */}
        <div className="panel" style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
          <div className="panel-header" style={{ borderBottom: '1px solid var(--color-border)' }}>
            <span className="panel-title">Results ({totalElements})</span>
            <div style={{ display: 'flex', gap: 8 }}>
              <button className="btn btn-ghost btn-sm" disabled={page === 0} onClick={() => setPage(p => p - 1)}>Prev</button>
              <span style={{ fontSize: 13, color: 'var(--color-text-muted)', display: 'flex', alignItems: 'center', padding: '0 8px' }}>
                Page {page + 1} of {Math.max(1, totalPages)}
              </span>
              <button className="btn btn-ghost btn-sm" disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>Next</button>
            </div>
          </div>
          
          <div style={{ flex: 1, overflowY: 'auto' }}>
            <table className="data-table">
              <thead style={{ position: 'sticky', top: 0, zIndex: 10 }}>
                <tr>
                  <th>Symbol</th>
                  <th>Name</th>
                  <th>Sector</th>
                  <th>Exchange</th>
                  <th>Country</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {isLoading ? (
                  <tr><td colSpan={6} style={{ textAlign: 'center', padding: 32, color: 'var(--color-text-muted)' }}>Loading...</td></tr>
                ) : stocks.length === 0 ? (
                  <tr><td colSpan={6} style={{ textAlign: 'center', padding: 32, color: 'var(--color-text-muted)' }}>No stocks found matching criteria.</td></tr>
                ) : (
                  stocks.map(stock => (
                    <tr key={stock.symbol}>
                      <td><span style={{ fontWeight: 600, color: 'var(--color-text-primary)' }}>{stock.symbol}</span></td>
                      <td>{stock.name}</td>
                      <td>{stock.sector ? <span className="badge badge-blue">{stock.sector}</span> : '—'}</td>
                      <td>{stock.exchange}</td>
                      <td>{stock.country || '—'}</td>
                      <td>
                        <a href={`/stocks/${stock.symbol}`} className="btn btn-ghost btn-xs">View</a>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

      </main>
    </div>
  );
}
