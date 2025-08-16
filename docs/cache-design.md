**Version:** 1.0
**Date:** 2025-03-05
**Author:** Mark Andrew Ray-Smith Cityline Ltd

## **Proposed Caching Architecture**

### **1. Cache Configuration Layer**
Extend existing YAML configurations to include caching directives:

````yaml path=generic-config/example-queries-with-cache.yml mode=EDIT
queries:
  get_stock_trades:
    database: "stocktrades_db"
    sql: "SELECT * FROM stock_trades WHERE symbol = ? ORDER BY trade_date DESC LIMIT ?"
    cache:
      enabled: true
      strategy: "LRU"
      ttl: 300  # 5 minutes
      maxSize: 1000
      keyPattern: "stock_trades:{symbol}:{limit}"
      invalidateOn: ["stock_trades_insert", "stock_trades_update"]
      
  get_aggregated_volume:
    database: "stocktrades_db" 
    sql: "SELECT symbol, SUM(volume) as total_volume FROM stock_trades GROUP BY symbol"
    cache:
      enabled: true
      strategy: "TIME_BASED"
      ttl: 600  # 10 minutes for aggregations
      refreshAsync: true
      preload: true  # Cache warming
````

### **2. Multi-Level Cache Implementation**

**Level 1: Query Result Cache** - Caches raw query results
**Level 2: Aggregation Cache** - Caches computed aggregations  
**Level 3: API Response Cache** - Caches formatted API responses

````java path=cordal-common-library/src/main/java/dev/cordal/common/cache/CacheManager.java mode=EDIT
@Singleton
public class CacheManager {
    private final Map<String, Cache<String, Object>> caches = new ConcurrentHashMap<>();
    private final CacheConfig cacheConfig;
    
    public <T> Optional<T> get(String cacheName, String key, Class<T> type) {
        Cache<String, Object> cache = getOrCreateCache(cacheName);
        return Optional.ofNullable((T) cache.get(key));
    }
    
    public void put(String cacheName, String key, Object value, Duration ttl) {
        // Implementation with configurable eviction policies
    }
    
    public void invalidate(String cacheName, String... patterns) {
        // Pattern-based cache invalidation
    }
}
````

### **3. Integration Points**

**A. Query Execution Layer**
````java path=cordal-api-service/src/main/java/dev/cordal/generic/GenericRepository.java mode=EDIT
public List<Map<String, Object>> executeQuery(String queryName, Map<String, Object> parameters) {
    QueryConfig queryConfig = getQueryConfig(queryName);
    
    if (queryConfig.isCacheEnabled()) {
        String cacheKey = buildCacheKey(queryName, parameters);
        Optional<List<Map<String, Object>>> cached = cacheManager.get("query_results", cacheKey, List.class);
        
        if (cached.isPresent()) {
            metricsCollector.recordCacheHit(queryName);
            return cached.get();
        }
    }
    
    List<Map<String, Object>> results = executeQueryDirect(queryConfig, parameters);
    
    if (queryConfig.isCacheEnabled()) {
        cacheManager.put("query_results", cacheKey, results, queryConfig.getCacheTtl());
    }
    
    return results;
}
````

**B. Configuration Management**
````java path=cordal-api-service/src/main/java/dev/cordal/generic/config/QueryConfig.java mode=EDIT
public class QueryConfig {
    // Existing fields...
    
    private CacheConfiguration cache;
    
    public static class CacheConfiguration {
        private boolean enabled = false;
        private String strategy = "LRU";
        private int ttl = 300;
        private int maxSize = 1000;
        private String keyPattern;
        private List<String> invalidateOn = new ArrayList<>();
        private boolean refreshAsync = false;
        private boolean preload = false;
        
        // Getters/setters...
    }
}
````

### **4. Cache Strategies**

**LRU Cache**: For frequently accessed queries
**Time-Based**: For data with known freshness requirements  
**Write-Through**: For critical data consistency
**Refresh-Ahead**: For expensive aggregations

### **5. Invalidation Mechanisms**

**Time-based**: Automatic TTL expiration
**Event-based**: Invalidate on data modifications
**Pattern-based**: Invalidate related cache entries
**Manual**: Administrative cache clearing

### **6. Monitoring & Metrics**

````java path=cordal-api-service/src/main/java/dev/cordal/generic/management/CacheMonitoringService.java mode=EDIT
@Singleton
public class CacheMonitoringService {
    public CacheStatistics getCacheStatistics(String cacheName) {
        return CacheStatistics.builder()
            .hitRate(calculateHitRate(cacheName))
            .missRate(calculateMissRate(cacheName))
            .evictionCount(getEvictionCount(cacheName))
            .size(getCacheSize(cacheName))
            .build();
    }
}
````

### **7. Configuration Example**

````yaml path=cordal-api-service/src/main/resources/application.yml mode=EDIT
cache:
  enabled: true
  defaultTtl: 300
  maxMemoryMb: 512
  strategies:
    LRU:
      maxSize: 10000
    TIME_BASED:
      cleanupInterval: 60
  monitoring:
    enabled: true
    metricsInterval: 30
````

## **Key Benefits**

1. **Zero-Code Integration** - Caching configured via YAML
2. **Performance Boost** - Especially for expensive aggregations
3. **Flexible Strategies** - Different caching approaches per query
4. **Monitoring Built-in** - Cache metrics integrated with existing system
5. **Invalidation Control** - Precise cache management
6. **Memory Efficient** - Configurable limits and eviction policies

This approach maintains CORDAL's configuration-driven philosophy while adding powerful caching capabilities that can dramatically improve performance for data-intensive applications.
