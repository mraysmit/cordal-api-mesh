package dev.cordal.common.cache;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CacheKeyBuilder
 */
class CacheKeyBuilderTest {

    @Test
    void testBuildKeyWithPattern() {
        String queryName = "get_stock_trades";
        String pattern = "stock_trades:{symbol}:{limit}";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("symbol", "AAPL");
        parameters.put("limit", 100);

        String key = CacheKeyBuilder.buildKey(queryName, pattern, parameters);
        assertEquals("stock_trades:aapl:100", key);
    }

    @Test
    void testBuildKeyWithoutPattern() {
        String queryName = "get_stock_trades";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("symbol", "AAPL");
        parameters.put("limit", 100);

        String key = CacheKeyBuilder.buildKey(queryName, null, parameters);
        assertTrue(key.startsWith("get_stock_trades:"));
        assertTrue(key.contains("limit=100"));
        assertTrue(key.contains("symbol=aapl"));
    }

    @Test
    void testBuildDefaultKey() {
        String queryName = "test_query";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("param1", "value1");
        parameters.put("param2", 42);

        String key = CacheKeyBuilder.buildDefaultKey(queryName, parameters);
        assertEquals("test_query:param1=value1&param2=42", key);
    }

    @Test
    void testBuildDefaultKeyWithEmptyParameters() {
        String queryName = "test_query";
        Map<String, Object> parameters = new HashMap<>();

        String key = CacheKeyBuilder.buildDefaultKey(queryName, parameters);
        assertEquals("test_query", key);
    }

    @Test
    void testNormalizeValue() {
        // Test through buildDefaultKey since normalizeValue is private
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("string", "  UPPER CASE  ");
        parameters.put("number", 123);
        parameters.put("boolean", true);
        parameters.put("null", null);

        String key = CacheKeyBuilder.buildDefaultKey("test", parameters);
        assertTrue(key.contains("string=upper case"));
        assertTrue(key.contains("number=123"));
        assertTrue(key.contains("boolean=true"));
        assertTrue(key.contains("null=null"));
    }

    @Test
    void testIsValidPattern() {
        assertTrue(CacheKeyBuilder.isValidPattern("simple_pattern"));
        assertTrue(CacheKeyBuilder.isValidPattern("pattern:{param1}:{param2}"));
        assertTrue(CacheKeyBuilder.isValidPattern("{param}"));
        
        assertFalse(CacheKeyBuilder.isValidPattern(null));
        assertFalse(CacheKeyBuilder.isValidPattern(""));
        assertFalse(CacheKeyBuilder.isValidPattern("unmatched{"));
        assertFalse(CacheKeyBuilder.isValidPattern("unmatched}"));
        assertFalse(CacheKeyBuilder.isValidPattern("{unmatched"));
    }

    @Test
    void testExtractParameterNames() {
        Set<String> params = CacheKeyBuilder.extractParameterNames("stock_trades:{symbol}:{limit}");
        assertEquals(2, params.size());
        assertTrue(params.contains("symbol"));
        assertTrue(params.contains("limit"));

        params = CacheKeyBuilder.extractParameterNames("no_params");
        assertTrue(params.isEmpty());

        params = CacheKeyBuilder.extractParameterNames("{single}");
        assertEquals(1, params.size());
        assertTrue(params.contains("single"));
    }

    @Test
    void testLongKeyHashing() {
        String queryName = "test_query";
        String pattern = "very_long_pattern_that_will_exceed_the_maximum_length_limit_and_should_be_hashed_instead_of_used_directly";
        // Create a pattern that will result in a very long key
        StringBuilder longPattern = new StringBuilder(pattern);
        for (int i = 0; i < 10; i++) {
            longPattern.append(":{param").append(i).append("}");
        }
        
        Map<String, Object> parameters = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            parameters.put("param" + i, "very_long_value_that_contributes_to_making_the_key_extremely_long");
        }

        String key = CacheKeyBuilder.buildKey(queryName, longPattern.toString(), parameters);
        
        // Should be hashed and start with query name
        assertTrue(key.startsWith("test_query:"));
        assertTrue(key.length() < 250); // Should be much shorter due to hashing
    }

    @Test
    void testParameterOrdering() {
        String queryName = "test_query";
        Map<String, Object> parameters1 = new HashMap<>();
        parameters1.put("b", "value2");
        parameters1.put("a", "value1");
        parameters1.put("c", "value3");

        Map<String, Object> parameters2 = new HashMap<>();
        parameters2.put("c", "value3");
        parameters2.put("a", "value1");
        parameters2.put("b", "value2");

        String key1 = CacheKeyBuilder.buildDefaultKey(queryName, parameters1);
        String key2 = CacheKeyBuilder.buildDefaultKey(queryName, parameters2);
        
        // Keys should be identical regardless of parameter insertion order
        assertEquals(key1, key2);
    }
}
