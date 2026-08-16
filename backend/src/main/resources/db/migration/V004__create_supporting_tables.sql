-- V004: Supporting tables (watchlists, portfolio, annotations, jobs, quota)

-- Watchlists
CREATE TABLE IF NOT EXISTS watchlists (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS watchlist_items (
    id            BIGSERIAL PRIMARY KEY,
    watchlist_id  BIGINT NOT NULL REFERENCES watchlists(id) ON DELETE CASCADE,
    stock_id      BIGINT NOT NULL REFERENCES stocks(id),
    display_order INT DEFAULT 0,
    added_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(watchlist_id, stock_id)
);

-- Splits & Dividends
CREATE TABLE IF NOT EXISTS splits (
    id           BIGSERIAL PRIMARY KEY,
    stock_id     BIGINT NOT NULL REFERENCES stocks(id),
    date         DATE NOT NULL,
    split_factor DECIMAL(10,4) NOT NULL,
    created_at   TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(stock_id, date)
);

CREATE TABLE IF NOT EXISTS dividends (
    id         BIGSERIAL PRIMARY KEY,
    stock_id   BIGINT NOT NULL REFERENCES stocks(id),
    date       DATE NOT NULL,
    amount     DECIMAL(12,6) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(stock_id, date)
);

-- Chart annotations
CREATE TABLE IF NOT EXISTS chart_annotations (
    id          BIGSERIAL PRIMARY KEY,
    stock_id    BIGINT NOT NULL REFERENCES stocks(id),
    session_id  VARCHAR(100),
    timeframe   VARCHAR(10) NOT NULL,
    type        VARCHAR(50) NOT NULL,
    data        JSONB NOT NULL,
    label       VARCHAR(255),
    is_visible  BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_annotations_stock_tf ON chart_annotations(stock_id, timeframe);

-- Background jobs
CREATE TABLE IF NOT EXISTS background_jobs (
    id            BIGSERIAL PRIMARY KEY,
    type          VARCHAR(100) NOT NULL,
    payload       JSONB NOT NULL,
    status        VARCHAR(20) DEFAULT 'PENDING',
    priority      INT DEFAULT 5,
    attempts      INT DEFAULT 0,
    max_attempts  INT DEFAULT 3,
    error_message TEXT,
    scheduled_at  TIMESTAMPTZ DEFAULT NOW(),
    started_at    TIMESTAMPTZ,
    completed_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_jobs_status_priority ON background_jobs(status, priority DESC, scheduled_at);

-- API quota tracking
CREATE TABLE IF NOT EXISTS api_quota_usage (
    id               BIGSERIAL PRIMARY KEY,
    provider         VARCHAR(50) NOT NULL DEFAULT 'marketstack',
    date             DATE NOT NULL DEFAULT CURRENT_DATE,
    requests_total   INT DEFAULT 0,
    requests_success INT DEFAULT 0,
    requests_failed  INT DEFAULT 0,
    quota_used       INT DEFAULT 0,
    quota_limit      INT,
    updated_at       TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(provider, date)
);

-- API cache metadata
CREATE TABLE IF NOT EXISTS api_cache_metadata (
    id              BIGSERIAL PRIMARY KEY,
    cache_key       VARCHAR(500) UNIQUE NOT NULL,
    data_type       VARCHAR(100) NOT NULL,
    fetched_at      TIMESTAMPTZ NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    is_stale        BOOLEAN DEFAULT FALSE,
    source_provider VARCHAR(50) DEFAULT 'marketstack'
);

CREATE INDEX IF NOT EXISTS idx_cache_meta_key     ON api_cache_metadata(cache_key);
CREATE INDEX IF NOT EXISTS idx_cache_meta_expires ON api_cache_metadata(expires_at);

-- Price alerts (Phase 2, schema ready now)
CREATE TABLE IF NOT EXISTS price_alerts (
    id           BIGSERIAL PRIMARY KEY,
    stock_id     BIGINT NOT NULL REFERENCES stocks(id),
    condition    VARCHAR(20) NOT NULL CHECK (condition IN ('ABOVE', 'BELOW')),
    price        DECIMAL(18,6) NOT NULL,
    is_active    BOOLEAN DEFAULT TRUE,
    triggered_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ DEFAULT NOW()
);

-- AI analysis (Phase 3, schema ready now)
CREATE TABLE IF NOT EXISTS ai_analysis (
    id               BIGSERIAL PRIMARY KEY,
    stock_id         BIGINT NOT NULL REFERENCES stocks(id),
    analysis_version VARCHAR(20) DEFAULT 'v1',
    input_summary    JSONB NOT NULL,
    output           JSONB NOT NULL,
    model_used       VARCHAR(100),
    token_input      INT,
    token_output     INT,
    expires_at       TIMESTAMPTZ,
    created_at       TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ai_stock_date ON ai_analysis(stock_id, created_at DESC);
