-- V002: Create stocks table
CREATE TABLE IF NOT EXISTS stocks (
    id                   BIGSERIAL PRIMARY KEY,
    symbol               VARCHAR(20)  NOT NULL,
    name                 VARCHAR(500),
    exchange_id          BIGINT REFERENCES exchanges(id),
    has_intraday         BOOLEAN DEFAULT FALSE,
    has_eod              BOOLEAN DEFAULT TRUE,
    asset_type           VARCHAR(50),
    currency_code        VARCHAR(10),
    country              VARCHAR(100),
    sector               VARCHAR(100),
    industry             VARCHAR(100),
    market_cap           BIGINT,
    pe_ratio             DECIMAL(12,4),
    eps                  DECIMAL(12,4),
    beta                 DECIMAL(8,4),
    description          TEXT,
    logo_url             VARCHAR(500),
    company_website      VARCHAR(500),
    is_active            BOOLEAN DEFAULT TRUE,
    fundamentals_fetched_at TIMESTAMPTZ,
    created_at           TIMESTAMPTZ DEFAULT NOW(),
    updated_at           TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uq_stock_symbol_exchange UNIQUE (symbol, exchange_id)
);

CREATE INDEX IF NOT EXISTS idx_stocks_symbol     ON stocks(symbol);
CREATE INDEX IF NOT EXISTS idx_stocks_symbol_upper ON stocks(UPPER(symbol));
CREATE INDEX IF NOT EXISTS idx_stocks_name_upper ON stocks(UPPER(name));
CREATE INDEX IF NOT EXISTS idx_stocks_sector     ON stocks(sector);
CREATE INDEX IF NOT EXISTS idx_stocks_exchange   ON stocks(exchange_id);
CREATE INDEX IF NOT EXISTS idx_stocks_active     ON stocks(is_active);
