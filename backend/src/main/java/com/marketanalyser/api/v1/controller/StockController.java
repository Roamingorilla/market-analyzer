package com.marketanalyser.api.v1.controller;

import com.marketanalyser.api.v1.dto.response.*;
import com.marketanalyser.domain.model.HistoricalPrice;
import com.marketanalyser.domain.model.Stock;
import com.marketanalyser.domain.repository.HistoricalPriceRepository;
import com.marketanalyser.domain.repository.StockRepository;
import com.marketanalyser.service.market.ApiQuotaManager;
import com.marketanalyser.service.market.MarketDataService;
import com.marketanalyser.service.provider.marketstack.MarketstackMapper;
import com.marketanalyser.service.technical.TechnicalIndicatorService;
import com.marketanalyser.shared.model.HistoricalCandle;
import com.marketanalyser.shared.model.MarketQuote;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Stocks", description = "Stock data endpoints")
public class StockController {

    private final StockRepository stockRepository;
    private final MarketDataService marketDataService;
    private final TechnicalIndicatorService technicalIndicatorService;
    private final HistoricalPriceRepository historicalPriceRepository;
    private final MarketstackMapper marketstackMapper;
    private final ApiQuotaManager quotaManager;

    @GetMapping("/search")
    @Operation(summary = "Search stocks by symbol or name")
    public ResponseEntity<ApiResponse<Page<StockSearchResult>>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (q == null || q.trim().length() < 1) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Query must be at least 1 character"));
        }

        Page<Stock> results = stockRepository.searchBySymbolOrName(q.trim(), PageRequest.of(page, Math.min(size, 50)));
        Page<StockSearchResult> mapped = results.map(StockSearchResult::from);
        return ResponseEntity.ok(ApiResponse.success(mapped));
    }

    @GetMapping("/{symbol}")
    @Operation(summary = "Get stock quote and profile")
    public ResponseEntity<ApiResponse<StockDetailResponse>> getStock(@PathVariable String symbol) {
        String sym = symbol.toUpperCase();

        // Get latest quote (uses single-flight cache)
        MarketQuote quote = marketDataService.getLatestQuote(sym);

        // Get stock profile from DB
        Optional<Stock> stockOpt = stockRepository.findBySymbolIgnoreCase(sym);

        if (quote == null && stockOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Calculate change vs previous close from DB
        BigDecimal prevClose = null;
        BigDecimal change = null;
        BigDecimal changePct = null;

        if (stockOpt.isPresent() && quote != null) {
            Optional<HistoricalPrice> prevDay = historicalPriceRepository
                    .findByStockIdAndDate(stockOpt.get().getId(),
                            quote.getDate() != null ? quote.getDate().minusDays(1) : LocalDate.now().minusDays(1));
            // Try a few days back for weekends/holidays
            if (prevDay.isEmpty() && quote.getDate() != null) {
                List<HistoricalPrice> recent = historicalPriceRepository
                        .findByStockIdAndDateBetweenOrderByDateAsc(stockOpt.get().getId(),
                                quote.getDate().minusDays(7), quote.getDate().minusDays(1));
                if (!recent.isEmpty()) {
                    prevClose = recent.get(recent.size() - 1).getClose();
                }
            } else {
                prevClose = prevDay.map(HistoricalPrice::getClose).orElse(null);
            }

            if (prevClose != null && quote.getClose() != null) {
                change = MarketstackMapper.calcChange(quote.getClose(), prevClose);
                changePct = MarketstackMapper.calcChangePct(quote.getClose(), prevClose);
            }
        }

        StockDetailResponse response = StockDetailResponse.from(quote, stockOpt.orElse(null), prevClose, change, changePct);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{symbol}/historical")
    @Operation(summary = "Get historical OHLCV data")
    public ResponseEntity<ApiResponse<List<HistoricalCandle>>> getHistorical(
            @PathVariable String symbol,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "daily") String interval
    ) {
        LocalDate endDate = to != null ? to : LocalDate.now();
        LocalDate startDate = from != null ? from : endDate.minusYears(1);

        List<HistoricalCandle> data = marketDataService.getHistoricalData(symbol.toUpperCase(), startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/{symbol}/technicals")
    @Operation(summary = "Get technical indicators (computed from cached prices)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTechnicals(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "sma20,sma50,sma200,rsi14,macd,bb20") String indicators
    ) {
        String sym = symbol.toUpperCase();

        // Get enough historical data (200 days for SMA200)
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(400);
        List<HistoricalCandle> candles = marketDataService.getHistoricalData(sym, from, to);

        if (candles.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(Map.of()));
        }

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        String[] requestedIndicators = indicators.split(",");

        for (String ind : requestedIndicators) {
            String key = ind.trim().toLowerCase();
            try {
                if (key.startsWith("sma")) {
                    int period = Integer.parseInt(key.substring(3));
                    List<BigDecimal> sma = technicalIndicatorService.sma(candles, period);
                    result.put(key, sma);
                } else if (key.startsWith("ema")) {
                    int period = Integer.parseInt(key.substring(3));
                    result.put(key, technicalIndicatorService.ema(candles, period));
                } else if (key.startsWith("rsi")) {
                    int period = key.length() > 3 ? Integer.parseInt(key.substring(3)) : 14;
                    result.put("rsi" + period, technicalIndicatorService.rsi(candles, period));
                } else if (key.equals("macd")) {
                    var macd = technicalIndicatorService.macd(candles, 12, 26, 9);
                    result.put("macd", Map.of("line", macd.macd(), "signal", macd.signal(), "histogram", macd.histogram()));
                } else if (key.startsWith("bb")) {
                    int period = key.length() > 2 ? Integer.parseInt(key.substring(2)) : 20;
                    var bb = technicalIndicatorService.bollingerBands(candles, period, 2.0);
                    result.put("bb" + period, Map.of("upper", bb.upper(), "middle", bb.middle(), "lower", bb.lower()));
                } else if (key.startsWith("atr")) {
                    int period = key.length() > 3 ? Integer.parseInt(key.substring(3)) : 14;
                    result.put("atr" + period, technicalIndicatorService.atr(candles, period));
                }
            } catch (Exception e) {
                log.warn("Failed to calculate indicator {}: {}", key, e.getMessage());
            }
        }

        // Include candle dates for chart alignment
        result.put("dates", candles.stream().map(c -> c.getDate().toString()).toList());
        result.put("dataFreshness", "CACHED");
        result.put("computedAt", OffsetDateTime.now().toString());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{symbol}/data-freshness")
    @Operation(summary = "Get data freshness metadata for a symbol")
    public ResponseEntity<ApiResponse<DataFreshnessResponse>> getDataFreshness(@PathVariable String symbol) {
        String sym = symbol.toUpperCase();
        Optional<Stock> stockOpt = stockRepository.findBySymbolIgnoreCase(sym);

        if (stockOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Stock stock = stockOpt.get();
        Optional<LocalDate> lastDate = historicalPriceRepository.findMaxDateByStockId(stock.getId());
        long totalBars = historicalPriceRepository.countByStockId(stock.getId());

        DataFreshnessResponse resp = DataFreshnessResponse.builder()
                .symbol(sym)
                .lastUpdated(lastDate.map(LocalDate::atStartOfDay).map(d -> d.atOffset(java.time.ZoneOffset.UTC)).orElse(null))
                .totalBars(totalBars)
                .hasIntraday(Boolean.TRUE.equals(stock.getHasIntraday()))
                .hasEod(Boolean.TRUE.equals(stock.getHasEod()))
                .quotaStatus(quotaManager.getStatus("marketstack"))
                .build();

        return ResponseEntity.ok(ApiResponse.success(resp));
    }
}
