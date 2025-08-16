package dev.cordal.common.cache;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InvalidationRule
 */
class InvalidationRuleTest {

    @Test
    void testBasicRuleCreation() {
        InvalidationRule rule = new InvalidationRule(
            "trade_executed", 
            List.of("user_portfolio:*"), 
            null, 
            null, 
            true
        );

        assertEquals("trade_executed", rule.getEventType());
        assertEquals(List.of("user_portfolio:*"), rule.getPatterns());
        assertNull(rule.getCondition());
        assertNull(rule.getDelay());
        assertTrue(rule.isAsync());
        assertFalse(rule.hasCondition());
        assertFalse(rule.hasDelay());
    }

    @Test
    void testRuleWithCondition() {
        InvalidationRule rule = InvalidationRule.builder()
            .eventType("user_update")
            .pattern("user:{user_id}:*")
            .condition("user_id = ${event.user_id}")
            .build();

        assertEquals("user_update", rule.getEventType());
        assertEquals(List.of("user:{user_id}:*"), rule.getPatterns());
        assertEquals("user_id = ${event.user_id}", rule.getCondition());
        assertTrue(rule.hasCondition());
        assertFalse(rule.hasDelay());
        assertTrue(rule.isAsync()); // Default
    }

    @Test
    void testRuleWithDelay() {
        Duration delay = Duration.ofMinutes(5);
        InvalidationRule rule = InvalidationRule.builder()
            .eventType("market_close")
            .patterns(List.of("market:*", "portfolio:*"))
            .delay(delay)
            .sync()
            .build();

        assertEquals("market_close", rule.getEventType());
        assertEquals(List.of("market:*", "portfolio:*"), rule.getPatterns());
        assertEquals(delay, rule.getDelay());
        assertTrue(rule.hasDelay());
        assertFalse(rule.isAsync());
    }

    @Test
    void testBuilderDefaults() {
        InvalidationRule rule = InvalidationRule.builder()
            .eventType("test_event")
            .build();

        assertEquals("test_event", rule.getEventType());
        assertTrue(rule.getPatterns().isEmpty());
        assertNull(rule.getCondition());
        assertNull(rule.getDelay());
        assertTrue(rule.isAsync()); // Default
    }

    @Test
    void testBuilderWithMultiplePatterns() {
        List<String> patterns = List.of("cache1:*", "cache2:*", "cache3:*");
        InvalidationRule rule = InvalidationRule.builder()
            .eventType("clear_all")
            .patterns(patterns)
            .build();

        assertEquals(patterns, rule.getPatterns());
    }

    @Test
    void testBuilderAsync() {
        InvalidationRule asyncRule = InvalidationRule.builder()
            .eventType("test")
            .async(true)
            .build();

        assertTrue(asyncRule.isAsync());

        InvalidationRule syncRule = InvalidationRule.builder()
            .eventType("test")
            .async(false)
            .build();

        assertFalse(syncRule.isAsync());
    }

    @Test
    void testEqualsAndHashCode() {
        InvalidationRule rule1 = InvalidationRule.builder()
            .eventType("test")
            .pattern("pattern:*")
            .condition("condition")
            .delay(Duration.ofSeconds(10))
            .async(true)
            .build();

        InvalidationRule rule2 = InvalidationRule.builder()
            .eventType("test")
            .pattern("pattern:*")
            .condition("condition")
            .delay(Duration.ofSeconds(10))
            .async(true)
            .build();

        InvalidationRule rule3 = InvalidationRule.builder()
            .eventType("different")
            .pattern("pattern:*")
            .condition("condition")
            .delay(Duration.ofSeconds(10))
            .async(true)
            .build();

        assertEquals(rule1, rule2);
        assertEquals(rule1.hashCode(), rule2.hashCode());
        assertNotEquals(rule1, rule3);
        assertNotEquals(rule1.hashCode(), rule3.hashCode());
    }

    @Test
    void testToString() {
        InvalidationRule rule = InvalidationRule.builder()
            .eventType("test_event")
            .patterns(List.of("pattern1", "pattern2"))
            .condition("test condition")
            .delay(Duration.ofSeconds(30))
            .async(false)
            .build();

        String toString = rule.toString();
        assertTrue(toString.contains("test_event"));
        assertTrue(toString.contains("pattern1"));
        assertTrue(toString.contains("pattern2"));
        assertTrue(toString.contains("test condition"));
        assertTrue(toString.contains("PT30S"));
        assertTrue(toString.contains("false"));
    }

    @Test
    void testNullEventType() {
        assertThrows(NullPointerException.class, () -> 
            new InvalidationRule(null, List.of(), null, null, true));
    }

    @Test
    void testNullPatterns() {
        InvalidationRule rule = new InvalidationRule("test", null, null, null, true);
        assertTrue(rule.getPatterns().isEmpty());
    }

    @Test
    void testEmptyCondition() {
        InvalidationRule rule1 = InvalidationRule.builder()
            .eventType("test")
            .condition("")
            .build();

        InvalidationRule rule2 = InvalidationRule.builder()
            .eventType("test")
            .condition("   ")
            .build();

        assertFalse(rule1.hasCondition());
        assertFalse(rule2.hasCondition());
    }

    @Test
    void testZeroOrNegativeDelay() {
        InvalidationRule rule1 = InvalidationRule.builder()
            .eventType("test")
            .delay(Duration.ZERO)
            .build();

        InvalidationRule rule2 = InvalidationRule.builder()
            .eventType("test")
            .delay(Duration.ofSeconds(-1))
            .build();

        assertFalse(rule1.hasDelay());
        assertFalse(rule2.hasDelay());
    }
}
