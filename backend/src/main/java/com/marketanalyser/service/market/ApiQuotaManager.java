package com.marketanalyser.service.market;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;

/**
 * Centralized API quota manager.
 * Tracks usage in Redis for atomic increments across distributed instances.
 * Enforces configurable daily/monthly limits with safety margins.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ApiQuotaManager {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${marketstack.quota.monthly-limit:10000}")
    private int marketstackMonthlyLimit;

    @Value("${marketstack.quota.safe-usage-percentage:0.85}")
    private double safeUsagePct;

    @Value("${marketstack.quota.max-requests-per-minute:60}")
    private int maxPerMinute;

    @Value("${alphavantage.quota.daily-limit:25}")
    private int alphaVantageDaily;

    private static final String KEY_PREFIX = "quota:";
    private static final String MARKETSTACK = "marketstack";
    private static final String ALPHAVANTAGE = "alphavantage";

    /**
     * Check quota and increment atomically.
     * Throws QuotaExceededException if limit would be exceeded.
     */
    public void checkAndIncrementQuota(String provider) {
        String key = quotaKey(provider);
        Long current = redisTemplate.opsForValue().increment(key);

        // Set expiry on first increment
        if (current != null && current == 1L) {
            if (MARKETSTACK.equals(provider)) {
                // Monthly quota — expire end of month
                redisTemplate.expire(key, Duration.ofDays(31));
            } else {
                // Daily quota
                redisTemplate.expire(key, Duration.ofDays(1));
            }
        }

        int limit = getLimit(provider);
        double safeLimit = limit * safeUsagePct;

        if (current != null && current > safeLimit) {
            if (current > limit) {
                // Roll back increment
                redisTemplate.opsForValue().decrement(key);
                throw new QuotaExceededException(provider + " quota exhausted: " + current + "/" + limit);
            }
            log.warn("API quota warning for {}: {}/{} ({:.1f}%)", provider, current, limit,
                    (current * 100.0 / limit));
        }
    }

    public boolean hasQuotaRemaining(String provider) {
        String key = quotaKey(provider);
        Object val = redisTemplate.opsForValue().get(key);
        long current = val != null ? ((Number) val).longValue() : 0L;
        return current < (getLimit(provider) * safeUsagePct);
    }

    public QuotaStatus getStatus(String provider) {
        String key = quotaKey(provider);
        Object val = redisTemplate.opsForValue().get(key);
        long current = val != null ? ((Number) val).longValue() : 0L;
        int limit = getLimit(provider);

        return QuotaStatus.builder()
                .provider(provider)
                .requestsUsed(current)
                .quotaLimit(limit)
                .remaining(Math.max(0, limit - current))
                .percentUsed((current * 100.0) / limit)
                .safeThreshold((long)(limit * safeUsagePct))
                .isNearLimit(current >= limit * safeUsagePct)
                .isExhausted(current >= limit)
                .build();
    }

    private String quotaKey(String provider) {
        if (MARKETSTACK.equals(provider)) {
            // Monthly key
            LocalDate now = LocalDate.now();
            return KEY_PREFIX + provider + ":monthly:" + now.getYear() + "-" + now.getMonthValue();
        }
        return KEY_PREFIX + provider + ":daily:" + LocalDate.now();
    }

    private int getLimit(String provider) {
        return MARKETSTACK.equals(provider) ? marketstackMonthlyLimit : alphaVantageDaily;
    }

    @lombok.Data @lombok.Builder
    public static class QuotaStatus {
        private String provider;
        private long requestsUsed;
        private int quotaLimit;
        private long remaining;
        private double percentUsed;
        private long safeThreshold;
        private boolean isNearLimit;
        private boolean isExhausted;
    }

    public static class QuotaExceededException extends RuntimeException {
        public QuotaExceededException(String message) { super(message); }
    }
}
