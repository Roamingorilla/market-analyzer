package com.marketanalyser.service.market;

import com.marketanalyser.domain.model.HistoricalPrice;
import com.marketanalyser.domain.model.Stock;
import com.marketanalyser.domain.repository.ExchangeRepository;
import com.marketanalyser.domain.repository.HistoricalPriceRepository;
import com.marketanalyser.domain.repository.StockRepository;
import com.marketanalyser.service.provider.marketstack.MarketstackClient;
import com.marketanalyser.service.provider.marketstack.MarketstackDto;
import com.marketanalyser.service.provider.marketstack.MarketstackMapper;
import com.marketanalyser.shared.model.HistoricalCandle;
import com.marketanalyser.shared.model.MarketQuote;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.marketanalyser.config.RabbitMQConfig.QUEUE_HISTORICAL_INGEST;

/**
 * Central market data service.
 * All market data flows through here — never fetched directly by controllers.
 *
 * Flow: Controller → MarketDataService → Cache → DB → Marketstack
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MarketDataService {

    private final MarketstackClient marketstackClient;
    private final MarketstackMapper marketstackMapper;
    private final SingleFlightService singleFlight;
    private final HistoricalPriceRepository historicalPriceRepository;
    private final StockRepository stockRepository;
    private final ExchangeRepository exchangeRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${cache.ttl.eod-latest:3600}")
    private long eodLatestTtl;

    @Value("${cache.ttl.eod-historical:86400}")
    private long eodHistoricalTtl;

    /**
     * Get the most recent EOD quote for a symbol.
     * Uses single-flight: 1000 concurrent requests → 1 Marketstack call.
     */
    public MarketQuote getLatestQuote(String symbol) {
        String cacheKey = "market:eod:latest:" + symbol.toUpperCase();
        String lockKey = "lock:" + cacheKey;

        return singleFlight.execute(
                cacheKey,
                lockKey,
                Duration.ofSeconds(eodLatestTtl),
                () -> fetchAndStoreLatestQuote(symbol)
        );
    }

    /**
     * Get historical OHLCV data for a symbol within a date range.
     * Served from DB when available; fetches from Marketstack only for gaps.
     */
    @Transactional(readOnly = true)
    public List<HistoricalCandle> getHistoricalData(String symbol, LocalDate from, LocalDate to) {
        Optional<Stock> stockOpt = stockRepository.findBySymbolIgnoreCase(symbol);
        if (stockOpt.isEmpty()) {
            log.warn("Stock not found in DB: {}", symbol);
            return List.of();
        }

        Stock stock = stockOpt.get();
        List<HistoricalPrice> dbData = historicalPriceRepository
                .findByStockIdAndDateBetweenOrderByDateAsc(stock.getId(), from, to);

        // Check for gaps — if we have all data, return from DB
        if (isDataComplete(dbData, from, to)) {
            return dbData.stream().map(marketstackMapper::toCandle).collect(Collectors.toList());
        }

        // Trigger background fetch for missing data
        log.info("Historical data gap detected for {} [{} to {}], enqueueing fetch", symbol, from, to);
        enqueueHistoricalIngest(symbol, from, to);

        // Return what we have (stale-while-revalidate)
        return dbData.stream().map(marketstackMapper::toCandle).collect(Collectors.toList());
    }

    /**
     * Fetch directly from Marketstack and persist (called by workers, not users).
     */
    @Transactional
    public List<HistoricalCandle> fetchAndStoreHistory(String symbol, LocalDate from, LocalDate to) {
        log.info("Fetching historical data from Marketstack: {} [{} to {}]", symbol, from, to);

        List<MarketstackDto.EodBar> bars = marketstackClient.getFullEodHistory(symbol, from, to);
        if (bars.isEmpty()) {
            log.warn("No data returned from Marketstack for {} [{} to {}]", symbol, from, to);
            return List.of();
        }

        Stock stock = findOrCreateStock(symbol, bars.get(0));
        List<HistoricalCandle> candles = bars.stream()
                .map(marketstackMapper::toCandle)
                .collect(Collectors.toList());

        // Upsert all bars
        upsertHistoricalPrices(stock, bars);

        return candles;
    }

    private MarketQuote fetchAndStoreLatestQuote(String symbol) {
        log.info("Fetching latest EOD from Marketstack for: {}", symbol);
        List<MarketstackDto.EodBar> bars = marketstackClient.getEodLatest(symbol);

        if (bars.isEmpty()) {
            // Try to return last known DB price
            return getLastKnownQuoteFromDb(symbol);
        }

        MarketstackDto.EodBar bar = bars.get(0);
        Stock stock = findOrCreateStock(symbol, bar);

        // Persist to DB
        upsertHistoricalPrices(stock, bars);

        MarketQuote quote = marketstackMapper.toQuote(bar);
        quote.setDataFreshness("CACHED");
        quote.setFetchedAt(OffsetDateTime.now());
        return quote;
    }

    private MarketQuote getLastKnownQuoteFromDb(String symbol) {
        return stockRepository.findBySymbolIgnoreCase(symbol)
                .flatMap(stock -> historicalPriceRepository.findMostRecentByStockId(stock.getId()))
                .map(price -> {
                    MarketQuote q = MarketQuote.builder()
                            .symbol(symbol.toUpperCase())
                            .close(price.getClose())
                            .open(price.getOpen())
                            .high(price.getHigh())
                            .low(price.getLow())
                            .volume(price.getVolume())
                            .date(price.getDate())
                            .dataFreshness("HISTORICAL")
                            .isStale(true)
                            .fetchedAt(OffsetDateTime.now())
                            .build();
                    return q;
                })
                .orElse(null);
    }

    @Transactional
    public Stock findOrCreateStock(String symbol, MarketstackDto.EodBar bar) {
        return stockRepository.findBySymbolIgnoreCase(symbol)
                .orElseGet(() -> {
                    Stock s = Stock.builder()
                            .symbol(symbol.toUpperCase())
                            .name(bar.getName())
                            .assetType(bar.getAsset_type())
                            .currencyCode(bar.getPrice_currency())
                            .build();
                    return stockRepository.save(s);
                });
    }

    @Transactional
    public void upsertHistoricalPrices(Stock stock, List<MarketstackDto.EodBar> bars) {
        for (MarketstackDto.EodBar bar : bars) {
            if (bar.getDate() == null) continue;
            LocalDate date = bar.getDate().toLocalDate();

            historicalPriceRepository.findByStockIdAndDate(stock.getId(), date)
                    .ifPresentOrElse(
                            existing -> updatePrice(existing, bar),
                            () -> historicalPriceRepository.save(marketstackMapper.toEntity(stock, bar))
                    );
        }
    }

    private void updatePrice(HistoricalPrice existing, MarketstackDto.EodBar bar) {
        existing.setOpen(bar.getOpen());
        existing.setHigh(bar.getHigh());
        existing.setLow(bar.getLow());
        existing.setClose(bar.getClose());
        existing.setVolume(bar.getVolume());
        existing.setAdjClose(bar.getAdj_close());
        existing.setSplitFactor(bar.getSplit_factor());
        existing.setDividend(bar.getDividend());
        historicalPriceRepository.save(existing);
    }

    private boolean isDataComplete(List<HistoricalPrice> data, LocalDate from, LocalDate to) {
        if (data.isEmpty()) return false;
        // Approximate: if we have data and the most recent bar is recent enough
        LocalDate lastDate = data.get(data.size() - 1).getDate();
        long tradingDaysExpected = (to.toEpochDay() - from.toEpochDay()) * 5 / 7;
        return data.size() >= tradingDaysExpected * 0.9 && lastDate.isAfter(to.minusDays(5));
    }

    private void enqueueHistoricalIngest(String symbol, LocalDate from, LocalDate to) {
        try {
            Map<String, String> payload = Map.of(
                    "symbol", symbol,
                    "from", from.toString(),
                    "to", to.toString()
            );
            rabbitTemplate.convertAndSend(QUEUE_HISTORICAL_INGEST, payload);
        } catch (Exception e) {
            log.error("Failed to enqueue historical ingest job for {}: {}", symbol, e.getMessage());
        }
    }
}
