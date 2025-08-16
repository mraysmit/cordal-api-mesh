package dev.cordal.common.cache;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConditionEvaluator
 */
class ConditionEvaluatorTest {

    @Test
    void testSimpleEquality() {
        CacheEvent event = CacheEvent.builder()
            .eventType("test")
            .source("test")
            .addData("symbol", "AAPL")
            .addData("volume", 1000)
            .build();

        assertTrue(ConditionEvaluator.evaluate("symbol = AAPL", event));
        assertFalse(ConditionEvaluator.evaluate("symbol = GOOGL", event));
        assertTrue(ConditionEvaluator.evaluate("volume = 1000", event));
    }

    @Test
    void testVariableSubstitution() {
        CacheEvent event = CacheEvent.builder()
            .eventType("trade_executed")
            .source("trading_system")
            .addData("symbol", "AAPL")
            .addData("user_id", 123)
            .build();

        assertTrue(ConditionEvaluator.evaluate("symbol = ${event.symbol}", event));
        assertTrue(ConditionEvaluator.evaluate("user_id = ${event.user_id}", event));
        assertFalse(ConditionEvaluator.evaluate("symbol = ${event.user_id}", event));
    }

    @Test
    void testNumericComparisons() {
        CacheEvent event = CacheEvent.builder()
            .eventType("test")
            .source("test")
            .addData("volume", 1000)
            .addData("price", 150.5)
            .build();

        assertTrue(ConditionEvaluator.evaluate("volume > 500", event));
        assertTrue(ConditionEvaluator.evaluate("volume >= 1000", event));
        assertTrue(ConditionEvaluator.evaluate("volume < 2000", event));
        assertTrue(ConditionEvaluator.evaluate("volume <= 1000", event));
        assertFalse(ConditionEvaluator.evaluate("volume > 1000", event));

        assertTrue(ConditionEvaluator.evaluate("price > 150", event));
        assertTrue(ConditionEvaluator.evaluate("price >= 150.5", event));
    }

    @Test
    void testNotEquals() {
        CacheEvent event = CacheEvent.builder()
            .eventType("test")
            .source("test")
            .addData("status", "active")
            .build();

        assertTrue(ConditionEvaluator.evaluate("status != inactive", event));
        assertFalse(ConditionEvaluator.evaluate("status != active", event));
    }

    @Test
    void testNullValues() {
        CacheEvent event = CacheEvent.builder()
            .eventType("test")
            .source("test")
            .addData("value", null)
            .build();

        assertTrue(ConditionEvaluator.evaluate("value = null", event));
        assertFalse(ConditionEvaluator.evaluate("value = something", event));
    }

    @Test
    void testEmptyOrNullCondition() {
        CacheEvent event = CacheEvent.builder()
            .eventType("test")
            .source("test")
            .build();

        assertTrue(ConditionEvaluator.evaluate(null, event));
        assertTrue(ConditionEvaluator.evaluate("", event));
        assertTrue(ConditionEvaluator.evaluate("   ", event));
    }

    @Test
    void testInvalidConditions() {
        CacheEvent event = CacheEvent.builder()
            .eventType("test")
            .source("test")
            .addData("value", "test")
            .build();

        // Invalid condition format should return false
        assertFalse(ConditionEvaluator.evaluate("invalid condition format", event));
        assertFalse(ConditionEvaluator.evaluate("value", event)); // Missing operator
        assertFalse(ConditionEvaluator.evaluate("= value", event)); // Missing left operand
    }

    @Test
    void testQuotedValues() {
        CacheEvent event = CacheEvent.builder()
            .eventType("test")
            .source("test")
            .addData("message", "hello world")
            .build();

        assertTrue(ConditionEvaluator.evaluate("message = 'hello world'", event));
        assertTrue(ConditionEvaluator.evaluate("message = \"hello world\"", event));
        assertFalse(ConditionEvaluator.evaluate("message = 'hello'", event));
    }

    @Test
    void testComplexVariableSubstitution() {
        CacheEvent event = CacheEvent.builder()
            .eventType("user_action")
            .source("web_app")
            .addData("user_id", 123)
            .addData("action", "purchase")
            .addData("amount", 99.99)
            .build();

        // Note: AND logic is not implemented yet, so test individual conditions
        assertTrue(ConditionEvaluator.evaluate("user_id = ${event.user_id}", event));
        assertTrue(ConditionEvaluator.evaluate("action = ${event.action}", event));
        assertTrue(ConditionEvaluator.evaluate("amount > 50", event));
    }

    @Test
    void testDotNotationVariables() {
        CacheEvent event = CacheEvent.builder()
            .eventType("test")
            .source("test")
            .addData("symbol", "AAPL")
            .build();

        // Both event.symbol and data.symbol should work
        assertTrue(ConditionEvaluator.evaluate("symbol = ${event.symbol}", event));
        assertTrue(ConditionEvaluator.evaluate("symbol = ${data.symbol}", event));
        assertTrue(ConditionEvaluator.evaluate("symbol = ${symbol}", event)); // Direct key lookup
    }
}
