package dev.cordal.hotreload;

import dev.cordal.generic.config.ApiEndpointConfig;
import io.javalin.Javalin;
import io.javalin.http.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages runtime endpoint registration and deregistration with Javalin
 * Provides zero-downtime endpoint updates
 */
@Singleton
public class DynamicEndpointRegistry {
    private static final Logger logger = LoggerFactory.getLogger(DynamicEndpointRegistry.class);
    
    private final Map<String, RegisteredEndpoint> activeEndpoints = new ConcurrentHashMap<>();
    private final AtomicBoolean updateInProgress = new AtomicBoolean(false);
    private final AtomicInteger registrationCounter = new AtomicInteger(0);
    
    private Javalin javalinApp;
    
    @Inject
    public DynamicEndpointRegistry() {
        logger.info("DynamicEndpointRegistry initialized");
    }
    
    /**
     * Set the Javalin application instance
     */
    public void setJavalinApp(Javalin javalinApp) {
        this.javalinApp = javalinApp;
        logger.info("Javalin application set for dynamic endpoint registry");
    }
    
    /**
     * Register a new endpoint dynamically
     */
    public EndpointRegistrationResult registerEndpoint(String name, ApiEndpointConfig config) {
        if (javalinApp == null) {
            return EndpointRegistrationResult.failure("Javalin application not initialized");
        }
        
        logger.info("Registering endpoint: {} -> {} {}", name, config.getMethod(), config.getPath());
        
        try {
            // Check if endpoint already exists
            if (activeEndpoints.containsKey(name)) {
                return EndpointRegistrationResult.failure("Endpoint already exists: " + name);
            }
            
            // Create handler for the endpoint
            Handler handler = createEndpointHandler(name, config);
            
            // Register with Javalin based on HTTP method
            registerWithJavalin(config.getMethod(), config.getPath(), handler);
            
            // Track the registered endpoint
            RegisteredEndpoint registeredEndpoint = new RegisteredEndpoint(
                name, config, handler, System.currentTimeMillis()
            );
            activeEndpoints.put(name, registeredEndpoint);
            
            int totalEndpoints = registrationCounter.incrementAndGet();
            logger.info("Endpoint registered successfully: {} (total active: {})", name, totalEndpoints);
            
            return EndpointRegistrationResult.success("Endpoint registered: " + name);
            
        } catch (Exception e) {
            logger.error("Failed to register endpoint: {}", name, e);
            return EndpointRegistrationResult.failure("Registration failed: " + e.getMessage());
        }
    }
    
    /**
     * Unregister an endpoint dynamically
     */
    public EndpointRegistrationResult unregisterEndpoint(String name) {
        logger.info("Unregistering endpoint: {}", name);
        
        try {
            RegisteredEndpoint endpoint = activeEndpoints.remove(name);
            if (endpoint == null) {
                return EndpointRegistrationResult.failure("Endpoint not found: " + name);
            }
            
            // Note: Javalin doesn't support dynamic route removal in current version
            // We mark the endpoint as inactive instead
            endpoint.setActive(false);
            
            int totalEndpoints = registrationCounter.decrementAndGet();
            logger.info("Endpoint unregistered successfully: {} (total active: {})", name, totalEndpoints);
            
            return EndpointRegistrationResult.success("Endpoint unregistered: " + name);
            
        } catch (Exception e) {
            logger.error("Failed to unregister endpoint: {}", name, e);
            return EndpointRegistrationResult.failure("Unregistration failed: " + e.getMessage());
        }
    }
    
    /**
     * Update an existing endpoint (unregister old, register new)
     */
    public EndpointRegistrationResult updateEndpoint(String name, ApiEndpointConfig newConfig) {
        logger.info("Updating endpoint: {} -> {} {}", name, newConfig.getMethod(), newConfig.getPath());
        
        try {
            // For now, we'll unregister and re-register
            // In a more sophisticated implementation, we might support in-place updates
            EndpointRegistrationResult unregisterResult = unregisterEndpoint(name);
            if (!unregisterResult.isSuccess()) {
                return unregisterResult;
            }
            
            EndpointRegistrationResult registerResult = registerEndpoint(name, newConfig);
            if (!registerResult.isSuccess()) {
                logger.error("Failed to re-register endpoint after update: {}", name);
                return registerResult;
            }
            
            logger.info("Endpoint updated successfully: {}", name);
            return EndpointRegistrationResult.success("Endpoint updated: " + name);
            
        } catch (Exception e) {
            logger.error("Failed to update endpoint: {}", name, e);
            return EndpointRegistrationResult.failure("Update failed: " + e.getMessage());
        }
    }
    
    /**
     * Begin atomic update operation
     */
    public boolean beginAtomicUpdate() {
        boolean acquired = updateInProgress.compareAndSet(false, true);
        if (acquired) {
            logger.info("Atomic update operation started");
        } else {
            logger.warn("Atomic update already in progress");
        }
        return acquired;
    }
    
