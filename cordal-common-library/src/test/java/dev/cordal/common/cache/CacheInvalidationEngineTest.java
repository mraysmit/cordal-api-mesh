package dev.cordal.common.cache;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CacheInvalidationEngine
 */
class CacheInvalidationEngineTest {

    private CacheManager cacheManager;
    private CacheEventPublisher eventPublisher;
    private CacheInvalidationEngine invalidationEngine;

    @BeforeEach
    void setUp() {
        CacheManager.CacheConfiguration config = new CacheManager.CacheConfiguration(100, 300, 1);
        cacheManager = new CacheManager(config);
        eventPublisher = new CacheEventPublisher();
        invalidationEngine = new CacheInvalidationEngine(cacheManager, eventPublisher);
    }

    @AfterEach
    void tearDown() {
        invalidationEngine.shutdown();
        eventPublisher.shutdown();
        cacheManager.shutdown();
    }

    @Test
    void testRegisterAndUnregisterRule() {
        InvalidationRule rule = InvalidationRule.builder()
            .eventType("test_event")
            .pattern("test:*")
            .build();

        invalidationEngine.registerRule(rule);
        
        assertTrue(invalidationEngine.getRegisteredEventTypes().contains("test_event"));
        assertEquals(1, invalidationEngine.getRules("test_event").size());
        assertEquals(rule, invalidationEngine.getRules("test_event").get(0));

        boolean removed = invalidationEngine.unregisterRule(rule);
        assertTrue(removed);
        assertFalse(invalidationEngine.getRegisteredEventTypes().contains("test_event"));
        assertTrue(invalidationEngine.getRules("test_event").isEmpty());
    }

    @Test
    void testSimpleInvalidation() throws InterruptedException {
        // Setup cache with data
        cacheManager.put("test_cache", "user:123:profile", "profile_data");
        cacheManager.put("test_cache", "user:123:settings", "settings_data");
        cacheManager.put("test_cache", "user:456:profile", "other_profile");
        
        // Verify data is cached
        assertTrue(cacheManager.get("test_cache", "user:123:profile", String.class).isPresent());
        assertTrue(cacheManager.get("test_cache", "user:123:settings", String.class).isPresent());
        assertTrue(cacheManager.get("test_cache", "user:456:profile", String.class).isPresent());

        // Register invalidation rule
        InvalidationRule rule = InvalidationRule.builder()
            .eventType("user_update")
            .pattern("user:123:*")
            .build();
        invalidationEngine.registerRule(rule);

        // Publish event
        CacheEvent event = CacheEvent.builder()
            .eventType("user_update")
            .source("test")
            .addData("user_id", 123)
            .build();

        eventPublisher.publishSync(event);

        // Give some time for async processing
        Thread.sleep(100);

        // Verify invalidation
        assertFalse(cacheManager.get("test_cache", "user:123:profile", String.class).isPresent());
        assertFalse(cacheManager.get("test_cache", "user:123:settings", String.class).isPresent());
        assertTrue(cacheManager.get("test_cache", "user:456:profile", String.class).isPresent()); // Should not be affected
    }

    @Test
    void testConditionalInvalidation() throws InterruptedException {
        // Setup cache
        cacheManager.put("test_cache", "user:123:data", "data123");
        cacheManager.put("test_cache", "user:456:data", "data456");

        // Register rule with condition
        InvalidationRule rule = InvalidationRule.builder()
            .eventType("user_action")
            .pattern("user:{user_id}:*")
            .condition("user_id = ${event.user_id}")
            .build();
        invalidationEngine.registerRule(rule);

        // Publish event for user 123
        CacheEvent event = CacheEvent.builder()
            .eventType("user_action")
            .source("test")
            .addData("user_id", 123)
            .build();

        eventPublisher.publishSync(event);
        Thread.sleep(100);

        // Only user 123 data should be invalidated
        assertFalse(cacheManager.get("test_cache", "user:123:data", String.class).isPresent());
        assertTrue(cacheManager.get("test_cache", "user:456:data", String.class).isPresent());
    }

    @Test
    void testDelayedInvalidation() throws InterruptedException {
        // Setup cache
        cacheManager.put("test_cache", "delayed:data", "test_data");
        assertTrue(cacheManager.get("test_cache", "delayed:data", String.class).isPresent());

        // Register rule with delay
        InvalidationRule rule = InvalidationRule.builder()
            .eventType("delayed_event")
            .pattern("delayed:*")
            .delay(Duration.ofMillis(200))
            .build();
        invalidationEngine.registerRule(rule);

        // Publish event
        CacheEvent event = CacheEvent.builder()
            .eventType("delayed_event")
            .source("test")
            .build();

        eventPublisher.publishSync(event);

        // Should still be cached immediately after event
        assertTrue(cacheManager.get("test_cache", "delayed:data", String.class).isPresent());

        // Wait for delay
        Thread.sleep(300);

        // Should be invalidated after delay
        assertFalse(cacheManager.get("test_cache", "delayed:data", String.class).isPresent());
    }

