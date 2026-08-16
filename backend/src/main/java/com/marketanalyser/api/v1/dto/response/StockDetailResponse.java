package com.marketanalyser.api.v1.dto.response;

import com.marketanalyser.domain.model.Stock;
import com.marketanalyser.shared.model.MarketQuote;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data @Builder
public class StockDetailResponse {
    // Identity
    private String symbol;
    private String name;
    private String exchange;
    private String exchangeMic;
    private String assetType;
    private String currency;
    private String country;
    private String sector;
    private String industry;
    private String website;
    private String description;
    private String logoUrl;

    // Price data
    private BigDecimal price;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal prevClose;
    private BigDecimal change;
    private BigDecimal changePct;
    private Long volume;

    // Fundamentals (from Alpha Vantage, may be null)
    private Long marketCap;
    private BigDecimal peRatio;
    private BigDecimal eps;
    private BigDecimal beta;
    private BigDecimal dividendYield;

    // Data availability flags
    private boolean hasIntraday;
    private boolean hasEod;
    private LocalDate lastPriceDate;

    // Freshness
    private String dataFreshness;
    private OffsetDateTime lastUpdated;
    private boolean isStale;

    public static StockDetailResponse from(MarketQuote quote, Stock stock,
                                            BigDecimal prevClose, BigDecimal change, BigDecimal changePct) {
        var builder = StockDetailResponse.builder();

        if (quote != null) {
            builder.symbol(quote.getSymbol())
                    .name(quote.getName())
                    .exchange(quote.getExchangeCode())
                    .exchangeMic(quote.getExchange())
                    .assetType(quote.getAssetType())
                    .currency(quote.getCurrencyCode())
                    .price(quote.getClose())
                    .open(quote.getOpen())
                    .high(quote.getHigh())
                    .low(quote.getLow())
                    .volume(quote.getVolume())
                    .lastPriceDate(quote.getDate())
                    .dataFreshness(quote.getDataFreshness())
                    .lastUpdated(quote.getFetchedAt())
                    .isStale(quote.isStale());
        }

        if (stock != null) {
            if (quote == null) builder.symbol(stock.getSymbol()).name(stock.getName());
            builder.sector(stock.getSector())
                    .industry(stock.getIndustry())
                    .country(stock.getCountry())
                    .website(stock.getCompanyWebsite())
                    .description(stock.getDescription())
                    .logoUrl(stock.getLogoUrl())
                    .marketCap(stock.getMarketCap())
                    .peRatio(stock.getPeRatio())
                    .eps(stock.getEps())
                    .beta(stock.getBeta())
                    .hasIntraday(Boolean.TRUE.equals(stock.getHasIntraday()))
                    .hasEod(Boolean.TRUE.equals(stock.getHasEod()));
        }

        builder.prevClose(prevClose).change(change).changePct(changePct);

        return builder.build();
    }
}
