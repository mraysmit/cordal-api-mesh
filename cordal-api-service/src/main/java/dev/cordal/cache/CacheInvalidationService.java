package dev.cordal.cache;

import dev.cordal.common.cache.CacheEvent;
import dev.cordal.common.cache.CacheEventPublisher;
import dev.cordal.common.cache.CacheInvalidationEngine;
import dev.cordal.common.cache.InvalidationRule;
import dev.cordal.generic.config.QueryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Service for managing cache invalidation rules and publishing cache events
 */
@Singleton
public class CacheInvalidationService {
    private static final Logger logger = LoggerFactory.getLogger(CacheInvalidationService.class);
    
    private final CacheEventPublisher eventPublisher;
    private final CacheInvalidationEngine invalidationEngine;

    @Inject
    public CacheInvalidationService(CacheEventPublisher eventPublisher, CacheInvalidationEngine invalidationEngine) {
        this.eventPublisher = eventPublisher;
        this.invalidationEngine = invalidationEngine;
        logger.info("CacheInvalidationService initialized");
    }

    /**
     * Register invalidation rules from query configuration
     * 
     * @param queryConfig the query configuration containing cache settings
     */
    public void registerInvalidationRules(QueryConfig queryConfig) {
        if (!queryConfig.isCacheEnabled()) {
            return;
        }

        QueryConfig.CacheConfiguration cacheConfig = queryConfig.getCache();
        
        // Register simple event-based invalidation (backward compatibility)
        for (String eventType : cacheConfig.getInvalidateOn()) {
            InvalidationRule rule = InvalidationRule.builder()
                .eventType(eventType)
                .pattern(cacheConfig.getKeyPattern() != null ? cacheConfig.getKeyPattern() : queryConfig.getName() + ":*")
                .async(true)
                .build();
            
            invalidationEngine.registerRule(rule);
            logger.debug("Registered simple invalidation rule for query {} on event {}", 
                        queryConfig.getName(), eventType);
        }
        
        // Register advanced invalidation rules
        for (QueryConfig.InvalidationRuleConfig ruleConfig : cacheConfig.getInvalidationRules()) {
            InvalidationRule.Builder ruleBuilder = InvalidationRule.builder()
                .eventType(ruleConfig.getEvent())
                .patterns(ruleConfig.getPatterns())
                .condition(ruleConfig.getCondition())
                .async(ruleConfig.getAsync() != null ? ruleConfig.getAsync() : true);
            
            if (ruleConfig.getDelaySeconds() != null) {
                ruleBuilder.delay(Duration.ofSeconds(ruleConfig.getDelaySeconds()));
            }
            
            InvalidationRule rule = ruleBuilder.build();
            invalidationEngine.registerRule(rule);
            
            logger.debug("Registered advanced invalidation rule for query {} on event {} with patterns {}", 
                        queryConfig.getName(), ruleConfig.getEvent(), ruleConfig.getPatterns());
        }
    }

    /**
     * Publish a cache event
     * 
     * @param eventType the type of event
     * @param source the source of the event
     * @param data the event data
     */
    public void publishEvent(String eventType, String source, Map<String, Object> data) {
        CacheEvent event = CacheEvent.builder()
            .eventType(eventType)
            .source(source)
            .data(data)
            .build();
        
        eventPublisher.publish(event);
        logger.debug("Published cache event: {} from source: {}", eventType, source);
    }

    /**
     * Publish a cache event synchronously
     * 
     * @param eventType the type of event
     * @param source the source of the event
     * @param data the event data
     */
    public void publishEventSync(String eventType, String source, Map<String, Object> data) {
        CacheEvent event = CacheEvent.builder()
            .eventType(eventType)
            .source(source)
            .data(data)
            .build();
        
        eventPublisher.publishSync(event);
        logger.debug("Published cache event synchronously: {} from source: {}", eventType, source);
    }

    /**
     * Manually trigger cache invalidation
     * 
     * @param patterns the cache key patterns to invalidate
     * @return the number of cache entries invalidated
     */
    public int manualInvalidate(String... patterns) {
        return invalidationEngine.manualInvalidate(patterns);
    }

    /**
     * Get statistics about the invalidation engine
     * 
     * @return map of statistics
     */
    public Map<String, Object> getInvalidationStatistics() {
        return invalidationEngine.getStatistics();
    }

    /**
     * Get all registered invalidation rules for a specific event type
     * 
     * @param eventType the event type
     * @return list of invalidation rules
     */
    public List<InvalidationRule> getInvalidationRules(String eventType) {
        return invalidationEngine.getRules(eventType);
    }

    /**
     * Get all registered event types
     * 
     * @return set of event types that have invalidation rules
     */
    public java.util.Set<String> getRegisteredEventTypes() {
        return invalidationEngine.getRegisteredEventTypes();
    }

    /**
     * Get the number of listeners for a specific event type
     * 
     * @param eventType the event type
     * @return the number of registered listeners
     */
    public int getListenerCount(String eventType) {
        return eventPublisher.getListenerCount(eventType);
    }

    /**
     * Shutdown the invalidation service
     */
    public void shutdown() {
        logger.info("Shutting down CacheInvalidationService");
        invalidationEngine.shutdown();
        eventPublisher.shutdown();
    }
}
