package com.marketanalyser.service.market;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Single-flight / request coalescing service.
 *
 * When 1,000 users request AAPL simultaneously:
 *   - User #1 acquires a Redis distributed lock → fetches Marketstack → stores in cache
 *   - Users #2-1,000 detect the lock → wait → read from cache when populated
 *   - Result: exactly ONE Marketstack call, regardless of concurrent users
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SingleFlightService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${cache.ttl.lock-ttl:30}")
    private long lockTtlSeconds;

    private static final int MAX_WAIT_CYCLES = 30;
    private static final long WAIT_INTERVAL_MS = 500;

    /**
     * Execute a supplier with single-flight semantics.
     * Only one caller executes the supplier; all others wait and reuse the result.
     *
     * @param cacheKey   the cache key to check/populate
     * @param lockKey    the distributed lock key
     * @param cacheTtl   TTL for the cached result
     * @param supplier   the expensive operation to execute once
     * @return the result (from cache or fresh)
     */
    @SuppressWarnings("unchecked")
    public <T> T execute(String cacheKey, String lockKey, Duration cacheTtl, Supplier<T> supplier) {
        // 1. Fast path: check cache first
        T cached = (T) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Cache HIT: {}", cacheKey);
            return cached;
        }

        // 2. Try to acquire distributed lock (SET NX EX)
        String lockValue = String.valueOf(Thread.currentThread().threadId());
        Boolean lockAcquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, Duration.ofSeconds(lockTtlSeconds));

        if (Boolean.TRUE.equals(lockAcquired)) {
            // 3a. I am the leader — execute the expensive operation
            log.debug("Lock acquired, fetching: {}", cacheKey);
            try {
                T result = supplier.get();
                if (result != null) {
                    redisTemplate.opsForValue().set(cacheKey, result, cacheTtl);
                }
                return result;
            } finally {
                // Release lock only if we still own it
                String currentLock = (String) redisTemplate.opsForValue().get(lockKey);
                if (lockValue.equals(currentLock)) {
                    redisTemplate.delete(lockKey);
                }
            }
        } else {
            // 3b. I am a follower — wait for leader to populate cache
            log.debug("Lock NOT acquired, waiting for cache population: {}", cacheKey);
            return waitForCache(cacheKey, lockKey);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T waitForCache(String cacheKey, String lockKey) {
        for (int i = 0; i < MAX_WAIT_CYCLES; i++) {
            try {
                Thread.sleep(WAIT_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            T result = (T) redisTemplate.opsForValue().get(cacheKey);
            if (result != null) {
                log.debug("Cache populated by leader, returning: {}", cacheKey);
                return result;
            }

            // If lock is gone but cache is empty, leader may have failed
            Boolean lockExists = redisTemplate.hasKey(lockKey);
            if (Boolean.FALSE.equals(lockExists)) {
                log.warn("Leader released lock but cache is empty for: {}", cacheKey);
                return null; // Caller will handle null gracefully
            }
        }
        log.warn("Timeout waiting for cache population: {}", cacheKey);
        return null;
    }
}
