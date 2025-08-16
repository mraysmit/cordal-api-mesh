package dev.cordal.generic.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Configuration model for SQL queries
 */
public class QueryConfig {
    private String name;
    private String description;
    private String sql;
    private String database; // Reference to database configuration
    private List<QueryParameter> parameters;
    private CacheConfiguration cache;

    // Default constructor
    public QueryConfig() {}

    // Constructor with all fields
    public QueryConfig(String name, String description, String sql, String database, List<QueryParameter> parameters) {
        this.name = name;
        this.description = description;
        this.sql = sql;
        this.database = database;
        this.parameters = parameters;
        this.cache = null; // Cache configuration is optional
    }

    // Constructor with cache configuration
    public QueryConfig(String name, String description, String sql, String database, List<QueryParameter> parameters, CacheConfiguration cache) {
        this.name = name;
        this.description = description;
        this.sql = sql;
        this.database = database;
        this.parameters = parameters;
        this.cache = cache;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public List<QueryParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<QueryParameter> parameters) {
        this.parameters = parameters;
    }

    public CacheConfiguration getCache() {
        return cache;
    }

    public void setCache(CacheConfiguration cache) {
        this.cache = cache;
    }

    /**
     * Check if caching is enabled for this query
     */
    public boolean isCacheEnabled() {
        return cache != null && cache.isEnabled();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueryConfig that = (QueryConfig) o;
        return Objects.equals(name, that.name) &&
               Objects.equals(description, that.description) &&
               Objects.equals(sql, that.sql) &&
               Objects.equals(database, that.database) &&
               Objects.equals(parameters, that.parameters) &&
               Objects.equals(cache, that.cache);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, sql, database, parameters, cache);
    }

    @Override
    public String toString() {
        return "QueryConfig{" +
               "name='" + name + '\'' +
               ", description='" + description + '\'' +
               ", sql='" + sql + '\'' +
               ", database='" + database + '\'' +
               ", parameters=" + parameters +
               ", cache=" + cache +
               '}';
    }

    /**
     * Query parameter configuration
     */
    public static class QueryParameter {
        private String name;
        private String type;
        private boolean required;

        // Default constructor
        public QueryParameter() {}

        // Constructor with all fields
        public QueryParameter(String name, String type, boolean required) {
            this.name = name;
            this.type = type;
            this.required = required;
        }

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            QueryParameter that = (QueryParameter) o;
            return required == that.required &&
                   Objects.equals(name, that.name) &&
                   Objects.equals(type, that.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, type, required);
        }

        @Override
        public String toString() {
            return "QueryParameter{" +
                   "name='" + name + '\'' +
                   ", type='" + type + '\'' +
                   ", required=" + required +
                   '}';
        }
    }

    /**
     * Cache configuration for queries
     */
    public static class CacheConfiguration {
        private boolean enabled = false;
        private String strategy = "LRU";
        private int ttl = 300; // Time to live in seconds (5 minutes default)
        private int maxSize = 1000; // Maximum number of entries
        private String keyPattern; // Pattern for generating cache keys
        private List<String> invalidateOn = new ArrayList<>(); // Events that invalidate cache
        private boolean refreshAsync = false; // Refresh cache asynchronously before expiry
        private boolean preload = false; // Preload cache on startup
        private List<InvalidationRuleConfig> invalidationRules = new ArrayList<>(); // Advanced invalidation rules

        // Default constructor
        public CacheConfiguration() {}

        // Constructor with basic settings
        public CacheConfiguration(boolean enabled, String strategy, int ttl, int maxSize) {
            this.enabled = enabled;
            this.strategy = strategy;
            this.ttl = ttl;
            this.maxSize = maxSize;
        }

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getStrategy() {
            return strategy;
        }

        public void setStrategy(String strategy) {
            this.strategy = strategy;
        }

        public int getTtl() {
            return ttl;
        }

        public void setTtl(int ttl) {
            this.ttl = ttl;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }

        public String getKeyPattern() {
            return keyPattern;
        }

        public void setKeyPattern(String keyPattern) {
            this.keyPattern = keyPattern;
        }

        public List<String> getInvalidateOn() {
            return invalidateOn;
        }

        public void setInvalidateOn(List<String> invalidateOn) {
            this.invalidateOn = invalidateOn != null ? invalidateOn : new ArrayList<>();
        }

        public boolean isRefreshAsync() {
            return refreshAsync;
        }

        public void setRefreshAsync(boolean refreshAsync) {
            this.refreshAsync = refreshAsync;
        }

        public boolean isPreload() {
            return preload;
        }

        public void setPreload(boolean preload) {
            this.preload = preload;
        }

        public List<InvalidationRuleConfig> getInvalidationRules() {
            return invalidationRules;
        }

        public void setInvalidationRules(List<InvalidationRuleConfig> invalidationRules) {
            this.invalidationRules = invalidationRules != null ? invalidationRules : new ArrayList<>();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheConfiguration that = (CacheConfiguration) o;
            return enabled == that.enabled &&
                   ttl == that.ttl &&
                   maxSize == that.maxSize &&
                   refreshAsync == that.refreshAsync &&
                   preload == that.preload &&
                   Objects.equals(strategy, that.strategy) &&
                   Objects.equals(keyPattern, that.keyPattern) &&
                   Objects.equals(invalidateOn, that.invalidateOn) &&
                   Objects.equals(invalidationRules, that.invalidationRules);
        }

        @Override
        public int hashCode() {
            return Objects.hash(enabled, strategy, ttl, maxSize, keyPattern, invalidateOn, refreshAsync, preload, invalidationRules);
        }

        @Override
        public String toString() {
            return "CacheConfiguration{" +
                   "enabled=" + enabled +
                   ", strategy='" + strategy + '\'' +
                   ", ttl=" + ttl +
                   ", maxSize=" + maxSize +
                   ", keyPattern='" + keyPattern + '\'' +
                   ", invalidateOn=" + invalidateOn +
                   ", refreshAsync=" + refreshAsync +
                   ", preload=" + preload +
                   ", invalidationRules=" + invalidationRules +
                   '}';
        }
    }

    /**
     * Configuration for cache invalidation rules
     */
    public static class InvalidationRuleConfig {
        private String event;
        private List<String> patterns = new ArrayList<>();
        private String condition;
        private Integer delaySeconds;
        private Boolean async = true;

        // Default constructor
        public InvalidationRuleConfig() {}

        // Constructor with basic settings
        public InvalidationRuleConfig(String event, List<String> patterns) {
            this.event = event;
            this.patterns = patterns != null ? patterns : new ArrayList<>();
        }

        // Getters and setters
        public String getEvent() {
            return event;
        }

        public void setEvent(String event) {
            this.event = event;
        }

        public List<String> getPatterns() {
            return patterns;
        }

        public void setPatterns(List<String> patterns) {
            this.patterns = patterns != null ? patterns : new ArrayList<>();
        }

        public String getCondition() {
            return condition;
        }

        public void setCondition(String condition) {
            this.condition = condition;
        }

        public Integer getDelaySeconds() {
            return delaySeconds;
        }

        public void setDelaySeconds(Integer delaySeconds) {
            this.delaySeconds = delaySeconds;
        }

        public Boolean getAsync() {
            return async;
        }

        public void setAsync(Boolean async) {
            this.async = async != null ? async : true;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InvalidationRuleConfig that = (InvalidationRuleConfig) o;
            return Objects.equals(event, that.event) &&
                   Objects.equals(patterns, that.patterns) &&
                   Objects.equals(condition, that.condition) &&
                   Objects.equals(delaySeconds, that.delaySeconds) &&
                   Objects.equals(async, that.async);
        }

        @Override
        public int hashCode() {
            return Objects.hash(event, patterns, condition, delaySeconds, async);
        }

        @Override
        public String toString() {
            return "InvalidationRuleConfig{" +
                   "event='" + event + '\'' +
                   ", patterns=" + patterns +
                   ", condition='" + condition + '\'' +
                   ", delaySeconds=" + delaySeconds +
                   ", async=" + async +
                   '}';
        }
    }
}
