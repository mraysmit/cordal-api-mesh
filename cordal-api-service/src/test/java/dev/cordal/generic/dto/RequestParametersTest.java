package dev.cordal.generic.dto;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for RequestParameters DTO
 * Ensures type safety and proper validation for API request parameters
 */
class RequestParametersTest {

    @Test
    void shouldCreateEmptyRequestParameters() {
        RequestParameters params = new RequestParameters();
        
        assertThat(params.isEmpty()).isTrue();
        assertThat(params.size()).isEqualTo(0);
        assertThat(params.getParameterNames()).isEmpty();
    }

    @Test
    void shouldCreateRequestParametersFromMap() {
        Map<String, Object> data = new HashMap<>();
        data.put("page", 1);
        data.put("size", 10);
        data.put("filter", "active");
        
        RequestParameters params = new RequestParameters(data);
        
        assertThat(params.isEmpty()).isFalse();
        assertThat(params.size()).isEqualTo(3);
        assertThat(params.getParameterNames()).containsExactlyInAnyOrder("page", "size", "filter");
    }

    @Test
    void shouldAddParameters() {
        RequestParameters params = new RequestParameters()
            .add("page", 1)
            .add("size", 10)
            .add("filter", "active");
        
        assertThat(params.size()).isEqualTo(3);
        assertThat(params.hasParameter("page")).isTrue();
        assertThat(params.hasParameter("size")).isTrue();
        assertThat(params.hasParameter("filter")).isTrue();
    }

    @Test
    void shouldGetStringParametersWithNullSafety() {
        RequestParameters params = new RequestParameters()
            .add("name", "John Doe")
            .add("nullValue", null)
            .add("number", 123);
        
        assertThat(params.getString("name")).hasValue("John Doe");
        assertThat(params.getString("nullValue")).isEmpty();
        assertThat(params.getString("number")).hasValue("123"); // Number converted to string
        assertThat(params.getString("missing")).isEmpty();
        
        assertThat(params.getString("name", "default")).isEqualTo("John Doe");
        assertThat(params.getString("missing", "default")).isEqualTo("default");
    }

