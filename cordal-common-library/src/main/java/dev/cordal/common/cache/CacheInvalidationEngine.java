package dev.cordal.common.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Engine that handles cache invalidation based on events and rules
 */
@Singleton
public class CacheInvalidationEngine {
    private static final Logger logger = LoggerFactory.getLogger(CacheInvalidationEngine.class);
    
    private final CacheManager cacheManager;
    private final CacheEventPublisher eventPublisher;
    private final Map<String, List<InvalidationRule>> rulesByEventType = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduledExecutor;
    private volatile boolean shutdown = false;

    @Inject
    public CacheInvalidationEngine(CacheManager cacheManager, CacheEventPublisher eventPublisher) {
        this.cacheManager = cacheManager;
        this.eventPublisher = eventPublisher;
        this.scheduledExecutor = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "cache-invalidation-engine");
            t.setDaemon(true);
            return t;
        });
        
        logger.info("CacheInvalidationEngine initialized");
    }

    /**
     * Register an invalidation rule
     * 
     * @param rule the invalidation rule to register
     */
    public void registerRule(InvalidationRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("Invalidation rule cannot be null");
        }

        rulesByEventType.computeIfAbsent(rule.getEventType(), k -> new CopyOnWriteArrayList<>()).add(rule);
        
        // Subscribe to the event type if this is the first rule for it
        if (rulesByEventType.get(rule.getEventType()).size() == 1) {
            eventPublisher.subscribe(rule.getEventType(), this::handleEvent);
            logger.debug("Subscribed to event type: {}", rule.getEventType());
        }
        
        logger.debug("Registered invalidation rule for event type: {}", rule.getEventType());
    }

    /**
     * Unregister an invalidation rule
     * 
     * @param rule the invalidation rule to unregister
     * @return true if the rule was removed, false if it wasn't registered
     */
    public boolean unregisterRule(InvalidationRule rule) {
        if (rule == null) {
            return false;
        }

        List<InvalidationRule> rules = rulesByEventType.get(rule.getEventType());
        if (rules != null) {
            boolean removed = rules.remove(rule);
            if (removed) {
                logger.debug("Unregistered invalidation rule for event type: {}", rule.getEventType());
                
                // Unsubscribe from event type if no more rules
                if (rules.isEmpty()) {
                    eventPublisher.unsubscribe(rule.getEventType(), this::handleEvent);
                    rulesByEventType.remove(rule.getEventType());
                    logger.debug("Unsubscribed from event type: {}", rule.getEventType());
                }
            }
            return removed;
        }
        return false;
    }

    /**
     * Get all rules for a specific event type
     * 
     * @param eventType the event type
     * @return list of rules for the event type
     */
    public List<InvalidationRule> getRules(String eventType) {
        List<InvalidationRule> rules = rulesByEventType.get(eventType);
        return rules != null ? List.copyOf(rules) : List.of();
    }

    /**
     * Get all registered event types
     * 
     * @return set of event types that have rules
     */
    public java.util.Set<String> getRegisteredEventTypes() {
        return rulesByEventType.keySet();
    }

    /**
     * Handle a cache event by applying matching invalidation rules
     * 
     * @param event the cache event to handle
     */
    private void handleEvent(CacheEvent event) {
        if (shutdown) {
            return;
        }

        List<InvalidationRule> rules = rulesByEventType.get(event.getEventType());
        if (rules == null || rules.isEmpty()) {
            return;
        }

        logger.debug("Processing {} invalidation rules for event: {}", rules.size(), event.getEventType());

        for (InvalidationRule rule : rules) {
            try {
                processRule(rule, event);
            } catch (Exception e) {
                logger.error("Error processing invalidation rule for event {}: {}", 
                           event.getEventType(), e.getMessage(), e);
            }
        }
    }

    /**
     * Process a single invalidation rule against an event
     * 
     * @param rule the invalidation rule
     * @param event the cache event
     */
    private void processRule(InvalidationRule rule, CacheEvent event) {
        // Check if the condition is met
        if (rule.hasCondition() && !ConditionEvaluator.evaluate(rule.getCondition(), event)) {
            logger.debug("Condition not met for rule: {}", rule.getCondition());
            return;
        }

        // Create the invalidation task
        Runnable invalidationTask = () -> executeInvalidation(rule, event);

        // Execute with delay if specified
        if (rule.hasDelay()) {
            scheduledExecutor.schedule(invalidationTask, rule.getDelay().toMillis(), TimeUnit.MILLISECONDS);
            logger.debug("Scheduled delayed invalidation for patterns: {} with delay: {}", 
                        rule.getPatterns(), rule.getDelay());
        } else {
            // Execute immediately
            if (rule.isAsync()) {
                scheduledExecutor.submit(invalidationTask);
            } else {
                invalidationTask.run();
            }
        }
    }

    /**
     * Execute the actual cache invalidation
     * 
     * @param rule the invalidation rule
     * @param event the cache event that triggered the invalidation
     */
    private void executeInvalidation(InvalidationRule rule, CacheEvent event) {
        if (shutdown) {
            return;
        }

        int totalInvalidated = 0;
        
        for (String pattern : rule.getPatterns()) {
            try {
                // Substitute variables in the pattern
                String resolvedPattern = substituteVariablesInPattern(pattern, event);
                
                // Invalidate from all caches
                for (String cacheName : cacheManager.getCacheNames()) {
                    int invalidated = cacheManager.invalidate(cacheName, resolvedPattern);
                    totalInvalidated += invalidated;
                }
                
                logger.debug("Invalidated cache entries matching pattern: {} (resolved: {})", 
                           pattern, resolvedPattern);
            } catch (Exception e) {
                logger.error("Error invalidating cache with pattern {}: {}", pattern, e.getMessage(), e);
            }
        }

        if (totalInvalidated > 0) {
            logger.info("Cache invalidation completed: {} entries invalidated for event {} with patterns {}", 
                       totalInvalidated, event.getEventType(), rule.getPatterns());
        }
    }

    /**
     * Substitute variables in a pattern with values from the event
     * 
     * @param pattern the pattern with variables like "user:{user_id}:*"
     * @param event the event containing the data
     * @return the pattern with variables substituted
     */
    private String substituteVariablesInPattern(String pattern, CacheEvent event) {
        String result = pattern;
        
        // Simple variable substitution for patterns like {variable}
        java.util.regex.Pattern variablePattern = java.util.regex.Pattern.compile("\\{([^}]+)\\}");
        java.util.regex.Matcher matcher = variablePattern.matcher(pattern);
        
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String variable = matcher.group(1);
            Object value = event.getValue(variable);
            String replacement = value != null ? value.toString() : variable;
            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }

    /**
     * Manually trigger cache invalidation for testing or administrative purposes
     * 
     * @param patterns the cache key patterns to invalidate
     * @return the number of cache entries invalidated
     */
    public int manualInvalidate(String... patterns) {
        int totalInvalidated = 0;
        
        for (String pattern : patterns) {
            for (String cacheName : cacheManager.getCacheNames()) {
                int invalidated = cacheManager.invalidate(cacheName, pattern);
                totalInvalidated += invalidated;
            }
        }
        
        logger.info("Manual cache invalidation completed: {} entries invalidated with patterns {}", 
                   totalInvalidated, java.util.Arrays.toString(patterns));
        return totalInvalidated;
    }

    /**
     * Get statistics about the invalidation engine
     * 
     * @return map of statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("registeredEventTypes", rulesByEventType.keySet());
        stats.put("totalRules", rulesByEventType.values().stream().mapToInt(List::size).sum());
        stats.put("shutdown", shutdown);
        return stats;
    }

    /**
     * Shutdown the invalidation engine and cleanup resources
     */
    public void shutdown() {
        logger.info("Shutting down CacheInvalidationEngine");
        shutdown = true;
        
        // Unsubscribe from all events
        for (String eventType : rulesByEventType.keySet()) {
            eventPublisher.unsubscribe(eventType, this::handleEvent);
        }
        rulesByEventType.clear();
        
        scheduledExecutor.shutdown();
        try {
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
