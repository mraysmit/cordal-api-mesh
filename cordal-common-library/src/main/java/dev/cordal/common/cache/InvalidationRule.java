package dev.cordal.common.cache;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Represents a cache invalidation rule that defines when and how cache entries should be invalidated
 */
public class InvalidationRule {
    private final String eventType;
    private final List<String> patterns;
    private final String condition;
    private final Duration delay;
    private final boolean async;

    public InvalidationRule(String eventType, List<String> patterns, String condition, Duration delay, boolean async) {
        this.eventType = Objects.requireNonNull(eventType, "Event type cannot be null");
        this.patterns = patterns != null ? List.copyOf(patterns) : List.of();
        this.condition = condition;
        this.delay = delay;
        this.async = async;
    }

    public String getEventType() {
        return eventType;
    }

    public List<String> getPatterns() {
        return patterns;
    }

    public String getCondition() {
        return condition;
    }

    public Duration getDelay() {
        return delay;
    }

    public boolean isAsync() {
        return async;
    }

    public boolean hasCondition() {
        return condition != null && !condition.trim().isEmpty();
    }

    public boolean hasDelay() {
        return delay != null && !delay.isZero() && !delay.isNegative();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InvalidationRule that = (InvalidationRule) o;
        return async == that.async &&
               Objects.equals(eventType, that.eventType) &&
               Objects.equals(patterns, that.patterns) &&
               Objects.equals(condition, that.condition) &&
               Objects.equals(delay, that.delay);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventType, patterns, condition, delay, async);
    }

    @Override
    public String toString() {
        return "InvalidationRule{" +
               "eventType='" + eventType + '\'' +
               ", patterns=" + patterns +
               ", condition='" + condition + '\'' +
               ", delay=" + delay +
               ", async=" + async +
               '}';
    }

    /**
     * Builder for creating invalidation rules
     */
    public static class Builder {
        private String eventType;
        private List<String> patterns = List.of();
        private String condition;
        private Duration delay;
        private boolean async = true;

        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder patterns(List<String> patterns) {
            this.patterns = patterns;
            return this;
        }

        public Builder pattern(String pattern) {
            this.patterns = List.of(pattern);
            return this;
        }

        public Builder condition(String condition) {
            this.condition = condition;
            return this;
        }

        public Builder delay(Duration delay) {
            this.delay = delay;
            return this;
        }

        public Builder async(boolean async) {
            this.async = async;
            return this;
        }

        public Builder sync() {
            this.async = false;
            return this;
        }

        public InvalidationRule build() {
            return new InvalidationRule(eventType, patterns, condition, delay, async);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
