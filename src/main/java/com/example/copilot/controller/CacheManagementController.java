package com.example.copilot.controller;

import com.example.copilot.service.CacheManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

/**
 * REST controller for cache management operations.
 * Provides endpoints for cache monitoring and manual cache operations.
 * Should be secured and only accessible to administrators in production.
 */
@RestController
@RequestMapping("/api/admin/cache")
@RequiredArgsConstructor
public class CacheManagementController {

    private final CacheManagementService cacheManagementService;

    /**
     * Gets all cache names.
     * 
     * @return collection of cache names
     */
    @GetMapping
    public ResponseEntity<Collection<String>> getCacheNames() {
        return ResponseEntity.ok(cacheManagementService.getCacheNames());
    }

    /**
     * Clears a specific cache.
     * 
     * @param cacheName the name of the cache to clear
     * @return success message
     */
    @DeleteMapping("/{cacheName}")
    public ResponseEntity<Map<String, String>> clearCache(@PathVariable String cacheName) {
        cacheManagementService.clearCache(cacheName);
        return ResponseEntity.ok(Map.of(
            "message", "Cache cleared successfully",
            "cacheName", cacheName
        ));
    }

    /**
     * Clears all caches.
     * 
     * @return success message
     */
    @DeleteMapping
    public ResponseEntity<Map<String, Object>> clearAllCaches() {
        Collection<String> cacheNames = cacheManagementService.getCacheNames();
        cacheManagementService.clearAllCaches();
        return ResponseEntity.ok(Map.of(
            "message", "All caches cleared successfully",
            "clearedCaches", cacheNames
        ));
    }

    /**
     * Evicts a specific entry from a cache.
     * 
     * @param cacheName the name of the cache
     * @param key the key to evict
     * @return success message
     */
    @DeleteMapping("/{cacheName}/{key}")
    public ResponseEntity<Map<String, String>> evictCacheEntry(
            @PathVariable String cacheName, 
            @PathVariable String key) {
        cacheManagementService.evictCacheEntry(cacheName, key);
        return ResponseEntity.ok(Map.of(
            "message", "Cache entry evicted successfully",
            "cacheName", cacheName,
            "key", key
        ));
    }

    /**
     * Checks if a specific key exists in a cache.
     * 
     * @param cacheName the name of the cache
     * @param key the key to check
     * @return whether the key is cached
     */
    @GetMapping("/{cacheName}/{key}")
    public ResponseEntity<Map<String, Object>> checkCacheEntry(
            @PathVariable String cacheName, 
            @PathVariable String key) {
        boolean isCached = cacheManagementService.isCached(cacheName, key);
        return ResponseEntity.ok(Map.of(
            "cacheName", cacheName,
            "key", key,
            "isCached", isCached
        ));
    }
}
