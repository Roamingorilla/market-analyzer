package com.marketanalyser.api.v1.dto.response;

import com.marketanalyser.domain.model.Stock;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class StockSearchResult {
    private String symbol;
    private String name;
    private String exchange;
    private String exchangeMic;
    private String country;
    private String sector;
    private String currency;
    private boolean hasEod;
    private boolean hasIntraday;

    public static StockSearchResult from(Stock stock) {
        return StockSearchResult.builder()
                .symbol(stock.getSymbol())
                .name(stock.getName())
                .exchange(stock.getExchange() != null ? stock.getExchange().getAcronym() : null)
                .exchangeMic(stock.getExchange() != null ? stock.getExchange().getMic() : null)
                .country(stock.getCountry() != null ? stock.getCountry() : (stock.getExchange() != null ? stock.getExchange().getCountry() : null))
                .sector(stock.getSector())
                .currency(stock.getCurrencyCode())
                .hasEod(Boolean.TRUE.equals(stock.getHasEod()))
                .hasIntraday(Boolean.TRUE.equals(stock.getHasIntraday()))
                .build();
    }
}
