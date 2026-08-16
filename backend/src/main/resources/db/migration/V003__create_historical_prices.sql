-- V003: Create historical_prices table (partitioned by year)
CREATE TABLE IF NOT EXISTS historical_prices (
    id           BIGSERIAL,
    stock_id     BIGINT NOT NULL REFERENCES stocks(id),
    date         DATE NOT NULL,
    open         DECIMAL(18,6),
    high         DECIMAL(18,6),
    low          DECIMAL(18,6),
    close        DECIMAL(18,6),
    adj_open     DECIMAL(18,6),
    adj_high     DECIMAL(18,6),
    adj_low      DECIMAL(18,6),
    adj_close    DECIMAL(18,6),
    volume       BIGINT,
    adj_volume   BIGINT,
    split_factor DECIMAL(10,4) DEFAULT 1,
    dividend     DECIMAL(12,6) DEFAULT 0,
    data_source  VARCHAR(50) DEFAULT 'marketstack',
    created_at   TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (id, date)
) PARTITION BY RANGE (date);

-- Partitions
CREATE TABLE IF NOT EXISTS historical_prices_2020 PARTITION OF historical_prices FOR VALUES FROM ('2020-01-01') TO ('2021-01-01');
CREATE TABLE IF NOT EXISTS historical_prices_2021 PARTITION OF historical_prices FOR VALUES FROM ('2021-01-01') TO ('2022-01-01');
CREATE TABLE IF NOT EXISTS historical_prices_2022 PARTITION OF historical_prices FOR VALUES FROM ('2022-01-01') TO ('2023-01-01');
CREATE TABLE IF NOT EXISTS historical_prices_2023 PARTITION OF historical_prices FOR VALUES FROM ('2023-01-01') TO ('2024-01-01');
CREATE TABLE IF NOT EXISTS historical_prices_2024 PARTITION OF historical_prices FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');
CREATE TABLE IF NOT EXISTS historical_prices_2025 PARTITION OF historical_prices FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');
CREATE TABLE IF NOT EXISTS historical_prices_2026 PARTITION OF historical_prices FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');
CREATE TABLE IF NOT EXISTS historical_prices_default PARTITION OF historical_prices DEFAULT;

-- Indexes
CREATE UNIQUE INDEX IF NOT EXISTS idx_hp_stock_date ON historical_prices(stock_id, date);
CREATE INDEX IF NOT EXISTS idx_hp_date ON historical_prices(date);
CREATE INDEX IF NOT EXISTS idx_hp_stock_id ON historical_prices(stock_id);
