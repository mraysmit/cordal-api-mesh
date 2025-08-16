package dev.cordal.common.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluates conditions for cache invalidation rules
 * Supports simple expressions like "symbol = ${event.symbol}" or "volume > 1000"
 */
public class ConditionEvaluator {
    private static final Logger logger = LoggerFactory.getLogger(ConditionEvaluator.class);
    
    // Pattern to match variable substitutions like ${event.symbol}
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    
    // Pattern to match simple conditions like "field = value" or "field > value"
    // Note: Order matters - longer operators (>=, <=, !=) must come before shorter ones (>, <, =)
    private static final Pattern CONDITION_PATTERN = Pattern.compile("^\\s*([^\\s=!<>]+)\\s*(>=|<=|!=|>|<|=)\\s*(.+?)\\s*$");

    /**
     * Evaluate a condition against an event
     * 
     * @param condition the condition string to evaluate
     * @param event the event to evaluate against
     * @return true if the condition is met, false otherwise
     */
    public static boolean evaluate(String condition, CacheEvent event) {
        if (condition == null || condition.trim().isEmpty()) {
            return true; // No condition means always true
        }

        try {
            // First, substitute variables in the condition
            String substitutedCondition = substituteVariables(condition, event);

            // Then evaluate the condition
            return evaluateCondition(substitutedCondition, event);
        } catch (Exception e) {
            logger.warn("Failed to evaluate condition '{}' for event {}: {}",
                       condition, event.getEventType(), e.getMessage());
            return false; // Default to false on evaluation error
        }
    }

    /**
     * Substitute variables in a condition string with values from the event
     * 
     * @param condition the condition with variables
     * @param event the event containing the data
     * @return the condition with variables substituted
     */
    private static String substituteVariables(String condition, CacheEvent event) {
        Matcher matcher = VARIABLE_PATTERN.matcher(condition);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String variable = matcher.group(1);
            Object value = getVariableValue(variable, event);
            String replacement = value != null ? value.toString() : "null";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }

    /**
     * Get the value of a variable from the event
     * Supports dot notation like "event.symbol" or "data.volume"
     * 
     * @param variable the variable name
     * @param event the event
     * @return the variable value, or null if not found
     */
    private static Object getVariableValue(String variable, CacheEvent event) {
        if (variable.startsWith("event.")) {
            String key = variable.substring(6); // Remove "event." prefix
            return event.getValue(key);
        } else if (variable.startsWith("data.")) {
            String key = variable.substring(5); // Remove "data." prefix
            return event.getValue(key);
        } else {
            // Direct key lookup
            return event.getValue(variable);
        }
    }

    /**
     * Evaluate a simple condition like "symbol = AAPL" or "volume > 1000"
     * The left operand can be a field name from the event, the right operand is the value to compare
     *
     * @param condition the condition to evaluate
     * @param event the event containing the data
     * @return true if the condition is met, false otherwise
     */
    private static boolean evaluateCondition(String condition, CacheEvent event) {
        Matcher matcher = CONDITION_PATTERN.matcher(condition);
        if (!matcher.matches()) {
            logger.warn("Invalid condition format: {}", condition);
            return false;
        }

        String leftOperand = matcher.group(1).trim();
        String operator = matcher.group(2).trim();
        String rightOperand = matcher.group(3).trim();

        // Get the actual value for the left operand from the event
        Object leftValue = event.getValue(leftOperand);
        String leftStr = leftValue != null ? leftValue.toString() : "null";

        // Remove quotes if present from right operand
        rightOperand = removeQuotes(rightOperand);



        return evaluateComparison(leftStr, operator, rightOperand);
    }

    /**
     * Remove surrounding quotes from a string
     */
    private static String removeQuotes(String value) {
        if (value.length() >= 2 && 
            ((value.startsWith("\"") && value.endsWith("\"")) ||
             (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    /**
     * Evaluate a comparison between two operands
     */
    private static boolean evaluateComparison(String left, String operator, String right) {
        switch (operator) {
            case "=":
            case "==":
                return compareEqual(left, right);
            case "!=":
                return !compareEqual(left, right);
            case ">":
                return compareNumeric(left, right) > 0;
            case "<":
                return compareNumeric(left, right) < 0;
            case ">=":
                return compareNumeric(left, right) >= 0;
            case "<=":
                return compareNumeric(left, right) <= 0;
            default:
                logger.warn("Unsupported operator: {}", operator);
                return false;
        }
    }

    /**
     * Compare two values for equality
     */
    private static boolean compareEqual(String left, String right) {
        if ("null".equals(left) || "null".equals(right)) {
            return "null".equals(left) && "null".equals(right);
        }
        return left.equals(right);
    }

    /**
     * Compare two values numerically
     * Returns: negative if left < right, 0 if equal, positive if left > right
     */
    private static int compareNumeric(String left, String right) {
        try {
            double leftNum = Double.parseDouble(left);
            double rightNum = Double.parseDouble(right);
            return Double.compare(leftNum, rightNum);
        } catch (NumberFormatException e) {
            // Fall back to string comparison
            return left.compareTo(right);
        }
    }
}
