package dev.cordal.common.cache;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Represents an event that can trigger cache invalidation
 */
public class CacheEvent {
    private final String eventType;
    private final String source;
    private final Map<String, Object> data;
    private final Instant timestamp;

    public CacheEvent(String eventType, String source, Map<String, Object> data) {
        this.eventType = Objects.requireNonNull(eventType, "Event type cannot be null");
        this.source = Objects.requireNonNull(source, "Source cannot be null");

        // Handle null values in data - use Collections.unmodifiableMap to allow null values
        if (data == null || data.isEmpty()) {
            this.data = Map.of();
        } else {
            // Create a mutable copy that allows null values
            Map<String, Object> mutableData = new java.util.HashMap<>();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (entry.getKey() != null) {
                    mutableData.put(entry.getKey(), entry.getValue()); // Allow null values
                }
            }
            this.data = java.util.Collections.unmodifiableMap(mutableData);
        }
        this.timestamp = Instant.now();
    }

    public String getEventType() {
        return eventType;
    }

    public String getSource() {
        return source;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Get a data value by key
     * 
     * @param key the key to look up
     * @return the value, or null if not present
     */
    public Object getValue(String key) {
        return data.get(key);
    }

    /**
     * Get a data value by key with type casting
     * 
     * @param key the key to look up
     * @param type the expected type
     * @return the value cast to the specified type, or null if not present or wrong type
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue(String key, Class<T> type) {
        Object value = data.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * Check if the event contains a specific key
     * 
     * @param key the key to check
     * @return true if the key exists in the event data
     */
    public boolean hasKey(String key) {
        return data.containsKey(key);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CacheEvent that = (CacheEvent) o;
        return Objects.equals(eventType, that.eventType) &&
               Objects.equals(source, that.source) &&
               Objects.equals(data, that.data) &&
               Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventType, source, data, timestamp);
    }

    @Override
    public String toString() {
        return "CacheEvent{" +
               "eventType='" + eventType + '\'' +
               ", source='" + source + '\'' +
               ", data=" + data +
               ", timestamp=" + timestamp +
               '}';
    }

    /**
     * Builder for creating cache events
     */
    public static class Builder {
        private String eventType;
        private String source;
        private Map<String, Object> data = Map.of();

        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder data(Map<String, Object> data) {
            this.data = data;
            return this;
        }

        public Builder addData(String key, Object value) {
            if (this.data.isEmpty()) {
                Map<String, Object> newData = new java.util.HashMap<>();
                newData.put(key, value);
                this.data = newData;
            } else {
                Map<String, Object> newData = new java.util.HashMap<>(this.data);
                newData.put(key, value);
                this.data = newData;
            }
            return this;
        }

        public CacheEvent build() {
            return new CacheEvent(eventType, source, data);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
