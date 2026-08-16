package com.marketanalyser.worker;

import com.marketanalyser.domain.model.Exchange;
import com.marketanalyser.domain.repository.ExchangeRepository;
import com.marketanalyser.domain.repository.HistoricalPriceRepository;
import com.marketanalyser.domain.repository.StockRepository;
import com.marketanalyser.service.market.ApiQuotaManager;
import com.marketanalyser.service.market.MarketDataService;
import com.marketanalyser.service.provider.marketstack.MarketstackClient;
import com.marketanalyser.service.provider.marketstack.MarketstackDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.marketanalyser.config.RabbitMQConfig.QUEUE_HISTORICAL_INGEST;
import static com.marketanalyser.config.RabbitMQConfig.QUEUE_MARKET_DATA_REFRESH;

@Component
@Slf4j
@RequiredArgsConstructor
public class MarketDataWorker {

    private final MarketstackClient marketstackClient;
    private final MarketDataService marketDataService;
    private final StockRepository stockRepository;
    private final ExchangeRepository exchangeRepository;
    private final HistoricalPriceRepository historicalPriceRepository;
    private final ApiQuotaManager quotaManager;

    @Value("${marketstack.prefetch.symbols:AAPL,MSFT,NVDA,AMZN,GOOGL,META,TSLA,JPM,V,NFLX}")
    private String prefetchSymbols;

    @Value("${marketstack.prefetch.enabled:true}")
    private boolean prefetchEnabled;

    /**
     * On startup: seed exchanges and popular ticker history.
     * Only runs if quota is available and data is not already fresh.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void seedDataOnStartup() {
        log.info("Starting initial data seed from Marketstack...");

        if (!quotaManager.hasQuotaRemaining("marketstack")) {
            log.warn("Quota exhausted — skipping startup seed");
            return;
        }

        try {
            seedExchanges();
            Thread.sleep(1000);
            seedPopularTickers();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Error during startup seed: {}", e.getMessage(), e);
        }
    }

    private void seedExchanges() {
        long existing = exchangeRepository.count();
        if (existing > 10) {
            log.info("Exchanges already seeded ({} in DB), skipping", existing);
            return;
        }

        log.info("Seeding exchanges from Marketstack...");
        int offset = 0;
        int limit = 100;
        List<MarketstackDto.MarketstackExchange> exchanges;

        do {
            if (!quotaManager.hasQuotaRemaining("marketstack")) {
                log.warn("Quota limit approaching — stopping exchange seed at offset {}", offset);
                break;
            }
            exchanges = marketstackClient.getExchanges(limit, offset);
            for (MarketstackDto.MarketstackExchange ex : exchanges) {
                exchangeRepository.findByMic(ex.getMic()).ifPresentOrElse(
                        existing2 -> {
                            existing2.setName(ex.getName());
                            existing2.setAcronym(ex.getAcronym());
                            exchangeRepository.save(existing2);
                        },
                        () -> {
                            Exchange exchange = Exchange.builder()
                                    .mic(ex.getMic())
                                    .name(ex.getName())
                                    .acronym(ex.getAcronym())
                                    .country(ex.getCountry())
                                    .countryCode(ex.getCountry_code())
                                    .city(ex.getCity())
                                    .website(ex.getWebsite())
                                    .timezone(ex.getTimezone())
                                    .currencyCode(ex.getCurrency() != null ? ex.getCurrency().getCode() : null)
                                    .build();
                            exchangeRepository.save(exchange);
                        }
                );
            }
            offset += limit;
        } while (exchanges.size() == limit);

        log.info("Exchange seeding complete. Total in DB: {}", exchangeRepository.count());
    }

    private void seedPopularTickers() {
        String[] symbols = prefetchSymbols.split(",");
        log.info("Seeding {} popular symbols...", symbols.length);

        for (String symbol : symbols) {
            if (!quotaManager.hasQuotaRemaining("marketstack")) {
                log.warn("Quota limit approaching — stopping ticker seed");
                break;
            }
            try {
                seedTickerHistory(symbol.trim());
                Thread.sleep(500); // gentle rate limiting
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Failed to seed {}: {}", symbol, e.getMessage());
            }
        }
    }

    private void seedTickerHistory(String symbol) {
        // Skip if we already have recent data
        Optional<var> existingStock = stockRepository.findBySymbolIgnoreCase(symbol);
        if (existingStock.isPresent()) {
            var maxDate = historicalPriceRepository.findMaxDateByStockId(existingStock.get().getId());
            if (maxDate.isPresent() && maxDate.get().isAfter(LocalDate.now().minusDays(3))) {
                log.debug("Data already fresh for {}, skipping seed", symbol);
                return;
            }
        }

        log.info("Seeding 1-year history for {}", symbol);
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusYears(1);
        marketDataService.fetchAndStoreHistory(symbol, from, to);
    }

    /** Nightly refresh at 00:30 UTC */
    @Scheduled(cron = "0 30 0 * * *")
    public void nightlyRefresh() {
        if (!prefetchEnabled) return;
        if (!quotaManager.hasQuotaRemaining("marketstack")) {
            log.warn("Quota exhausted — skipping nightly refresh");
            return;
        }

        log.info("Starting nightly market data refresh...");
        Arrays.stream(prefetchSymbols.split(","))
                .map(String::trim)
                .forEach(symbol -> {
                    try {
                        if (quotaManager.hasQuotaRemaining("marketstack")) {
                            marketDataService.fetchAndStoreHistory(symbol,
                                    LocalDate.now().minusDays(7), LocalDate.now());
                            Thread.sleep(300);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        log.error("Nightly refresh failed for {}: {}", symbol, e.getMessage());
                    }
                });
        log.info("Nightly refresh complete");
    }

    @RabbitListener(queues = QUEUE_HISTORICAL_INGEST)
    public void processHistoricalIngest(Map<String, String> payload) {
        String symbol = payload.get("symbol");
        LocalDate from = LocalDate.parse(payload.get("from"));
        LocalDate to = LocalDate.parse(payload.get("to"));

        log.info("Processing historical ingest: {} [{} to {}]", symbol, from, to);

        if (!quotaManager.hasQuotaRemaining("marketstack")) {
            log.warn("Quota exhausted — dropping historical ingest for {}", symbol);
            return;
        }

        marketDataService.fetchAndStoreHistory(symbol, from, to);
    }

    @RabbitListener(queues = QUEUE_MARKET_DATA_REFRESH)
    public void processMarketDataRefresh(Map<String, String> payload) {
        String symbol = payload.get("symbol");
        log.info("Processing market data refresh: {}", symbol);
        marketDataService.getLatestQuote(symbol);
    }
}
