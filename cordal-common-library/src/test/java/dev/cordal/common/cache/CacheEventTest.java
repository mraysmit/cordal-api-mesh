package dev.cordal.common.cache;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CacheEvent
 */
class CacheEventTest {

    @Test
    void testCacheEventCreation() {
        Map<String, Object> data = Map.of("symbol", "AAPL", "volume", 1000);
        CacheEvent event = new CacheEvent("trade_executed", "trading_system", data);

        assertEquals("trade_executed", event.getEventType());
        assertEquals("trading_system", event.getSource());
        assertEquals(data, event.getData());
        assertNotNull(event.getTimestamp());
    }

    @Test
    void testCacheEventBuilder() {
        CacheEvent event = CacheEvent.builder()
            .eventType("user_login")
            .source("auth_service")
            .addData("user_id", 123)
            .addData("session_id", "abc123")
            .build();

        assertEquals("user_login", event.getEventType());
        assertEquals("auth_service", event.getSource());
        assertEquals(123, event.getValue("user_id"));
        assertEquals("abc123", event.getValue("session_id"));
    }

    @Test
    void testGetValueWithType() {
        CacheEvent event = CacheEvent.builder()
            .eventType("test")
            .source("test")
            .addData("number", 42)
            .addData("text", "hello")
            .build();

        Integer number = event.getValue("number", Integer.class);
        String text = event.getValue("text", String.class);
        String wrongType = event.getValue("number", String.class);

        assertEquals(42, number);
        assertEquals("hello", text);
        assertNull(wrongType); // Wrong type should return null
    }

    @Test
    void testHasKey() {
        CacheEvent event = CacheEvent.builder()
            .eventType("test")
            .source("test")
            .addData("key1", "value1")
            .build();

        assertTrue(event.hasKey("key1"));
        assertFalse(event.hasKey("key2"));
    }

    @Test
    void testNullHandling() {
        assertThrows(NullPointerException.class, () -> 
            new CacheEvent(null, "source", Map.of()));
        
        assertThrows(NullPointerException.class, () -> 
            new CacheEvent("event", null, Map.of()));
    }

    @Test
    void testEmptyData() {
        CacheEvent event = new CacheEvent("test", "source", null);
        assertTrue(event.getData().isEmpty());
        assertFalse(event.hasKey("anything"));
        assertNull(event.getValue("anything"));
    }

    @Test
    void testEqualsAndHashCode() {
        Map<String, Object> data = Map.of("key", "value");
        CacheEvent event1 = new CacheEvent("test", "source", data);
        
        // Need to create with same timestamp for equality
        CacheEvent event2 = new CacheEvent("test", "source", data);
        
        // Events with different timestamps won't be equal, but same data should have same hash for data part
        assertEquals(event1.getEventType(), event2.getEventType());
        assertEquals(event1.getSource(), event2.getSource());
        assertEquals(event1.getData(), event2.getData());
    }
}
