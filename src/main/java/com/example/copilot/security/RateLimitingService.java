package com.example.copilot.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiting service using Bucket4j with Caffeine cache backend.
 * Implements token bucket algorithm for request rate limiting.
 */
@Service
@Slf4j
public class RateLimitingService {

    private final Cache<String, Bucket> bucketCache;
    
    // Rate limiting configurations
    private static final int LOGIN_REQUESTS_PER_MINUTE = 10;
    private static final int GENERAL_REQUESTS_PER_MINUTE = 100;
    private static final Duration REFILL_PERIOD = Duration.ofMinutes(1);
    
    public RateLimitingService() {
        // Configure Caffeine cache for storing buckets
        this.bucketCache = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .recordStats()
            .build();
    }

    /**
     * Checks if a login request is allowed for the given IP address.
     * Implements stricter rate limiting for login endpoints to prevent brute force attacks.
     * 
     * @param ipAddress the client IP address
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean isLoginAllowed(String ipAddress) {
        String key = "login:" + ipAddress;
        Bucket bucket = bucketCache.get(key, k -> createLoginBucket());
        
        boolean consumed = bucket.tryConsume(1);
        
        if (!consumed) {
            log.warn("Rate limit exceeded for login attempts from IP: {}", ipAddress);
        } else {
            log.debug("Login attempt allowed for IP: {}, remaining tokens: {}", 
                ipAddress, bucket.getAvailableTokens());
        }
        
        return consumed;
    }

    /**
     * Checks if a general API request is allowed for the given IP address.
     * 
     * @param ipAddress the client IP address
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean isGeneralRequestAllowed(String ipAddress) {
        String key = "general:" + ipAddress;
        Bucket bucket = bucketCache.get(key, k -> createGeneralBucket());
        
        boolean consumed = bucket.tryConsume(1);
        
        if (!consumed) {
            log.warn("Rate limit exceeded for general requests from IP: {}", ipAddress);
        }
        
        return consumed;
    }

    /**
     * Gets the number of available tokens for login requests from an IP.
     * 
     * @param ipAddress the client IP address
     * @return number of available tokens
     */
    public long getLoginAvailableTokens(String ipAddress) {
        String key = "login:" + ipAddress;
        Bucket bucket = bucketCache.get(key, k -> createLoginBucket());
        return bucket.getAvailableTokens();
    }

    /**
     * Gets the time until next refill for login requests from an IP.
     * 
     * @param ipAddress the client IP address
     * @return nanoseconds until next refill
     */
    public long getLoginTimeUntilRefill(String ipAddress) {
        String key = "login:" + ipAddress;
        Bucket bucket = bucketCache.get(key, k -> createLoginBucket());
        
        try {
            return bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill();
        } catch (Exception e) {
            log.warn("Error estimating refill time for IP: {}", ipAddress, e);
            return REFILL_PERIOD.toNanos();
        }
    }

    /**
     * Creates a bucket for login endpoints with strict limits.
     */
    private Bucket createLoginBucket() {
        return Bucket.builder()
            .addLimit(Bandwidth.builder()
                .capacity(LOGIN_REQUESTS_PER_MINUTE)
                .refillIntervally(LOGIN_REQUESTS_PER_MINUTE, REFILL_PERIOD)
                .build())
            .build();
    }

    /**
     * Creates a bucket for general API endpoints with more permissive limits.
     */
    private Bucket createGeneralBucket() {
        return Bucket.builder()
            .addLimit(Bandwidth.builder()
                .capacity(GENERAL_REQUESTS_PER_MINUTE)
                .refillIntervally(GENERAL_REQUESTS_PER_MINUTE, REFILL_PERIOD)
                .build())
            .build();
    }

    /**
     * Resets rate limiting for a specific IP address (for testing or admin purposes).
     * 
     * @param ipAddress the IP address to reset
     */
    public void resetRateLimit(String ipAddress) {
        String loginKey = "login:" + ipAddress;
        String generalKey = "general:" + ipAddress;
        
        bucketCache.invalidate(loginKey);
        bucketCache.invalidate(generalKey);
        
        log.info("Rate limit reset for IP: {}", ipAddress);
    }

    /**
     * Gets rate limiting statistics for monitoring.
     */
    public RateLimitStats getStats() {
        return new RateLimitStats(
            LOGIN_REQUESTS_PER_MINUTE,
            GENERAL_REQUESTS_PER_MINUTE,
            REFILL_PERIOD.toMinutes()
        );
    }

    /**
     * Rate limiting statistics holder.
     */
    public static class RateLimitStats {
        private final int loginRequestsPerMinute;
        private final int generalRequestsPerMinute;
        private final long refillPeriodMinutes;

        public RateLimitStats(int loginRequestsPerMinute, int generalRequestsPerMinute, long refillPeriodMinutes) {
            this.loginRequestsPerMinute = loginRequestsPerMinute;
            this.generalRequestsPerMinute = generalRequestsPerMinute;
            this.refillPeriodMinutes = refillPeriodMinutes;
        }

        // Getters
        public int getLoginRequestsPerMinute() { return loginRequestsPerMinute; }
        public int getGeneralRequestsPerMinute() { return generalRequestsPerMinute; }
        public long getRefillPeriodMinutes() { return refillPeriodMinutes; }
    }
}