    /**
     * Commit atomic update operation
     */
    public void commitAtomicUpdate() {
        updateInProgress.set(false);
        logger.info("Atomic update operation committed");
    }
    
    /**
     * Rollback atomic update operation
     */
    public void rollbackAtomicUpdate() {
        updateInProgress.set(false);
        logger.info("Atomic update operation rolled back");
    }
    
    /**
     * Validate all active endpoints
     */
    public EndpointValidationResult validateAllEndpoints() {
        logger.debug("Validating all active endpoints");
        
        EndpointValidationResult.Builder builder = new EndpointValidationResult.Builder();
        
        for (Map.Entry<String, RegisteredEndpoint> entry : activeEndpoints.entrySet()) {
            String name = entry.getKey();
            RegisteredEndpoint endpoint = entry.getValue();
            
            try {
                if (endpoint.isActive()) {
                    validateEndpoint(name, endpoint);
                    builder.addValidEndpoint(name);
                } else {
                    builder.addInactiveEndpoint(name);
                }
            } catch (Exception e) {
                logger.warn("Endpoint validation failed for: {}", name, e);
                builder.addInvalidEndpoint(name, e.getMessage());
            }
        }
        
        EndpointValidationResult result = builder.build();
        logger.debug("Endpoint validation completed: {}", result);
        return result;
    }
    
    /**
     * Get information about all active endpoints
     */
    public Map<String, RegisteredEndpoint> getActiveEndpoints() {
        return Map.copyOf(activeEndpoints);
    }
    
    /**
     * Get registry statistics
     */
    public EndpointRegistryStatistics getStatistics() {
        long activeCount = activeEndpoints.values().stream()
            .mapToLong(endpoint -> endpoint.isActive() ? 1 : 0)
            .sum();
        
        return new EndpointRegistryStatistics(
            activeEndpoints.size(),
            (int) activeCount,
            updateInProgress.get(),
            registrationCounter.get()
        );
    }
    
    /**
     * Create a handler for the endpoint
     */
    private Handler createEndpointHandler(String name, ApiEndpointConfig config) {
        return ctx -> {
            // Check if endpoint is still active
            RegisteredEndpoint endpoint = activeEndpoints.get(name);
            if (endpoint == null || !endpoint.isActive()) {
                ctx.status(404).result("Endpoint not available: " + name);
                return;
            }
            
            // This is a placeholder implementation
            // In a real implementation, this would:
            // 1. Execute the configured query
            // 2. Apply pagination if enabled
            // 3. Format the response
            // 4. Handle errors appropriately
            
            logger.debug("Handling request for endpoint: {} ({})", name, ctx.path());
            ctx.json(Map.of(
                "endpoint", name,
                "path", config.getPath(),
                "method", config.getMethod(),
                "query", config.getQuery(),
                "message", "Dynamic endpoint response"
            ));
        };
    }
    
    /**
     * Register handler with Javalin based on HTTP method
     */
    private void registerWithJavalin(String method, String path, Handler handler) {
        switch (method.toUpperCase()) {
            case "GET" -> javalinApp.get(path, handler);
            case "POST" -> javalinApp.post(path, handler);
            case "PUT" -> javalinApp.put(path, handler);
            case "DELETE" -> javalinApp.delete(path, handler);
            case "PATCH" -> javalinApp.patch(path, handler);
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }
        
        logger.debug("Registered {} {} with Javalin", method, path);
    }
    
    /**
     * Validate a single endpoint
     */
    private void validateEndpoint(String name, RegisteredEndpoint endpoint) {
        // Placeholder validation logic
        // In a real implementation, this might:
        // 1. Check if the referenced query exists
        // 2. Validate database connectivity
        // 3. Test endpoint accessibility
        
        if (endpoint.getConfig().getPath() == null) {
            throw new RuntimeException("Endpoint path is null");
        }
        
        if (endpoint.getConfig().getQuery() == null) {
            throw new RuntimeException("Endpoint query is null");
        }
    }
    
    /**
     * Represents a registered endpoint
     */
    public static class RegisteredEndpoint {
        private final String name;
        private final ApiEndpointConfig config;
        private final Handler handler;
        private final long registrationTime;
        private volatile boolean active = true;
        
        public RegisteredEndpoint(String name, ApiEndpointConfig config, Handler handler, long registrationTime) {
            this.name = name;
            this.config = config;
            this.handler = handler;
            this.registrationTime = registrationTime;
        }
        
        // Getters
        public String getName() { return name; }
        public ApiEndpointConfig getConfig() { return config; }
        public Handler getHandler() { return handler; }
        public long getRegistrationTime() { return registrationTime; }
        public boolean isActive() { return active; }
        
        public void setActive(boolean active) { this.active = active; }
        
        @Override
        public String toString() {
            return String.format("RegisteredEndpoint{name='%s', path='%s', method='%s', active=%s}",
                               name, config.getPath(), config.getMethod(), active);
        }
    }
}
