package com.example.copilot.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache configuration for the application.
 * Enables caching with a simple concurrent map cache manager for development/testing.
 * For production environments, consider using Redis, Hazelcast, or Caffeine for better performance.
 */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    /**
     * Configures a simple in-memory cache manager using ConcurrentHashMap.
     * For production environments, consider using Redis, Hazelcast, or Caffeine.
     */
    @Bean
    @Override
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        
        // Pre-define cache names for better performance and explicit configuration
        cacheManager.setCacheNames(java.util.Arrays.asList(
            "product-details",      // Individual product lookups by ID
            "product-search",       // Product search results with filters
            "category-details"      // Category lookups by ID
        ));
        
        // Allow null values to be cached (useful for "not found" scenarios)
        cacheManager.setAllowNullValues(false); // Changed to false for better performance
        
        return cacheManager;
    }
    
    /**
     * Custom key generator for cache keys.
     * Uses the default SimpleKeyGenerator which creates keys based on method parameters.
     */
    @Bean
    @Override
    public KeyGenerator keyGenerator() {
        return new SimpleKeyGenerator();
    }
    
    /**
     * Error handler for cache operations.
     * Logs cache errors but doesn't break the application flow.
     */
    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler();
    }
    
    /**
     * Cache resolver - uses default behavior.
     */
    @Override
    public CacheResolver cacheResolver() {
        return null; // Use default
    }
}
