export interface StockSearchResult {
  symbol: string;
  name: string;
  exchange: string;
  exchangeMic: string;
  country: string;
  sector: string | null;
  currency: string;
  hasEod: boolean;
  hasIntraday: boolean;
}

export interface StockDetail {
  // Identity
  symbol: string;
  name: string;
  exchange: string;
  exchangeMic: string;
  assetType: string;
  currency: string;
  country: string;
  sector: string | null;
  industry: string | null;
  website: string | null;
  description: string | null;
  logoUrl: string | null;

  // Price data
  price: number;
  open: number;
  high: number;
  low: number;
  prevClose: number | null;
  change: number | null;
  changePct: number | null;
  volume: number;

  // Fundamentals
  marketCap: number | null;
  peRatio: number | null;
  eps: number | null;
  beta: number | null;
  dividendYield: number | null;

  // Flags
  hasIntraday: boolean;
  hasEod: boolean;
  lastPriceDate: string;

  // Freshness
  dataFreshness: 'LIVE' | 'DELAYED' | 'CACHED' | 'HISTORICAL';
  lastUpdated: string;
  isStale: boolean;
}

export interface HistoricalCandle {
  date: string;
  open: number;
  high: number;
  low: number;
  close: number;
  adjClose: number;
  volume: number;
  symbol: string;
  dataSource: string;
}

export interface TechnicalsResponse {
  dates: string[];
  sma20?: (number | null)[];
  sma50?: (number | null)[];
  sma200?: (number | null)[];
  rsi14?: (number | null)[];
  macd?: {
    line: (number | null)[];
    signal: (number | null)[];
    histogram: (number | null)[];
  };
  bb20?: {
    upper: (number | null)[];
    middle: (number | null)[];
    lower: (number | null)[];
  };
  dataFreshness: string;
  computedAt: string;
}