    @Test
    void testMultipleRulesForSameEvent() throws InterruptedException {
        // Setup cache
        cacheManager.put("cache1", "data1", "value1");
        cacheManager.put("cache2", "data2", "value2");

        // Register multiple rules for same event
        InvalidationRule rule1 = InvalidationRule.builder()
            .eventType("multi_event")
            .pattern("data1")
            .build();

        InvalidationRule rule2 = InvalidationRule.builder()
            .eventType("multi_event")
            .pattern("data2")
            .build();

        invalidationEngine.registerRule(rule1);
        invalidationEngine.registerRule(rule2);

        assertEquals(2, invalidationEngine.getRules("multi_event").size());

        // Publish event
        CacheEvent event = CacheEvent.builder()
            .eventType("multi_event")
            .source("test")
            .build();

        eventPublisher.publishSync(event);
        Thread.sleep(100);

        // Both should be invalidated
        assertFalse(cacheManager.get("cache1", "data1", String.class).isPresent());
        assertFalse(cacheManager.get("cache2", "data2", String.class).isPresent());
    }

    @Test
    void testManualInvalidation() {
        // Setup cache
        cacheManager.put("test_cache", "manual:data1", "value1");
        cacheManager.put("test_cache", "manual:data2", "value2");
        cacheManager.put("test_cache", "other:data", "value3");

        // Manual invalidation
        int invalidated = invalidationEngine.manualInvalidate("manual:*");
        assertEquals(2, invalidated);

        // Verify invalidation
        assertFalse(cacheManager.get("test_cache", "manual:data1", String.class).isPresent());
        assertFalse(cacheManager.get("test_cache", "manual:data2", String.class).isPresent());
        assertTrue(cacheManager.get("test_cache", "other:data", String.class).isPresent());
    }

    @Test
    void testVariableSubstitutionInPattern() throws InterruptedException {
        // Setup cache
        cacheManager.put("test_cache", "user:123:profile", "profile123");
        cacheManager.put("test_cache", "user:456:profile", "profile456");

        // Register rule with variable substitution
        InvalidationRule rule = InvalidationRule.builder()
            .eventType("user_specific")
            .pattern("user:{user_id}:*")
            .build();
        invalidationEngine.registerRule(rule);

        // Publish event with user_id
        CacheEvent event = CacheEvent.builder()
            .eventType("user_specific")
            .source("test")
            .addData("user_id", 123)
            .build();

        eventPublisher.publishSync(event);
        Thread.sleep(100);

        // Only user 123 should be invalidated
        assertFalse(cacheManager.get("test_cache", "user:123:profile", String.class).isPresent());
        assertTrue(cacheManager.get("test_cache", "user:456:profile", String.class).isPresent());
    }

    @Test
    void testGetStatistics() {
        InvalidationRule rule1 = InvalidationRule.builder()
            .eventType("event1")
            .pattern("pattern1")
            .build();

        InvalidationRule rule2 = InvalidationRule.builder()
            .eventType("event2")
            .pattern("pattern2")
            .build();

        invalidationEngine.registerRule(rule1);
        invalidationEngine.registerRule(rule2);

        Map<String, Object> stats = invalidationEngine.getStatistics();
        
        assertEquals(2, ((java.util.Set<?>) stats.get("registeredEventTypes")).size());
        assertEquals(2, stats.get("totalRules"));
        assertEquals(false, stats.get("shutdown"));
    }

    @Test
    void testNullRuleHandling() {
        assertThrows(IllegalArgumentException.class, () -> 
            invalidationEngine.registerRule(null));

        assertFalse(invalidationEngine.unregisterRule(null));
    }

    @Test
    void testShutdown() {
        InvalidationRule rule = InvalidationRule.builder()
            .eventType("test_event")
            .pattern("test:*")
            .build();

        invalidationEngine.registerRule(rule);
        assertTrue(invalidationEngine.getRegisteredEventTypes().contains("test_event"));

        invalidationEngine.shutdown();

        // After shutdown, should be empty
        assertTrue(invalidationEngine.getRegisteredEventTypes().isEmpty());
        
        Map<String, Object> stats = invalidationEngine.getStatistics();
        assertEquals(true, stats.get("shutdown"));
    }

    @Test
    void testConditionNotMet() throws InterruptedException {
        // Setup cache
        cacheManager.put("test_cache", "conditional:data", "test_data");

        // Register rule with condition that won't be met
        InvalidationRule rule = InvalidationRule.builder()
            .eventType("conditional_event")
            .pattern("conditional:*")
            .condition("user_id = 999") // Event will have user_id = 123
            .build();
        invalidationEngine.registerRule(rule);

        // Publish event
        CacheEvent event = CacheEvent.builder()
            .eventType("conditional_event")
            .source("test")
            .addData("user_id", 123)
            .build();

        eventPublisher.publishSync(event);
        Thread.sleep(100);

        // Should NOT be invalidated because condition not met
        assertTrue(cacheManager.get("test_cache", "conditional:data", String.class).isPresent());
    }
}
