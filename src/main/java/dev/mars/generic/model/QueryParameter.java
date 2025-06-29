package dev.mars.generic.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/**
 * Represents a parameter for SQL query execution
 */
public class QueryParameter {
    private String name;
    private Object value;
    private String type;
    private int position;

    // Default constructor
    public QueryParameter() {}

    // Constructor with all fields
    public QueryParameter(String name, Object value, String type, int position) {
        this.name = name;
        this.value = value;
        this.type = type;
        this.position = position;
    }

    // Static factory methods
    public static QueryParameter of(String name, Object value, String type, int position) {
        return new QueryParameter(name, value, type, position);
    }

    public static QueryParameter string(String name, String value, int position) {
        return new QueryParameter(name, value, "STRING", position);
    }

    public static QueryParameter integer(String name, Integer value, int position) {
        return new QueryParameter(name, value, "INTEGER", position);
    }

    public static QueryParameter longValue(String name, Long value, int position) {
        return new QueryParameter(name, value, "LONG", position);
    }

    public static QueryParameter decimal(String name, BigDecimal value, int position) {
        return new QueryParameter(name, value, "DECIMAL", position);
    }

    public static QueryParameter timestamp(String name, Timestamp value, int position) {
        return new QueryParameter(name, value, "TIMESTAMP", position);
    }

    public static QueryParameter bool(String name, Boolean value, int position) {
        return new QueryParameter(name, value, "BOOLEAN", position);
    }

    /**
     * Convert string value to appropriate type based on type specification
     */
    public Object getTypedValue() {
        if (value == null) {
            return null;
        }

        if (!(value instanceof String)) {
            return value; // Already typed
        }

        String stringValue = (String) value;
        
        try {
            switch (type.toUpperCase()) {
                case "STRING":
                    return stringValue;
                    
                case "INTEGER":
                    return Integer.valueOf(stringValue);
                    
                case "LONG":
                    return Long.valueOf(stringValue);
                    
                case "DECIMAL":
                    return new BigDecimal(stringValue);
                    
                case "BOOLEAN":
                    return Boolean.valueOf(stringValue);
                    
                case "TIMESTAMP":
                    return parseTimestamp(stringValue);
                    
                default:
                    return stringValue; // Default to string
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Cannot convert value '" + stringValue + "' to type " + type + " for parameter " + name, e);
        }
    }

    /**
     * Parse timestamp from string with multiple format support
     */
    private Timestamp parseTimestamp(String value) {
        // Try different timestamp formats
        String[] formats = {
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd"
        };

        for (String format : formats) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                LocalDateTime dateTime;
                
                if (format.equals("yyyy-MM-dd")) {
                    // For date-only format, set time to start of day
                    dateTime = LocalDateTime.parse(value + " 00:00:00", 
                                                 DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                } else {
                    dateTime = LocalDateTime.parse(value, formatter);
                }
                
                return Timestamp.valueOf(dateTime);
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }

        throw new IllegalArgumentException("Cannot parse timestamp: " + value + 
                                         ". Supported formats: yyyy-MM-dd HH:mm:ss, yyyy-MM-dd'T'HH:mm:ss, yyyy-MM-dd");
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueryParameter that = (QueryParameter) o;
        return position == that.position &&
               Objects.equals(name, that.name) &&
               Objects.equals(value, that.value) &&
               Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value, type, position);
    }

    @Override
    public String toString() {
        return "QueryParameter{" +
               "name='" + name + '\'' +
               ", value=" + value +
               ", type='" + type + '\'' +
               ", position=" + position +
               '}';
    }
}
