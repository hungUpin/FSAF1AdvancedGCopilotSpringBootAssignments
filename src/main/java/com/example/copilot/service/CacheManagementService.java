package com.example.copilot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * Service for managing cache operations and providing cache utilities.
 * Useful for cache monitoring, debugging, and manual cache management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheManagementService {

    private final CacheManager cacheManager;

    /**
     * Clears all entries from a specific cache.
     * 
     * @param cacheName the name of the cache to clear
     */
    public void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.info("Cleared cache: {}", cacheName);
        } else {
            log.warn("Cache not found: {}", cacheName);
        }
    }

    /**
     * Clears all caches.
     */
    public void clearAllCaches() {
        Collection<String> cacheNames = cacheManager.getCacheNames();
        cacheNames.forEach(this::clearCache);
        log.info("Cleared all caches: {}", cacheNames);
    }

    /**
     * Evicts a specific entry from a cache.
     * 
     * @param cacheName the name of the cache
     * @param key the key to evict
     */
    public void evictCacheEntry(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
            log.info("Evicted key '{}' from cache '{}'", key, cacheName);
        } else {
            log.warn("Cache not found: {}", cacheName);
        }
    }

    /**
     * Gets the value for a specific key from a cache without triggering cache loading.
     * 
     * @param cacheName the name of the cache
     * @param key the key to retrieve
     * @return the cached value or null if not found
     */
    public Object getCacheValue(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(key);
            return wrapper != null ? wrapper.get() : null;
        }
        return null;
    }

    /**
     * Checks if a specific key exists in a cache.
     * 
     * @param cacheName the name of the cache
     * @param key the key to check
     * @return true if the key exists in the cache
     */
    public boolean isCached(String cacheName, Object key) {
        return getCacheValue(cacheName, key) != null;
    }

    /**
     * Gets the names of all configured caches.
     * 
     * @return collection of cache names
     */
    public Collection<String> getCacheNames() {
        return cacheManager.getCacheNames();
    }

    /**
     * Preloads product details cache for commonly accessed products.
     * This can be called during application startup or scheduled periodically.
     * 
     * @param productIds collection of product IDs to preload
     */
    public void preloadProductCache(Collection<Long> productIds) {
        // This would require injecting ProductService, which would create circular dependency
        // Instead, this method serves as a placeholder for manual cache warming strategies
        log.info("Cache preloading requested for {} products", productIds.size());
    }
}
