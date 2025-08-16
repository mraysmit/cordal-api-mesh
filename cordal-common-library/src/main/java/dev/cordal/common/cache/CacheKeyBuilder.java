package dev.cordal.common.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for building consistent cache keys from query parameters and patterns
 */
public class CacheKeyBuilder {
    private static final Logger logger = LoggerFactory.getLogger(CacheKeyBuilder.class);
    private static final String DEFAULT_SEPARATOR = ":";
    private static final String HASH_ALGORITHM = "SHA-256";
    
    /**
     * Build a cache key using a pattern and parameters
     * 
     * @param queryName the name of the query
     * @param keyPattern the pattern for the key (e.g., "stock_trades:{symbol}:{limit}")
     * @param parameters the parameters to substitute in the pattern
     * @return the generated cache key
     */
    public static String buildKey(String queryName, String keyPattern, Map<String, Object> parameters) {
        if (queryName == null || queryName.trim().isEmpty()) {
            throw new IllegalArgumentException("Query name cannot be null or empty");
        }
        
        if (keyPattern == null || keyPattern.trim().isEmpty()) {
            // If no pattern provided, generate a default key
            return buildDefaultKey(queryName, parameters);
        }
        
        try {
            String key = substituteParameters(keyPattern, parameters);
            
            // Ensure the key is not too long (some cache systems have key length limits)
            if (key.length() > 250) {
                logger.debug("Cache key too long ({}), using hash: {}", key.length(), key);
                return queryName + DEFAULT_SEPARATOR + hashString(key);
            }
            
            return key;
        } catch (Exception e) {
            logger.warn("Failed to build cache key with pattern '{}', falling back to default", keyPattern, e);
            return buildDefaultKey(queryName, parameters);
        }
    }
    
    /**
     * Build a default cache key when no pattern is provided
     * 
     * @param queryName the name of the query
     * @param parameters the parameters
     * @return the generated cache key
     */
    public static String buildDefaultKey(String queryName, Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return queryName;
        }
        
        // Sort parameters by key for consistency
        String paramString = parameters.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> entry.getKey() + "=" + normalizeValue(entry.getValue()))
            .collect(Collectors.joining("&"));
        
        String key = queryName + DEFAULT_SEPARATOR + paramString;
        
        // Hash if too long
        if (key.length() > 250) {
            return queryName + DEFAULT_SEPARATOR + hashString(paramString);
        }
        
        return key;
    }
    
    /**
     * Substitute parameters in a key pattern
     * 
     * @param pattern the pattern with placeholders like {paramName}
     * @param parameters the parameters to substitute
     * @return the pattern with parameters substituted
     */
    private static String substituteParameters(String pattern, Map<String, Object> parameters) {
        String result = pattern;
        
        if (parameters != null) {
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                String placeholder = "{" + entry.getKey() + "}";
                String value = normalizeValue(entry.getValue());
                result = result.replace(placeholder, value);
            }
        }
        
        // Check for unresolved placeholders
        if (result.contains("{") && result.contains("}")) {
            logger.warn("Cache key pattern contains unresolved placeholders: {}", result);
        }
        
        return result;
    }
    
    /**
     * Normalize a parameter value for use in cache keys
     * 
     * @param value the value to normalize
     * @return the normalized string representation
     */
    private static String normalizeValue(Object value) {
        if (value == null) {
            return "null";
        }
        
        if (value instanceof String) {
            // Trim and convert to lowercase for consistency
            return ((String) value).trim().toLowerCase();
        }
        
        if (value instanceof Number) {
            return value.toString();
        }
        
        if (value instanceof Boolean) {
            return value.toString().toLowerCase();
        }
        
        if (value instanceof Collection) {
            Collection<?> collection = (Collection<?>) value;
            return collection.stream()
                .map(CacheKeyBuilder::normalizeValue)
                .sorted()
                .collect(Collectors.joining(",", "[", "]"));
        }
        
        if (value.getClass().isArray()) {
            Object[] array = (Object[]) value;
            return Arrays.stream(array)
                .map(CacheKeyBuilder::normalizeValue)
                .sorted()
                .collect(Collectors.joining(",", "[", "]"));
        }
        
        // For other objects, use toString and normalize
        return value.toString().trim().toLowerCase();
    }
    
    /**
     * Generate a hash of a string for use in cache keys
     * 
     * @param input the string to hash
     * @return the hash as a hex string
     */
    private static String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = digest.digest(input.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            // Return first 16 characters of hash for reasonable key length
            return hexString.substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Hash algorithm not available: {}", HASH_ALGORITHM, e);
            // Fallback to simple hash
            return String.valueOf(Math.abs(input.hashCode()));
        }
    }
    
    /**
     * Validate a cache key pattern
     * 
     * @param pattern the pattern to validate
     * @return true if the pattern is valid, false otherwise
     */
    public static boolean isValidPattern(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return false;
        }
        
        // Check for balanced braces
        int openBraces = 0;
        for (char c : pattern.toCharArray()) {
            if (c == '{') {
                openBraces++;
            } else if (c == '}') {
                openBraces--;
                if (openBraces < 0) {
                    return false; // Unmatched closing brace
                }
            }
        }
        
        return openBraces == 0; // All braces should be matched
    }
    
    /**
     * Extract parameter names from a cache key pattern
     * 
     * @param pattern the pattern to analyze
     * @return set of parameter names found in the pattern
     */
    public static Set<String> extractParameterNames(String pattern) {
        Set<String> paramNames = new HashSet<>();
        
        if (pattern == null) {
            return paramNames;
        }
        
        int start = 0;
        while ((start = pattern.indexOf('{', start)) != -1) {
            int end = pattern.indexOf('}', start);
            if (end != -1) {
                String paramName = pattern.substring(start + 1, end);
                paramNames.add(paramName);
                start = end + 1;
            } else {
                break; // Unmatched opening brace
            }
        }
        
        return paramNames;
    }
}
