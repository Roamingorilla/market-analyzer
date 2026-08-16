package com.marketanalyser.shared.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Normalized company fundamentals — populated from Alpha Vantage or seed data.
 * Marketstack does NOT provide these fields.
 */
@Data
@Builder
public class CompanyProfile {
    private String symbol;
    private String name;
    private String exchange;
    private String currencyCode;
    private String country;
    private String sector;
    private String industry;
    private String description;
    private String website;
    private String logoUrl;

    // Fundamentals
    private Long marketCap;
    private BigDecimal peRatio;
    private BigDecimal pegRatio;
    private BigDecimal eps;
    private BigDecimal beta;
    private BigDecimal dividendYield;
    private BigDecimal dividendPerShare;
    private BigDecimal bookValue;
    private BigDecimal profitMargin;
    private Long sharesOutstanding;

    // 52-week range
    private BigDecimal week52High;
    private BigDecimal week52Low;

    // Moving averages from AV
    private BigDecimal ma50Day;
    private BigDecimal ma200Day;

    // Data freshness
    private OffsetDateTime fetchedAt;
    private String dataSource; // "alphavantage", "seed", "manual"
}
