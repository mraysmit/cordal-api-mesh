package dev.cordal.util;

import dev.cordal.generic.config.QueryConfig;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL analysis utilities for configuration validation
 */
public class SqlAnalyzer {

    /**
     * Extract table names from SQL query
     */
    public static Set<String> extractTableNamesFromSql(String sql) {
        Set<String> tables = new HashSet<>();

        // Simple regex to find table names after FROM and JOIN keywords
        Pattern pattern = Pattern.compile("(?i)(?:FROM|JOIN)\\s+([a-zA-Z_][a-zA-Z0-9_]*)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sql);

        while (matcher.find()) {
            String tableName = matcher.group(1).toLowerCase();
            tables.add(tableName);
        }

        return tables;
    }

    /**
     * Extract column names from SQL query for a specific table
     */
    public static Set<String> extractColumnNamesFromSql(String sql, String tableName) {
        Set<String> columns = new HashSet<>();

        // Simple extraction - look for column names in SELECT clause
        Pattern selectPattern = Pattern.compile("(?i)SELECT\\s+(.*?)\\s+FROM", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher selectMatcher = selectPattern.matcher(sql);

        if (selectMatcher.find()) {
            String selectClause = selectMatcher.group(1);

            // Split by comma and extract column names
            String[] parts = selectClause.split(",");
            for (String part : parts) {
                part = part.trim();

                // Handle aliases (column AS alias)
                if (part.toLowerCase().contains(" as ")) {
                    part = part.split("(?i)\\s+as\\s+")[0].trim();
                }

                // Remove table prefixes (table.column)
                if (part.contains(".")) {
                    part = part.substring(part.lastIndexOf(".") + 1);
                }

                // Skip functions and special cases
                if (!part.equals("*") && !part.contains("(") && part.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                    columns.add(part.toLowerCase());
                }
            }
        }

        // Also look for columns in WHERE clause
        Pattern wherePattern = Pattern.compile("(?i)WHERE\\s+(.*?)(?:ORDER|GROUP|LIMIT|$)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher whereMatcher = wherePattern.matcher(sql);

        if (whereMatcher.find()) {
            String whereClause = whereMatcher.group(1);
            Pattern columnPattern = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*[=<>!]", Pattern.CASE_INSENSITIVE);
            Matcher columnMatcher = columnPattern.matcher(whereClause);

            while (columnMatcher.find()) {
                String column = columnMatcher.group(1).toLowerCase();
                if (!column.matches("(?i)(and|or|not|null|true|false)")) {
                    columns.add(column);
                }
            }
        }

        return columns;
    }

    /**
     * Check if a table exists in the database
     */
    public static boolean tableExists(DatabaseMetaData metaData, String tableName) throws SQLException {
        return tableExists(metaData, tableName, null);
    }

    /**
     * Check if a table exists in the database with schema awareness
     */
    public static boolean tableExists(DatabaseMetaData metaData, String tableName, String schemaName) throws SQLException {
        // Try with the specified schema first
        if (schemaName != null && !schemaName.isEmpty()) {
            try (ResultSet tables = metaData.getTables(null, schemaName.toUpperCase(), tableName.toUpperCase(), new String[]{"TABLE"})) {
                if (tables.next()) {
                    return true;
                }
            }

            // Also try with lowercase schema name for case-sensitive databases
            try (ResultSet tables = metaData.getTables(null, schemaName.toLowerCase(), tableName.toLowerCase(), new String[]{"TABLE"})) {
                if (tables.next()) {
                    return true;
                }
            }
        }

        // Fallback: try without schema (original behavior)
        try (ResultSet tables = metaData.getTables(null, null, tableName.toUpperCase(), new String[]{"TABLE"})) {
            if (tables.next()) {
                return true;
            }
        }

        // Also try lowercase for case-sensitive databases
        try (ResultSet tables = metaData.getTables(null, null, tableName.toLowerCase(), new String[]{"TABLE"})) {
            return tables.next();
        }
    }

    /**
     * Validate that columns exist in the specified table
     */
    public static void validateTableColumns(DatabaseMetaData metaData, String tableName, Set<String> referencedColumns,
                                          String queryName, ValidationResult result) {
        validateTableColumns(metaData, tableName, referencedColumns, queryName, result, null);
    }

    /**
     * Validate that columns exist in the specified table with schema awareness
     */
    public static void validateTableColumns(DatabaseMetaData metaData, String tableName, Set<String> referencedColumns,
                                          String queryName, ValidationResult result, String schemaName) {
        try {
            Set<String> existingColumns = new HashSet<>();

            // Try with schema first if provided
            if (schemaName != null && !schemaName.isEmpty()) {
                try (ResultSet columns = metaData.getColumns(null, schemaName.toUpperCase(), tableName.toUpperCase(), null)) {
                    while (columns.next()) {
                        String columnName = columns.getString("COLUMN_NAME").toLowerCase();
                        existingColumns.add(columnName);
                    }
                }

                // If no columns found, try with lowercase schema
                if (existingColumns.isEmpty()) {
                    try (ResultSet columns = metaData.getColumns(null, schemaName.toLowerCase(), tableName.toLowerCase(), null)) {
                        while (columns.next()) {
                            String columnName = columns.getString("COLUMN_NAME").toLowerCase();
                            existingColumns.add(columnName);
                        }
                    }
                }
            }

            // Fallback: try without schema if no columns found yet
            if (existingColumns.isEmpty()) {
                try (ResultSet columns = metaData.getColumns(null, null, tableName.toUpperCase(), null)) {
                    while (columns.next()) {
                        String columnName = columns.getString("COLUMN_NAME").toLowerCase();
                        existingColumns.add(columnName);
                    }
                }

                // Also try lowercase
                if (existingColumns.isEmpty()) {
                    try (ResultSet columns = metaData.getColumns(null, null, tableName.toLowerCase(), null)) {
                        while (columns.next()) {
                            String columnName = columns.getString("COLUMN_NAME").toLowerCase();
                            existingColumns.add(columnName);
                        }
                    }
                }
            }

            String tableDisplayName = schemaName != null ? schemaName + "." + tableName : tableName;

            for (String referencedColumn : referencedColumns) {
                if (existingColumns.contains(referencedColumn)) {
                    result.addSuccess("Query '" + queryName + "' -> table '" + tableDisplayName +
                                    "' -> column '" + referencedColumn + "' [EXISTS]");
                } else {
                    result.addError("Query '" + queryName + "' references non-existent column '" +
                                  referencedColumn + "' in table '" + tableDisplayName + "'");
                }
            }

        } catch (SQLException e) {
            String tableDisplayName = schemaName != null ? schemaName + "." + tableName : tableName;
            result.addError("Error validating columns for table '" + tableDisplayName + "': " + e.getMessage());
        }
    }

    /**
     * Validate query parameters
     */
    public static void validateQueryParameters(QueryConfig query, ValidationResult result) {
        String queryName = query.getName();
        String sql = query.getSql();

        // Count parameter placeholders in SQL
        int sqlParameterCount = 0;
        for (char c : sql.toCharArray()) {
            if (c == '?') {
                sqlParameterCount++;
            }
        }

        // Count defined parameters
        int definedParameterCount = query.getParameters() != null ? query.getParameters().size() : 0;

        if (sqlParameterCount == definedParameterCount) {
            result.addSuccess("Query '" + queryName + "' parameter count matches: " + sqlParameterCount + " parameters");
        } else {
            result.addError("Query '" + queryName + "' parameter mismatch: SQL has " + sqlParameterCount +
                          " placeholders but " + definedParameterCount + " parameters defined");
        }
    }
}
