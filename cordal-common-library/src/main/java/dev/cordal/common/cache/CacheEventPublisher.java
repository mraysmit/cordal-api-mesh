package dev.cordal.common.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Publisher for cache events that supports asynchronous event delivery
 */
@Singleton
public class CacheEventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(CacheEventPublisher.class);
    
    private final Map<String, List<CacheEventListener>> listeners = new ConcurrentHashMap<>();
    private final ExecutorService executorService;
    private volatile boolean shutdown = false;

    public CacheEventPublisher() {
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "cache-event-publisher");
            t.setDaemon(true);
            return t;
        });
        logger.info("CacheEventPublisher initialized");
    }

    /**
     * Subscribe to events of a specific type
     * 
     * @param eventType the type of events to listen for
     * @param listener the listener to register
     */
    public void subscribe(String eventType, CacheEventListener listener) {
        if (eventType == null || listener == null) {
            throw new IllegalArgumentException("Event type and listener cannot be null");
        }
        
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
        logger.debug("Registered listener for event type: {}", eventType);
    }

    /**
     * Unsubscribe from events of a specific type
     * 
     * @param eventType the type of events to stop listening for
     * @param listener the listener to unregister
     * @return true if the listener was removed, false if it wasn't registered
     */
    public boolean unsubscribe(String eventType, CacheEventListener listener) {
        List<CacheEventListener> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            boolean removed = eventListeners.remove(listener);
            if (removed) {
                logger.debug("Unregistered listener for event type: {}", eventType);
                // Clean up empty lists
                if (eventListeners.isEmpty()) {
                    listeners.remove(eventType);
                }
            }
            return removed;
        }
        return false;
    }

    /**
     * Publish an event synchronously
     * 
     * @param event the event to publish
     */
    public void publishSync(CacheEvent event) {
        if (shutdown) {
            logger.warn("Cannot publish event, publisher is shutdown: {}", event.getEventType());
            return;
        }
        
        List<CacheEventListener> eventListeners = listeners.get(event.getEventType());
        if (eventListeners != null && !eventListeners.isEmpty()) {
            logger.debug("Publishing event synchronously: {} to {} listeners", 
                        event.getEventType(), eventListeners.size());
            
            for (CacheEventListener listener : eventListeners) {
                try {
                    listener.onEvent(event);
                } catch (Exception e) {
                    logger.error("Error in event listener for event type: {}", event.getEventType(), e);
                }
            }
        } else {
            logger.debug("No listeners registered for event type: {}", event.getEventType());
        }
    }

    /**
     * Publish an event asynchronously
     * 
     * @param event the event to publish
     */
    public void publishAsync(CacheEvent event) {
        if (shutdown) {
            logger.warn("Cannot publish event, publisher is shutdown: {}", event.getEventType());
            return;
        }
        
        List<CacheEventListener> eventListeners = listeners.get(event.getEventType());
        if (eventListeners != null && !eventListeners.isEmpty()) {
            logger.debug("Publishing event asynchronously: {} to {} listeners", 
                        event.getEventType(), eventListeners.size());
            
            executorService.submit(() -> {
                for (CacheEventListener listener : eventListeners) {
                    try {
                        listener.onEvent(event);
                    } catch (Exception e) {
                        logger.error("Error in async event listener for event type: {}", event.getEventType(), e);
                    }
                }
            });
        } else {
            logger.debug("No listeners registered for event type: {}", event.getEventType());
        }
    }

    /**
     * Publish an event (defaults to asynchronous)
     * 
     * @param event the event to publish
     */
    public void publish(CacheEvent event) {
        publishAsync(event);
    }

    /**
     * Get the number of listeners for a specific event type
     * 
     * @param eventType the event type
     * @return the number of registered listeners
     */
    public int getListenerCount(String eventType) {
        List<CacheEventListener> eventListeners = listeners.get(eventType);
        return eventListeners != null ? eventListeners.size() : 0;
    }

    /**
     * Get all registered event types
     * 
     * @return set of event types that have listeners
     */
    public java.util.Set<String> getRegisteredEventTypes() {
        return listeners.keySet();
    }

    /**
     * Shutdown the event publisher and cleanup resources
     */
    public void shutdown() {
        logger.info("Shutting down CacheEventPublisher");
        shutdown = true;
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        listeners.clear();
    }

    /**
     * Check if the publisher is shutdown
     * 
     * @return true if shutdown, false otherwise
     */
    public boolean isShutdown() {
        return shutdown;
    }
}
