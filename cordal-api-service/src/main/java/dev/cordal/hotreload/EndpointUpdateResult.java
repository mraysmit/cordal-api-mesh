package dev.cordal.hotreload;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of endpoint configuration updates
 */
public class EndpointUpdateResult {
    private final List<String> addedEndpoints;
    private final List<String> updatedEndpoints;
    private final List<String> removedEndpoints;
    private final List<String> errors;
    private final boolean success;
    
    private EndpointUpdateResult(Builder builder) {
        this.addedEndpoints = List.copyOf(builder.addedEndpoints);
        this.updatedEndpoints = List.copyOf(builder.updatedEndpoints);
        this.removedEndpoints = List.copyOf(builder.removedEndpoints);
        this.errors = List.copyOf(builder.errors);
        this.success = builder.errors.isEmpty();
    }
    
    public List<String> getAddedEndpoints() { return addedEndpoints; }
    public List<String> getUpdatedEndpoints() { return updatedEndpoints; }
    public List<String> getRemovedEndpoints() { return removedEndpoints; }
    public List<String> getErrors() { return errors; }
    public boolean isSuccess() { return success; }
    
    public int getTotalChanges() {
        return addedEndpoints.size() + updatedEndpoints.size() + removedEndpoints.size();
    }

    public String getError() {
        return errors.isEmpty() ? null : String.join("; ", errors);
    }
    
    @Override
    public String toString() {
        return String.format("EndpointUpdateResult{success=%s, added=%d, updated=%d, removed=%d, errors=%d}",
                           success, addedEndpoints.size(), updatedEndpoints.size(), 
                           removedEndpoints.size(), errors.size());
    }
    
    /**
     * Builder for EndpointUpdateResult
     */
    public static class Builder {
        private final List<String> addedEndpoints = new ArrayList<>();
        private final List<String> updatedEndpoints = new ArrayList<>();
        private final List<String> removedEndpoints = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();
        
        public Builder addedEndpoint(String name) {
            addedEndpoints.add(name);
            return this;
        }
        
        public Builder updatedEndpoint(String name) {
            updatedEndpoints.add(name);
            return this;
        }
        
        public Builder removedEndpoint(String name) {
            removedEndpoints.add(name);
            return this;
        }
        
        public Builder addError(String error) {
            errors.add(error);
            return this;
        }
        
        public EndpointUpdateResult build() {
            return new EndpointUpdateResult(this);
        }
    }
}
