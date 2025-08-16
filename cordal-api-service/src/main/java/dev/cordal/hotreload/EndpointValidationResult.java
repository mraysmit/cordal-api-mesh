package dev.cordal.hotreload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of endpoint validation operation
 */
public class EndpointValidationResult {
    private final List<String> validEndpoints;
    private final List<String> inactiveEndpoints;
    private final Map<String, String> invalidEndpoints;
    private final boolean allValid;
    
    private EndpointValidationResult(Builder builder) {
        this.validEndpoints = List.copyOf(builder.validEndpoints);
        this.inactiveEndpoints = List.copyOf(builder.inactiveEndpoints);
        this.invalidEndpoints = Map.copyOf(builder.invalidEndpoints);
        this.allValid = builder.invalidEndpoints.isEmpty();
    }
    
    public List<String> getValidEndpoints() { return validEndpoints; }
    public List<String> getInactiveEndpoints() { return inactiveEndpoints; }
    public Map<String, String> getInvalidEndpoints() { return invalidEndpoints; }
    public boolean isAllValid() { return allValid; }
    
    public int getTotalEndpoints() {
        return validEndpoints.size() + inactiveEndpoints.size() + invalidEndpoints.size();
    }
    
    public int getValidCount() { return validEndpoints.size(); }
    public int getInactiveCount() { return inactiveEndpoints.size(); }
    public int getInvalidCount() { return invalidEndpoints.size(); }
    
    @Override
    public String toString() {
        return String.format("EndpointValidationResult{total=%d, valid=%d, inactive=%d, invalid=%d}",
                           getTotalEndpoints(), getValidCount(), getInactiveCount(), getInvalidCount());
    }
    
    /**
     * Builder for EndpointValidationResult
     */
    public static class Builder {
        private final List<String> validEndpoints = new ArrayList<>();
        private final List<String> inactiveEndpoints = new ArrayList<>();
        private final Map<String, String> invalidEndpoints = new HashMap<>();
        
        public Builder addValidEndpoint(String name) {
            validEndpoints.add(name);
            return this;
        }
        
        public Builder addInactiveEndpoint(String name) {
            inactiveEndpoints.add(name);
            return this;
        }
        
        public Builder addInvalidEndpoint(String name, String error) {
            invalidEndpoints.put(name, error);
            return this;
        }
        
        public EndpointValidationResult build() {
            return new EndpointValidationResult(this);
        }
    }
}
