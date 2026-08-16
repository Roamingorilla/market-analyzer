package com.marketanalyser.shared.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Normalized market quote — wraps most-recent EOD data.
 */
@Data
@Builder
public class MarketQuote {
    private String symbol;
    private String name;
    private String exchange;
    private String exchangeCode;
    private String assetType;
    private String currencyCode;

    // Price data
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private BigDecimal adjClose;
    private Long volume;

    // Change calculations (computed, not from API)
    private BigDecimal change;
    private BigDecimal changePct;

    // Date
    private LocalDate date;

    // Metadata
    private OffsetDateTime fetchedAt;
    private boolean isStale;
    private String dataFreshness; // "LIVE", "DELAYED", "CACHED", "HISTORICAL"
}
