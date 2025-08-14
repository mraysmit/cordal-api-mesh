package dev.cordal.generic.config;

import java.util.List;
import java.util.Objects;

/**
 * Configuration model for SQL queries
 */
public class QueryConfig {
    private String name;
    private String description;
    private String sql;
    private String database; // Reference to database configuration
    private List<QueryParameter> parameters;

    // Default constructor
    public QueryConfig() {}

    // Constructor with all fields
    public QueryConfig(String name, String description, String sql, String database, List<QueryParameter> parameters) {
        this.name = name;
        this.description = description;
        this.sql = sql;
        this.database = database;
        this.parameters = parameters;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public List<QueryParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<QueryParameter> parameters) {
        this.parameters = parameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueryConfig that = (QueryConfig) o;
        return Objects.equals(name, that.name) &&
               Objects.equals(description, that.description) &&
               Objects.equals(sql, that.sql) &&
               Objects.equals(database, that.database) &&
               Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, sql, database, parameters);
    }

    @Override
    public String toString() {
        return "QueryConfig{" +
               "name='" + name + '\'' +
               ", description='" + description + '\'' +
               ", sql='" + sql + '\'' +
               ", database='" + database + '\'' +
               ", parameters=" + parameters +
               '}';
    }

    /**
     * Query parameter configuration
     */
    public static class QueryParameter {
        private String name;
        private String type;
        private boolean required;

        // Default constructor
        public QueryParameter() {}

        // Constructor with all fields
        public QueryParameter(String name, String type, boolean required) {
            this.name = name;
            this.type = type;
            this.required = required;
        }

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            QueryParameter that = (QueryParameter) o;
            return required == that.required &&
                   Objects.equals(name, that.name) &&
                   Objects.equals(type, that.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, type, required);
        }

        @Override
        public String toString() {
            return "QueryParameter{" +
                   "name='" + name + '\'' +
                   ", type='" + type + '\'' +
                   ", required=" + required +
                   '}';
        }
    }
}
