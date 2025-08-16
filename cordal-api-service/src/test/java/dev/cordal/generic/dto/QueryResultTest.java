package dev.cordal.generic.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for QueryResult DTO
 * Ensures type safety and proper data handling for database query results
 */
class QueryResultTest {

    @Test
    void shouldCreateEmptyQueryResult() {
        QueryResult result = new QueryResult();
        
        assertThat(result.isEmpty()).isTrue();
        assertThat(result.size()).isEqualTo(0);
        assertThat(result.getFieldNames()).isEmpty();
    }

    @Test
    void shouldCreateQueryResultFromMap() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", 123);
        data.put("name", "Test User");
        data.put("active", true);
        data.put("balance", 45.67);
        
        QueryResult result = new QueryResult(data);
        
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.size()).isEqualTo(4);
        assertThat(result.getFieldNames()).containsExactly("id", "name", "active", "balance");
    }

    @Test
    void shouldGetStringValuesWithNullSafety() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", "John Doe");
        data.put("nullValue", null);
        data.put("number", 123);
        
        QueryResult result = new QueryResult(data);
        
        assertThat(result.getString("name")).hasValue("John Doe");
        assertThat(result.getString("nullValue")).isEmpty();
        assertThat(result.getString("number")).hasValue("123"); // Number converted to string
        assertThat(result.getString("missing")).isEmpty();
        
        assertThat(result.getString("name", "default")).isEqualTo("John Doe");
        assertThat(result.getString("missing", "default")).isEqualTo("default");
    }

    @Test
    void shouldGetIntegerValuesWithTypeConversion() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("intValue", 123);
        data.put("longValue", 456L);
        data.put("doubleValue", 78.9);
        data.put("stringNumber", "999");
        data.put("invalidString", "not-a-number");
        data.put("nullValue", null);
        
        QueryResult result = new QueryResult(data);
        
        assertThat(result.getInteger("intValue")).hasValue(123);
        assertThat(result.getInteger("longValue")).hasValue(456);
        assertThat(result.getInteger("doubleValue")).hasValue(78);
        assertThat(result.getInteger("stringNumber")).hasValue(999);
        assertThat(result.getInteger("invalidString")).isEmpty();
        assertThat(result.getInteger("nullValue")).isEmpty();
        assertThat(result.getInteger("missing")).isEmpty();
        
        assertThat(result.getInteger("missing", 42)).isEqualTo(42);
    }

    @Test
    void shouldGetLongValuesWithTypeConversion() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("longValue", 123456789L);
        data.put("intValue", 123);
        data.put("stringNumber", "999888777");
        
        QueryResult result = new QueryResult(data);
        
        assertThat(result.getLong("longValue")).hasValue(123456789L);
        assertThat(result.getLong("intValue")).hasValue(123L);
        assertThat(result.getLong("stringNumber")).hasValue(999888777L);
        assertThat(result.getLong("missing", 42L)).isEqualTo(42L);
    }

    @Test
    void shouldGetDoubleValuesWithTypeConversion() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("doubleValue", 123.45);
        data.put("intValue", 123);
        data.put("stringNumber", "456.78");
        
        QueryResult result = new QueryResult(data);
        
        assertThat(result.getDouble("doubleValue")).hasValue(123.45);
        assertThat(result.getDouble("intValue")).hasValue(123.0);
        assertThat(result.getDouble("stringNumber")).hasValue(456.78);
        assertThat(result.getDouble("missing", 42.0)).isEqualTo(42.0);
    }

    @Test
    void shouldGetBigDecimalValues() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("bigDecimalValue", new BigDecimal("123.456789"));
        data.put("doubleValue", 123.45);
        data.put("stringNumber", "456.789");
        
        QueryResult result = new QueryResult(data);
        
        assertThat(result.getBigDecimal("bigDecimalValue")).hasValue(new BigDecimal("123.456789"));
        assertThat(result.getBigDecimal("doubleValue")).hasValue(BigDecimal.valueOf(123.45));
        assertThat(result.getBigDecimal("stringNumber")).hasValue(new BigDecimal("456.789"));
    }

    @Test
    void shouldGetBooleanValues() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("trueValue", true);
        data.put("falseValue", false);
        data.put("stringTrue", "true");
        data.put("stringFalse", "false");
        data.put("otherString", "maybe");
        
        QueryResult result = new QueryResult(data);
        
        assertThat(result.getBoolean("trueValue")).hasValue(true);
        assertThat(result.getBoolean("falseValue")).hasValue(false);
        assertThat(result.getBoolean("stringTrue")).hasValue(true);
        assertThat(result.getBoolean("stringFalse")).hasValue(false);
        assertThat(result.getBoolean("otherString")).hasValue(false); // Non-"true" strings are false
        assertThat(result.getBoolean("missing", true)).isEqualTo(true);
    }

    @Test
    void shouldGetLocalDateTimeValues() {
        LocalDateTime now = LocalDateTime.now();
        java.sql.Timestamp timestamp = java.sql.Timestamp.valueOf(now);
        
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("localDateTime", now);
        data.put("timestamp", timestamp);
        
        QueryResult result = new QueryResult(data);
        
        assertThat(result.getLocalDateTime("localDateTime")).hasValue(now);
        assertThat(result.getLocalDateTime("timestamp")).hasValue(now);
        assertThat(result.getLocalDateTime("missing")).isEmpty();
    }

    @Test
    void shouldCheckFieldExistence() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("existingField", "value");
        data.put("nullField", null);
        
        QueryResult result = new QueryResult(data);
        
        assertThat(result.hasField("existingField")).isTrue();
        assertThat(result.hasField("nullField")).isTrue(); // null values still count as existing fields
        assertThat(result.hasField("missingField")).isFalse();
    }

    @Test
    void shouldGetRawFieldValue() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("complexObject", new java.util.Date());
        
        QueryResult result = new QueryResult(data);
        
        Object value = result.getField("complexObject");
        assertThat(value).isInstanceOf(java.util.Date.class);
        assertThat(result.getField("missing")).isNull();
    }

    @Test
    void shouldCreateDefensiveCopy() {
        Map<String, Object> originalData = new LinkedHashMap<>();
        originalData.put("key", "value");
        
        QueryResult result = new QueryResult(originalData);
        
        // Modify original map
        originalData.put("newKey", "newValue");
        
        // QueryResult should not be affected
        assertThat(result.hasField("newKey")).isFalse();
        assertThat(result.size()).isEqualTo(1);
    }

    @Test
    void shouldSerializeToJsonCorrectly() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", 123);
        data.put("name", "Test");
        
        QueryResult result = new QueryResult(data);
        
        // The @JsonAnyGetter annotation should make all fields available for JSON serialization
        Map<String, Object> jsonData = result.getData();
        assertThat(jsonData).containsEntry("id", 123);
        assertThat(jsonData).containsEntry("name", "Test");
    }

    @Test
    void shouldHaveProperEqualsAndHashCode() {
        Map<String, Object> data1 = Map.of("id", 123, "name", "Test");
        Map<String, Object> data2 = Map.of("id", 123, "name", "Test");
        Map<String, Object> data3 = Map.of("id", 456, "name", "Other");
        
        QueryResult result1 = new QueryResult(data1);
        QueryResult result2 = new QueryResult(data2);
        QueryResult result3 = new QueryResult(data3);
        
        assertThat(result1).isEqualTo(result2);
        assertThat(result1).isNotEqualTo(result3);
        assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
    }

    @Test
    void shouldHaveInformativeToString() {
        Map<String, Object> data = Map.of("id", 123, "name", "Test");
        QueryResult result = new QueryResult(data);
        
        String toString = result.toString();
        assertThat(toString).contains("QueryResult");
        assertThat(toString).contains("123");
        assertThat(toString).contains("Test");
    }
}
