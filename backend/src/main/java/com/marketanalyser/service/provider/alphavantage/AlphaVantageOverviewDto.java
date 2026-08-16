package com.marketanalyser.service.provider.alphavantage;

import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Alpha Vantage OVERVIEW endpoint response DTO.
 * Free tier: 25 requests/day — we cache this for 30 days.
 */
@Data
@NoArgsConstructor
public class AlphaVantageOverviewDto {
    @JsonProperty("Symbol") private String symbol;
    @JsonProperty("AssetType") private String assetType;
    @JsonProperty("Name") private String name;
    @JsonProperty("Description") private String description;
    @JsonProperty("CIK") private String cik;
    @JsonProperty("Exchange") private String exchange;
    @JsonProperty("Currency") private String currency;
    @JsonProperty("Country") private String country;
    @JsonProperty("Sector") private String sector;
    @JsonProperty("Industry") private String industry;
    @JsonProperty("Address") private String address;
    @JsonProperty("OfficialSite") private String officialSite;
    @JsonProperty("MarketCapitalization") private String marketCapitalization;
    @JsonProperty("EBITDA") private String ebitda;
    @JsonProperty("PERatio") private String peRatio;
    @JsonProperty("PEGRatio") private String pegRatio;
    @JsonProperty("BookValue") private String bookValue;
    @JsonProperty("DividendPerShare") private String dividendPerShare;
    @JsonProperty("DividendYield") private String dividendYield;
    @JsonProperty("EPS") private String eps;
    @JsonProperty("RevenuePerShareTTM") private String revenuePerShareTTM;
    @JsonProperty("ProfitMargin") private String profitMargin;
    @JsonProperty("Beta") private String beta;
    @JsonProperty("52WeekHigh") private String week52High;
    @JsonProperty("52WeekLow") private String week52Low;
    @JsonProperty("50DayMovingAverage") private String ma50Day;
    @JsonProperty("200DayMovingAverage") private String ma200Day;
    @JsonProperty("SharesOutstanding") private String sharesOutstanding;
}
