package com.marketanalyser.service.provider.alphavantage;

import com.marketanalyser.service.market.ApiQuotaManager;
import com.marketanalyser.shared.model.CompanyProfile;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Alpha Vantage fundamentals client.
 * Free tier: 25 requests/day.
 * All results cached for 30 days in Redis + stored in PostgreSQL stocks table.
 * Only called when fundamentals_fetched_at is null or > 30 days old.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AlphaVantageClient {

    private final WebClient alphaVantageWebClient;
    private final ApiQuotaManager quotaManager;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${alphavantage.api-key:demo}")
    private String apiKey;

    @Value("${cache.ttl.fundamentals:2592000}")
    private long fundamentalsTtlSeconds;

    @CircuitBreaker(name = "alphavantage")
    public Optional<CompanyProfile> getCompanyOverview(String symbol) {
        String cacheKey = "fundamentals:" + symbol.toUpperCase();

        // Check Redis cache first
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof CompanyProfile cp) {
            log.debug("Fundamentals cache HIT for {}", symbol);
            return Optional.of(cp);
        }

        // Check quota
        if (!quotaManager.hasQuotaRemaining("alphavantage")) {
            log.warn("Alpha Vantage daily quota exhausted — returning empty for {}", symbol);
            return Optional.empty();
        }

        log.info("Fetching fundamentals from Alpha Vantage for {}", symbol);
        quotaManager.checkAndIncrementQuota("alphavantage");

        try {
            AlphaVantageOverviewDto dto = alphaVantageWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/query")
                            .queryParam("function", "OVERVIEW")
                            .queryParam("symbol", symbol)
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(AlphaVantageOverviewDto.class)
                    .block();

            if (dto == null || dto.getSymbol() == null || dto.getSymbol().isEmpty()) {
                log.warn("No fundamentals data returned from Alpha Vantage for {}", symbol);
                return Optional.empty();
            }

            CompanyProfile profile = mapToProfile(dto);

            // Cache in Redis for 30 days
            redisTemplate.opsForValue().set(cacheKey, profile, Duration.ofSeconds(fundamentalsTtlSeconds));

            return Optional.of(profile);

        } catch (Exception e) {
            log.error("Alpha Vantage request failed for {}: {}", symbol, e.getMessage());
            return Optional.empty();
        }
    }

    private CompanyProfile mapToProfile(AlphaVantageOverviewDto dto) {
        return CompanyProfile.builder()
                .symbol(dto.getSymbol())
                .name(dto.getName())
                .exchange(dto.getExchange())
                .currencyCode(dto.getCurrency())
                .country(dto.getCountry())
                .sector(dto.getSector())
                .industry(dto.getIndustry())
                .description(dto.getDescription())
                .website(dto.getOfficialSite())
                .marketCap(parseLong(dto.getMarketCapitalization()))
                .peRatio(parseBD(dto.getPeRatio()))
                .pegRatio(parseBD(dto.getPegRatio()))
                .eps(parseBD(dto.getEps()))
                .beta(parseBD(dto.getBeta()))
                .dividendYield(parseBD(dto.getDividendYield()))
                .dividendPerShare(parseBD(dto.getDividendPerShare()))
                .bookValue(parseBD(dto.getBookValue()))
                .profitMargin(parseBD(dto.getProfitMargin()))
                .sharesOutstanding(parseLong(dto.getSharesOutstanding()))
                .week52High(parseBD(dto.getWeek52High()))
                .week52Low(parseBD(dto.getWeek52Low()))
                .ma50Day(parseBD(dto.getMa50Day()))
                .ma200Day(parseBD(dto.getMa200Day()))
                .fetchedAt(OffsetDateTime.now())
                .dataSource("alphavantage")
                .build();
    }

    private BigDecimal parseBD(String val) {
        if (val == null || val.isBlank() || "None".equalsIgnoreCase(val) || "-".equals(val)) return null;
        try { return new BigDecimal(val); } catch (NumberFormatException e) { return null; }
    }

    private Long parseLong(String val) {
        if (val == null || val.isBlank() || "None".equalsIgnoreCase(val)) return null;
        try { return Long.parseLong(val); } catch (NumberFormatException e) { return null; }
    }
}
