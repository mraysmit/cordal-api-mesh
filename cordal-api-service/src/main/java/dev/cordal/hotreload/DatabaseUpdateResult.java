package dev.cordal.hotreload;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of database configuration updates
 */
public class DatabaseUpdateResult {
    private final List<String> addedDatabases;
    private final List<String> updatedDatabases;
    private final List<String> removedDatabases;
    private final List<String> errors;
    private final boolean success;
    
    private DatabaseUpdateResult(Builder builder) {
        this.addedDatabases = List.copyOf(builder.addedDatabases);
        this.updatedDatabases = List.copyOf(builder.updatedDatabases);
        this.removedDatabases = List.copyOf(builder.removedDatabases);
        this.errors = List.copyOf(builder.errors);
        this.success = builder.errors.isEmpty();
    }
    
    public List<String> getAddedDatabases() { return addedDatabases; }
    public List<String> getUpdatedDatabases() { return updatedDatabases; }
    public List<String> getRemovedDatabases() { return removedDatabases; }
    public List<String> getErrors() { return errors; }
    public boolean isSuccess() { return success; }
    
    public int getTotalChanges() {
        return addedDatabases.size() + updatedDatabases.size() + removedDatabases.size();
    }

    public String getError() {
        return errors.isEmpty() ? null : String.join("; ", errors);
    }
    
    @Override
    public String toString() {
        return String.format("DatabaseUpdateResult{success=%s, added=%d, updated=%d, removed=%d, errors=%d}",
                           success, addedDatabases.size(), updatedDatabases.size(), 
                           removedDatabases.size(), errors.size());
    }
    
    /**
     * Builder for DatabaseUpdateResult
     */
    public static class Builder {
        private final List<String> addedDatabases = new ArrayList<>();
        private final List<String> updatedDatabases = new ArrayList<>();
        private final List<String> removedDatabases = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();
        
        public Builder addedDatabase(String name) {
            addedDatabases.add(name);
            return this;
        }
        
        public Builder updatedDatabase(String name) {
            updatedDatabases.add(name);
            return this;
        }
        
        public Builder removedDatabase(String name) {
            removedDatabases.add(name);
            return this;
        }
        
        public Builder addError(String error) {
            errors.add(error);
            return this;
        }
        
        public DatabaseUpdateResult build() {
            return new DatabaseUpdateResult(this);
        }
    }
}
