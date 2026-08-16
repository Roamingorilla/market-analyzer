package com.marketanalyser.service.provider.marketstack;

import com.marketanalyser.service.market.ApiQuotaManager;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class MarketstackClient {

    private final WebClient marketstackWebClient;
    private final ApiQuotaManager quotaManager;

    @Value("${marketstack.api-key}")
    private String apiKey;

    // --- EOD Data ---

    @CircuitBreaker(name = "marketstack", fallbackMethod = "eodFallback")
    @Retry(name = "marketstack")
    public List<MarketstackDto.EodBar> getEodLatest(String symbol) {
        quotaManager.checkAndIncrementQuota("marketstack");
        log.info("Fetching EOD latest from Marketstack: {}", symbol);

        var response = marketstackWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/eod/latest")
                        .queryParam("symbols", symbol)
                        .queryParam("access_key", apiKey)
                        .build())
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        resp -> resp.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RuntimeException("Marketstack error: " + body))))
                .bodyToMono(new ParameterizedTypeReference<MarketstackDto.PaginatedResponse<MarketstackDto.EodBar>>() {})
                .block();

        return response != null ? response.getData() : List.of();
    }

    @CircuitBreaker(name = "marketstack", fallbackMethod = "eodFallback")
    @Retry(name = "marketstack")
    public List<MarketstackDto.EodBar> getEodHistory(String symbol, LocalDate from, LocalDate to, int limit, int offset) {
        quotaManager.checkAndIncrementQuota("marketstack");
        log.info("Fetching EOD history from Marketstack: {} ({} to {})", symbol, from, to);

        var response = marketstackWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/eod")
                        .queryParam("symbols", symbol)
                        .queryParam("date_from", from.toString())
                        .queryParam("date_to", to.toString())
                        .queryParam("limit", limit)
                        .queryParam("offset", offset)
                        .queryParam("access_key", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<MarketstackDto.PaginatedResponse<MarketstackDto.EodBar>>() {})
                .block();

        return response != null ? response.getData() : List.of();
    }

    // Fetch all pages of history (quota-aware)
    public List<MarketstackDto.EodBar> getFullEodHistory(String symbol, LocalDate from, LocalDate to) {
        List<MarketstackDto.EodBar> all = new ArrayList<>();
        int limit = 100;
        int offset = 0;
        int total;

        do {
            var page = getEodHistory(symbol, from, to, limit, offset);
            if (page.isEmpty()) break;
            all.addAll(page);
            total = page.size() < limit ? all.size() : Integer.MAX_VALUE; // approximate
            offset += limit;
        } while (page.size() == limit && quotaManager.hasQuotaRemaining("marketstack"));

        return all;
    }

    // --- Tickers ---

    @CircuitBreaker(name = "marketstack")
    @Retry(name = "marketstack")
    public List<MarketstackDto.Ticker> getTickers(int limit, int offset) {
        quotaManager.checkAndIncrementQuota("marketstack");
        log.debug("Fetching tickers from Marketstack offset={}", offset);

        var response = marketstackWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/tickers")
                        .queryParam("limit", limit)
                        .queryParam("offset", offset)
                        .queryParam("access_key", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<MarketstackDto.PaginatedResponse<MarketstackDto.Ticker>>() {})
                .block();

        return response != null ? response.getData() : List.of();
    }

    @CircuitBreaker(name = "marketstack")
    public MarketstackDto.Ticker getTicker(String symbol) {
        quotaManager.checkAndIncrementQuota("marketstack");

        var response = marketstackWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/tickers/{symbol}")
                        .queryParam("access_key", apiKey)
                        .build(symbol))
                .retrieve()
                .bodyToMono(MarketstackDto.Ticker.class)
                .block();

        return response;
    }

    // --- Exchanges ---

    @CircuitBreaker(name = "marketstack")
    public List<MarketstackDto.MarketstackExchange> getExchanges(int limit, int offset) {
        quotaManager.checkAndIncrementQuota("marketstack");

        var response = marketstackWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/exchanges")
                        .queryParam("limit", limit)
                        .queryParam("offset", offset)
                        .queryParam("access_key", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<MarketstackDto.PaginatedResponse<MarketstackDto.MarketstackExchange>>() {})
                .block();

        return response != null ? response.getData() : List.of();
    }

    // --- Splits ---

    @CircuitBreaker(name = "marketstack")
    public List<MarketstackDto.SplitBar> getSplits(String symbol) {
        quotaManager.checkAndIncrementQuota("marketstack");

        var response = marketstackWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/splits")
                        .queryParam("symbols", symbol)
                        .queryParam("access_key", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<MarketstackDto.PaginatedResponse<MarketstackDto.SplitBar>>() {})
                .block();

        return response != null ? response.getData() : List.of();
    }

    // --- Dividends ---

    @CircuitBreaker(name = "marketstack")
    public List<MarketstackDto.DividendBar> getDividends(String symbol) {
        quotaManager.checkAndIncrementQuota("marketstack");

        var response = marketstackWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/dividends")
                        .queryParam("symbols", symbol)
                        .queryParam("access_key", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<MarketstackDto.PaginatedResponse<MarketstackDto.DividendBar>>() {})
                .block();

        return response != null ? response.getData() : List.of();
    }

    // Fallbacks
    private List<MarketstackDto.EodBar> eodFallback(String symbol, Exception ex) {
        log.warn("Marketstack circuit breaker open for {}: {}", symbol, ex.getMessage());
        return List.of();
    }
}
