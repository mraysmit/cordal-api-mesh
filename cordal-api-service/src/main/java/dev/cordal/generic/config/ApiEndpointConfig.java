package dev.cordal.generic.config;

import java.util.List;
import java.util.Objects;

/**
 * Configuration model for API endpoints
 */
public class ApiEndpointConfig {
    private String path;
    private String method;
    private String description;
    private String query;
    private String countQuery;
    private PaginationConfig pagination;
    private List<EndpointParameter> parameters;
    private ResponseConfig response;

    // Default constructor
    public ApiEndpointConfig() {}

    // Getters and Setters
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getCountQuery() {
        return countQuery;
    }

    public void setCountQuery(String countQuery) {
        this.countQuery = countQuery;
    }

    public PaginationConfig getPagination() {
        return pagination;
    }

    public void setPagination(PaginationConfig pagination) {
        this.pagination = pagination;
    }

    public List<EndpointParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<EndpointParameter> parameters) {
        this.parameters = parameters;
    }

    public ResponseConfig getResponse() {
        return response;
    }

    public void setResponse(ResponseConfig response) {
        this.response = response;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiEndpointConfig that = (ApiEndpointConfig) o;
        return Objects.equals(path, that.path) &&
               Objects.equals(method, that.method) &&
               Objects.equals(description, that.description) &&
               Objects.equals(query, that.query) &&
               Objects.equals(countQuery, that.countQuery) &&
               Objects.equals(pagination, that.pagination) &&
               Objects.equals(parameters, that.parameters) &&
               Objects.equals(response, that.response);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, method, description, query, countQuery, pagination, parameters, response);
    }

    @Override
    public String toString() {
        return "ApiEndpointConfig{" +
               "path='" + path + '\'' +
               ", method='" + method + '\'' +
               ", description='" + description + '\'' +
               ", query='" + query + '\'' +
               ", countQuery='" + countQuery + '\'' +
               ", pagination=" + pagination +
               ", parameters=" + parameters +
               ", response=" + response +
               '}';
    }

    /**
     * Pagination configuration
     */
    public static class PaginationConfig {
        private boolean enabled;
        private int defaultSize;
        private int maxSize;

        // Default constructor
        public PaginationConfig() {}

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getDefaultSize() {
            return defaultSize;
        }

        public void setDefaultSize(int defaultSize) {
            this.defaultSize = defaultSize;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PaginationConfig that = (PaginationConfig) o;
            return enabled == that.enabled &&
                   defaultSize == that.defaultSize &&
                   maxSize == that.maxSize;
        }

        @Override
        public int hashCode() {
            return Objects.hash(enabled, defaultSize, maxSize);
        }

        @Override
        public String toString() {
            return "PaginationConfig{" +
                   "enabled=" + enabled +
                   ", defaultSize=" + defaultSize +
                   ", maxSize=" + maxSize +
                   '}';
        }
    }

    /**
     * Endpoint parameter configuration
     */
    public static class EndpointParameter {
        private String name;
        private String type;
        private boolean required;
        private Object defaultValue;
        private String source; // PATH, QUERY, BODY
        private String description;

        // Default constructor
        public EndpointParameter() {}

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

        public Object getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EndpointParameter that = (EndpointParameter) o;
            return required == that.required &&
                   Objects.equals(name, that.name) &&
                   Objects.equals(type, that.type) &&
                   Objects.equals(defaultValue, that.defaultValue) &&
                   Objects.equals(source, that.source) &&
                   Objects.equals(description, that.description);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, type, required, defaultValue, source, description);
        }

        @Override
        public String toString() {
            return "EndpointParameter{" +
                   "name='" + name + '\'' +
                   ", type='" + type + '\'' +
                   ", required=" + required +
                   ", defaultValue=" + defaultValue +
                   ", source='" + source + '\'' +
                   ", description='" + description + '\'' +
                   '}';
        }
    }

    /**
     * Response configuration
     */
    public static class ResponseConfig {
        private String type; // SINGLE, PAGED, LIST
        private List<ResponseField> fields;

        // Default constructor
        public ResponseConfig() {}

        // Getters and Setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<ResponseField> getFields() {
            return fields;
        }

        public void setFields(List<ResponseField> fields) {
            this.fields = fields;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ResponseConfig that = (ResponseConfig) o;
            return Objects.equals(type, that.type) &&
                   Objects.equals(fields, that.fields);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, fields);
        }

        @Override
        public String toString() {
            return "ResponseConfig{" +
                   "type='" + type + '\'' +
                   ", fields=" + fields +
                   '}';
        }
    }

    /**
     * Response field configuration
     */
    public static class ResponseField {
        private String name;
        private String type;
        private String description;

        // Default constructor
        public ResponseField() {}

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

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ResponseField that = (ResponseField) o;
            return Objects.equals(name, that.name) &&
                   Objects.equals(type, that.type) &&
                   Objects.equals(description, that.description);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, type, description);
        }

        @Override
        public String toString() {
            return "ResponseField{" +
                   "name='" + name + '\'' +
                   ", type='" + type + '\'' +
                   ", description='" + description + '\'' +
                   '}';
        }
    }
}
