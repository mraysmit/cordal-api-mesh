package dev.cordal.common.cache;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CacheEventPublisher
 */
class CacheEventPublisherTest {

    private CacheEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new CacheEventPublisher();
    }

    @AfterEach
    void tearDown() {
        publisher.shutdown();
    }

    @Test
    void testSubscribeAndPublishSync() {
        AtomicInteger eventCount = new AtomicInteger(0);
        CacheEventListener listener = event -> eventCount.incrementAndGet();

        publisher.subscribe("test_event", listener);
        
        CacheEvent event = CacheEvent.builder()
            .eventType("test_event")
            .source("test")
            .build();

        publisher.publishSync(event);
        
        assertEquals(1, eventCount.get());
        assertEquals(1, publisher.getListenerCount("test_event"));
    }

    @Test
    void testPublishAsync() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger eventCount = new AtomicInteger(0);
        
        CacheEventListener listener = event -> {
            eventCount.incrementAndGet();
            latch.countDown();
        };

        publisher.subscribe("async_event", listener);
        
        CacheEvent event = CacheEvent.builder()
            .eventType("async_event")
            .source("test")
            .build();

        publisher.publishAsync(event);
        
        // Wait for async processing
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(1, eventCount.get());
    }

    @Test
    void testMultipleListeners() {
        AtomicInteger count1 = new AtomicInteger(0);
        AtomicInteger count2 = new AtomicInteger(0);
        
        publisher.subscribe("multi_event", event -> count1.incrementAndGet());
        publisher.subscribe("multi_event", event -> count2.incrementAndGet());
        
        CacheEvent event = CacheEvent.builder()
            .eventType("multi_event")
            .source("test")
            .build();

        publisher.publishSync(event);
        
        assertEquals(1, count1.get());
        assertEquals(1, count2.get());
        assertEquals(2, publisher.getListenerCount("multi_event"));
    }

    @Test
    void testUnsubscribe() {
        AtomicInteger eventCount = new AtomicInteger(0);
        CacheEventListener listener = event -> eventCount.incrementAndGet();

        publisher.subscribe("unsub_event", listener);
        assertEquals(1, publisher.getListenerCount("unsub_event"));
        
        boolean removed = publisher.unsubscribe("unsub_event", listener);
        assertTrue(removed);
        assertEquals(0, publisher.getListenerCount("unsub_event"));
        
        CacheEvent event = CacheEvent.builder()
            .eventType("unsub_event")
            .source("test")
            .build();

        publisher.publishSync(event);
        assertEquals(0, eventCount.get()); // Should not receive event after unsubscribe
    }

    @Test
    void testNoListenersForEvent() {
        CacheEvent event = CacheEvent.builder()
            .eventType("no_listeners")
            .source("test")
            .build();

        // Should not throw exception when no listeners
        assertDoesNotThrow(() -> publisher.publishSync(event));
        assertDoesNotThrow(() -> publisher.publishAsync(event));
        
        assertEquals(0, publisher.getListenerCount("no_listeners"));
    }

    @Test
    void testListenerException() {
        AtomicInteger successCount = new AtomicInteger(0);
        
        // Listener that throws exception
        CacheEventListener faultyListener = event -> {
            throw new RuntimeException("INTENTIONAL TEST ERROR - This error is expected in tests");
        };
        
        // Listener that should still work
        CacheEventListener goodListener = event -> successCount.incrementAndGet();
        
        publisher.subscribe("exception_event", faultyListener);
        publisher.subscribe("exception_event", goodListener);
        
        CacheEvent event = CacheEvent.builder()
            .eventType("exception_event")
            .source("test")
            .build();

        // Should not throw exception, and good listener should still be called
        assertDoesNotThrow(() -> publisher.publishSync(event));
        assertEquals(1, successCount.get());
    }

    @Test
    void testGetRegisteredEventTypes() {
        publisher.subscribe("event1", event -> {});
        publisher.subscribe("event2", event -> {});
        
        var eventTypes = publisher.getRegisteredEventTypes();
        assertEquals(2, eventTypes.size());
        assertTrue(eventTypes.contains("event1"));
        assertTrue(eventTypes.contains("event2"));
    }

    @Test
    void testShutdown() {
        AtomicInteger eventCount = new AtomicInteger(0);
        publisher.subscribe("shutdown_event", event -> eventCount.incrementAndGet());
        
        publisher.shutdown();
        assertTrue(publisher.isShutdown());
        
        CacheEvent event = CacheEvent.builder()
            .eventType("shutdown_event")
            .source("test")
            .build();

        // Should not process events after shutdown
        publisher.publishSync(event);
        assertEquals(0, eventCount.get());
    }

    @Test
    void testNullHandling() {
        assertThrows(IllegalArgumentException.class, () -> 
            publisher.subscribe(null, event -> {}));
        
        assertThrows(IllegalArgumentException.class, () -> 
            publisher.subscribe("event", null));
    }
}
