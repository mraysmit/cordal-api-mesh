package dev.cordal.common.cache;

import java.time.Instant;

/**
 * Statistics for cache performance monitoring
 */
public class CacheStatistics {
    private final long hitCount;
    private final long missCount;
    private final long evictionCount;
    private final long size;
    private final double hitRate;
    private final double missRate;
    private final Instant timestamp;

    private CacheStatistics(Builder builder) {
        this.hitCount = builder.hitCount;
        this.missCount = builder.missCount;
        this.evictionCount = builder.evictionCount;
        this.size = builder.size;
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        
        long totalRequests = hitCount + missCount;
        if (totalRequests > 0) {
            this.hitRate = (double) hitCount / totalRequests;
            this.missRate = (double) missCount / totalRequests;
        } else {
            this.hitRate = 0.0;
            this.missRate = 0.0;
        }
    }

    public long getHitCount() {
        return hitCount;
    }

    public long getMissCount() {
        return missCount;
    }

    public long getEvictionCount() {
        return evictionCount;
    }

    public long getSize() {
        return size;
    }

    public double getHitRate() {
        return hitRate;
    }

    public double getMissRate() {
        return missRate;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public long getTotalRequests() {
        return hitCount + missCount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long hitCount = 0;
        private long missCount = 0;
        private long evictionCount = 0;
        private long size = 0;
        private Instant timestamp;

        public Builder hitCount(long hitCount) {
            this.hitCount = hitCount;
            return this;
        }

        public Builder missCount(long missCount) {
            this.missCount = missCount;
            return this;
        }

        public Builder evictionCount(long evictionCount) {
            this.evictionCount = evictionCount;
            return this;
        }

        public Builder size(long size) {
            this.size = size;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public CacheStatistics build() {
            return new CacheStatistics(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CacheStatistics that = (CacheStatistics) o;
        return hitCount == that.hitCount &&
               missCount == that.missCount &&
               evictionCount == that.evictionCount &&
               size == that.size &&
               Double.compare(that.hitRate, hitRate) == 0 &&
               Double.compare(that.missRate, missRate) == 0 &&
               java.util.Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(hitCount, missCount, evictionCount, size, hitRate, missRate, timestamp);
    }

    @Override
    public String toString() {
        return "CacheStatistics{" +
               "hitCount=" + hitCount +
               ", missCount=" + missCount +
               ", evictionCount=" + evictionCount +
               ", size=" + size +
               ", hitRate=" + String.format("%.2f%%", hitRate * 100) +
               ", missRate=" + String.format("%.2f%%", missRate * 100) +
               ", timestamp=" + timestamp +
               '}';
    }
}
