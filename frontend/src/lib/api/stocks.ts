import apiClient from './client';
import type { StockSearchResult, StockDetail, HistoricalCandle, TechnicalsResponse } from '@/types/stock';

export const stocksApi = {
  /** Search stocks by symbol or name — served from local DB, NO Marketstack call */
  search: async (query: string, limit = 20): Promise<StockSearchResult[]> => {
    const { data } = await apiClient.get('/stocks/search', {
      params: { q: query, size: limit },
    });
    return data?.content ?? data ?? [];
  },

  /** Get stock quote + profile */
  getDetail: async (symbol: string): Promise<StockDetail> => {
    const { data } = await apiClient.get(`/stocks/${symbol}`);
    return data;
  },

  /** Get historical OHLCV data */
  getHistorical: async (
    symbol: string,
    from?: string,
    to?: string,
    interval = 'daily'
  ): Promise<HistoricalCandle[]> => {
    const { data } = await apiClient.get(`/stocks/${symbol}/historical`, {
      params: { from, to, interval },
    });
    return data ?? [];
  },

  /** Get technical indicators (computed server-side from cached prices) */
  getTechnicals: async (
    symbol: string,
    indicators = 'sma20,sma50,sma200,rsi14,macd,bb20'
  ): Promise<TechnicalsResponse> => {
    const { data } = await apiClient.get(`/stocks/${symbol}/technicals`, {
      params: { indicators },
    });
    return data;
  },

  /** Get data freshness metadata */
  getDataFreshness: async (symbol: string) => {
    const { data } = await apiClient.get(`/stocks/${symbol}/data-freshness`);
    return data;
  },
};

export const adminApi = {
  getStats: async () => {
    const { data } = await apiClient.get('/admin/stats');
    return data;
  },
  getQuota: async () => {
    const { data } = await apiClient.get('/admin/quota');
    return data;
  },
};
