package com.marketanalyser.service.provider.marketstack;

import com.marketanalyser.domain.model.HistoricalPrice;
import com.marketanalyser.domain.model.Stock;
import com.marketanalyser.shared.model.HistoricalCandle;
import com.marketanalyser.shared.model.MarketQuote;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Maps raw Marketstack v2 responses to normalized internal models.
 * This is the only place that knows about Marketstack's data format.
 */
@Component
public class MarketstackMapper {

    public MarketQuote toQuote(MarketstackDto.EodBar bar) {
        if (bar == null) return null;
        return MarketQuote.builder()
                .symbol(bar.getSymbol())
                .name(bar.getName())
                .exchange(bar.getExchange())
                .exchangeCode(bar.getExchange_code())
                .assetType(bar.getAsset_type())
                .currencyCode(bar.getPrice_currency())
                .open(bar.getOpen())
                .high(bar.getHigh())
                .low(bar.getLow())
                .close(bar.getClose())
                .adjClose(bar.getAdj_close())
                .volume(bar.getVolume())
                .date(bar.getDate() != null ? bar.getDate().toLocalDate() : null)
                .build();
    }

    public HistoricalCandle toCandle(MarketstackDto.EodBar bar) {
        if (bar == null) return null;
        return HistoricalCandle.builder()
                .date(bar.getDate() != null ? bar.getDate().toLocalDate() : null)
                .open(bar.getOpen())
                .high(bar.getHigh())
                .low(bar.getLow())
                .close(bar.getClose())
                .adjOpen(bar.getAdj_open())
                .adjHigh(bar.getAdj_high())
                .adjLow(bar.getAdj_low())
                .adjClose(bar.getAdj_close())
                .volume(bar.getVolume())
                .adjVolume(bar.getAdj_volume())
                .splitFactor(bar.getSplit_factor() != null ? bar.getSplit_factor() : BigDecimal.ONE)
                .dividend(bar.getDividend() != null ? bar.getDividend() : BigDecimal.ZERO)
                .symbol(bar.getSymbol())
                .exchange(bar.getExchange())
                .dataSource("marketstack")
                .build();
    }

    public HistoricalCandle toCandle(HistoricalPrice entity) {
        if (entity == null) return null;
        return HistoricalCandle.builder()
                .date(entity.getDate())
                .open(entity.getOpen())
                .high(entity.getHigh())
                .low(entity.getLow())
                .close(entity.getClose())
                .adjOpen(entity.getAdjOpen())
                .adjHigh(entity.getAdjHigh())
                .adjLow(entity.getAdjLow())
                .adjClose(entity.getAdjClose())
                .volume(entity.getVolume())
                .adjVolume(entity.getAdjVolume())
                .splitFactor(entity.getSplitFactor())
                .dividend(entity.getDividend())
                .symbol(entity.getStock().getSymbol())
                .dataSource(entity.getDataSource())
                .build();
    }

    public HistoricalPrice toEntity(Stock stock, MarketstackDto.EodBar bar) {
        return HistoricalPrice.builder()
                .stock(stock)
                .date(bar.getDate().toLocalDate())
                .open(bar.getOpen())
                .high(bar.getHigh())
                .low(bar.getLow())
                .close(bar.getClose())
                .adjOpen(bar.getAdj_open())
                .adjHigh(bar.getAdj_high())
                .adjLow(bar.getAdj_low())
                .adjClose(bar.getAdj_close())
                .volume(bar.getVolume())
                .adjVolume(bar.getAdj_volume())
                .splitFactor(bar.getSplit_factor() != null ? bar.getSplit_factor() : BigDecimal.ONE)
                .dividend(bar.getDividend() != null ? bar.getDividend() : BigDecimal.ZERO)
                .dataSource("marketstack")
                .build();
    }

    /**
     * Calculate price change between two consecutive bars.
     */
    public static BigDecimal calcChange(BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null || previous.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return current.subtract(previous);
    }

    public static BigDecimal calcChangePct(BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null || previous.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return current.subtract(previous)
                .divide(previous, 6, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
