package com.marketanalyser.service.provider.marketstack;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Raw Marketstack v2 API response DTOs — never exposed to frontend.
 */
public class MarketstackDto {

    @Data @NoArgsConstructor
    public static class PaginatedResponse<T> {
        private Pagination pagination;
        private List<T> data;
    }

    @Data @NoArgsConstructor
    public static class Pagination {
        private int limit;
        private int offset;
        private int count;
        private int total;
    }

    @Data @NoArgsConstructor
    public static class EodBar {
        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal close;
        private Long volume;
        private BigDecimal adj_high;
        private BigDecimal adj_low;
        private BigDecimal adj_close;
        private BigDecimal adj_open;
        private Long adj_volume;
        private BigDecimal split_factor;
        private BigDecimal dividend;
        private String name;
        private String exchange_code;
        private String asset_type;
        private String price_currency;
        private String symbol;
        private String exchange;
        private OffsetDateTime date;
    }

    @Data @NoArgsConstructor
    public static class Ticker {
        private String name;
        private String symbol;
        private Boolean has_intraday;
        private Boolean has_eod;
        private String country;
        private StockExchange stock_exchange;
    }

    @Data @NoArgsConstructor
    public static class StockExchange {
        private String name;
        private String acronym;
        private String mic;
        private String country;
        private String country_code;
        private String city;
        private String website;
        private String timezone;
        private Currency currency;
    }

    @Data @NoArgsConstructor
    public static class MarketstackExchange {
        private String name;
        private String acronym;
        private String mic;
        private String country;
        private String country_code;
        private String city;
        private String website;
        private String timezone;
        private Currency currency;
    }

    @Data @NoArgsConstructor
    public static class Currency {
        private String code;
        private String symbol;
        private String name;
    }

    @Data @NoArgsConstructor
    public static class SplitBar {
        private String date;
        private BigDecimal split_factor;
        private String symbol;
    }

    @Data @NoArgsConstructor
    public static class DividendBar {
        private String date;
        private BigDecimal dividend;
        private String symbol;
    }

    @Data @NoArgsConstructor
    public static class ErrorResponse {
        private ErrorBody error;
    }

    @Data @NoArgsConstructor
    public static class ErrorBody {
        private String code;
        private String message;
        private String context;
    }
}