    @Test
    void shouldGetRequiredStringParameters() {
        RequestParameters params = new RequestParameters()
            .add("name", "John Doe");
        
        assertThat(params.getRequiredString("name")).isEqualTo("John Doe");
        
        assertThatThrownBy(() -> params.getRequiredString("missing"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Required parameter 'missing' is missing");
    }

    @Test
    void shouldGetIntegerParametersWithValidation() {
        RequestParameters params = new RequestParameters()
            .add("intValue", 123)
            .add("longValue", 456L)
            .add("doubleValue", 78.9)
            .add("stringNumber", "999")
            .add("invalidString", "not-a-number")
            .add("nullValue", null);
        
        assertThat(params.getInteger("intValue")).hasValue(123);
        assertThat(params.getInteger("longValue")).hasValue(456);
        assertThat(params.getInteger("doubleValue")).hasValue(78);
        assertThat(params.getInteger("stringNumber")).hasValue(999);
        assertThat(params.getInteger("nullValue")).isEmpty();
        assertThat(params.getInteger("missing")).isEmpty();
        
        assertThatThrownBy(() -> params.getInteger("invalidString"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Parameter 'invalidString' must be a valid integer");
        
        assertThat(params.getInteger("missing", 42)).isEqualTo(42);
    }

    @Test
    void shouldGetRequiredIntegerParameters() {
        RequestParameters params = new RequestParameters()
            .add("page", 1);
        
        assertThat(params.getRequiredInteger("page")).isEqualTo(1);
        
        assertThatThrownBy(() -> params.getRequiredInteger("missing"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Required integer parameter 'missing' is missing");
    }

    @Test
    void shouldGetLongParametersWithValidation() {
        RequestParameters params = new RequestParameters()
            .add("longValue", 123456789L)
            .add("intValue", 123)
            .add("stringNumber", "999888777")
            .add("invalidString", "not-a-number");
        
        assertThat(params.getLong("longValue")).hasValue(123456789L);
        assertThat(params.getLong("intValue")).hasValue(123L);
        assertThat(params.getLong("stringNumber")).hasValue(999888777L);
        
        assertThatThrownBy(() -> params.getLong("invalidString"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Parameter 'invalidString' must be a valid long");
        
        assertThat(params.getLong("missing", 42L)).isEqualTo(42L);
    }

    @Test
    void shouldGetDoubleParametersWithValidation() {
        RequestParameters params = new RequestParameters()
            .add("doubleValue", 123.45)
            .add("intValue", 123)
            .add("stringNumber", "456.78")
            .add("invalidString", "not-a-number");
        
        assertThat(params.getDouble("doubleValue")).hasValue(123.45);
        assertThat(params.getDouble("intValue")).hasValue(123.0);
        assertThat(params.getDouble("stringNumber")).hasValue(456.78);
        
        assertThatThrownBy(() -> params.getDouble("invalidString"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Parameter 'invalidString' must be a valid double");
        
        assertThat(params.getDouble("missing", 42.0)).isEqualTo(42.0);
    }

    @Test
    void shouldGetBooleanParameters() {
        RequestParameters params = new RequestParameters()
            .add("trueValue", true)
            .add("falseValue", false)
            .add("stringTrue", "true")
            .add("stringFalse", "false")
            .add("stringOne", "1")
            .add("stringYes", "yes")
            .add("otherString", "maybe");
        
        assertThat(params.getBoolean("trueValue")).hasValue(true);
        assertThat(params.getBoolean("falseValue")).hasValue(false);
        assertThat(params.getBoolean("stringTrue")).hasValue(true);
        assertThat(params.getBoolean("stringFalse")).hasValue(false);
        assertThat(params.getBoolean("stringOne")).hasValue(true);
        assertThat(params.getBoolean("stringYes")).hasValue(true);
        assertThat(params.getBoolean("otherString")).hasValue(false);
        assertThat(params.getBoolean("missing", true)).isEqualTo(true);
    }

    @Test
    void shouldValidateRequiredParameters() {
        RequestParameters params = new RequestParameters()
            .add("page", 1)
            .add("size", 10);
        
        // Should not throw for existing parameters
        params.validateRequired("page", "size");
        
        // Should throw for missing parameters
        assertThatThrownBy(() -> params.validateRequired("page", "size", "missing"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Required parameter 'missing' is missing");
    }

    @Test
    void shouldValidateIntegerRange() {
        RequestParameters params = new RequestParameters()
            .add("page", 5)
            .add("size", 50);
        
        // Should not throw for values in range
        params.validateIntegerRange("page", 0, 10);
        params.validateIntegerRange("size", 1, 100);
        
        // Should throw for values out of range
        assertThatThrownBy(() -> params.validateIntegerRange("size", 1, 20))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Parameter 'size' must be between 1 and 20, got: 50");
        
        // Should not throw for missing parameters (validation is optional)
        params.validateIntegerRange("missing", 0, 10);
    }

    @Test
    void shouldConvertToMap() {
        RequestParameters params = new RequestParameters()
            .add("page", 1)
            .add("size", 10)
            .add("filter", "active");
        
        Map<String, Object> map = params.asMap();
        
        assertThat(map).hasSize(3);
        assertThat(map).containsEntry("page", 1);
        assertThat(map).containsEntry("size", 10);
        assertThat(map).containsEntry("filter", "active");
        
        // Should be a defensive copy
        map.put("newKey", "newValue");
        assertThat(params.hasParameter("newKey")).isFalse();
    }

    @Test
    void shouldCreateDefensiveCopy() {
        Map<String, Object> originalMap = new HashMap<>();
        originalMap.put("key", "value");
        
        RequestParameters params = new RequestParameters(originalMap);
        
        // Modify original map
        originalMap.put("newKey", "newValue");
        
        // RequestParameters should not be affected
        assertThat(params.hasParameter("newKey")).isFalse();
        assertThat(params.size()).isEqualTo(1);
    }

    @Test
    void shouldGetRawParameterValue() {
        RequestParameters params = new RequestParameters()
            .add("complexObject", new java.util.Date());
        
        Object value = params.getParameter("complexObject");
        assertThat(value).isInstanceOf(java.util.Date.class);
        assertThat(params.getParameter("missing")).isNull();
    }

    @Test
    void shouldHaveProperEqualsAndHashCode() {
        Map<String, Object> data1 = Map.of("page", 1, "size", 10);
        Map<String, Object> data2 = Map.of("page", 1, "size", 10);
        Map<String, Object> data3 = Map.of("page", 2, "size", 20);
        
        RequestParameters params1 = new RequestParameters(data1);
        RequestParameters params2 = new RequestParameters(data2);
        RequestParameters params3 = new RequestParameters(data3);
        
        assertThat(params1).isEqualTo(params2);
        assertThat(params1).isNotEqualTo(params3);
        assertThat(params1.hashCode()).isEqualTo(params2.hashCode());
    }

    @Test
    void shouldHaveInformativeToString() {
        RequestParameters params = new RequestParameters()
            .add("page", 1)
            .add("size", 10);
        
        String toString = params.toString();
        assertThat(toString).contains("RequestParameters");
        assertThat(toString).contains("page");
        assertThat(toString).contains("size");
    }
}
