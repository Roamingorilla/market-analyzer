-- V001: Create exchanges table
CREATE TABLE IF NOT EXISTS exchanges (
    id           BIGSERIAL PRIMARY KEY,
    mic          VARCHAR(10)  UNIQUE NOT NULL,
    name         VARCHAR(255) NOT NULL,
    acronym      VARCHAR(20),
    country      VARCHAR(100),
    country_code CHAR(2),
    city         VARCHAR(100),
    website      VARCHAR(255),
    timezone     VARCHAR(100),
    currency_code VARCHAR(10),
    open_time    TIME,
    close_time   TIME,
    created_at   TIMESTAMPTZ DEFAULT NOW(),
    updated_at   TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_exchanges_mic ON exchanges(mic);
CREATE INDEX IF NOT EXISTS idx_exchanges_country ON exchanges(country_code);
