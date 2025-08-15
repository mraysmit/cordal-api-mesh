package dev.cordal.integration.postgresql.config;

import dev.cordal.integration.postgresql.container.PostgreSQLContainerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Generates YAML configurations for dual PostgreSQL database integration testing
 * Creates database, query, and endpoint configurations dynamically based on container information
 */
public class DualDatabaseConfigurationGenerator {
    private static final Logger logger = LoggerFactory.getLogger(DualDatabaseConfigurationGenerator.class);
    
    private final PostgreSQLContainerManager containerManager;
    
    public DualDatabaseConfigurationGenerator(PostgreSQLContainerManager containerManager) {
        this.containerManager = containerManager;
    }
    
    /**
     * Generate database configurations for both PostgreSQL containers
     * 
     * @param database1Name Name of the first database
     * @param database2Name Name of the second database
     * @return YAML content for database configurations
     */
    public String generateDatabaseConfiguration(String database1Name, String database2Name) {
        logger.info("Generating database configuration for databases: {} and {}", database1Name, database2Name);
        
        var db1Info = containerManager.getConnectionInfo(database1Name);
        var db2Info = containerManager.getConnectionInfo(database2Name);
        
        StringBuilder yaml = new StringBuilder();
        yaml.append("# Dual PostgreSQL Database Configuration for Integration Testing\n");
        yaml.append("# Generated dynamically from TestContainers\n\n");
        yaml.append("databases:\n");
        
        // First database configuration
        yaml.append("  ").append(database1Name).append(":\n");
        yaml.append("    name: \"").append(database1Name).append("\"\n");
        yaml.append("    description: \"PostgreSQL database 1 for dual-database integration testing\"\n");
        yaml.append("    url: \"").append(db1Info.getConfigurationConnectionString()).append("\"\n");
        yaml.append("    username: \"").append(db1Info.username()).append("\"\n");
        yaml.append("    password: \"").append(db1Info.password()).append("\"\n");
        yaml.append("    driver: \"org.postgresql.Driver\"\n");
        yaml.append("    pool:\n");
        yaml.append("      maximumPoolSize: 10\n");
        yaml.append("      minimumIdle: 2\n");
        yaml.append("      connectionTimeout: 30000\n");
        yaml.append("      idleTimeout: 600000\n");
        yaml.append("      maxLifetime: 1800000\n");
        yaml.append("      leakDetectionThreshold: 60000\n");
        yaml.append("      connectionTestQuery: \"SELECT 1\"\n\n");
        
        // Second database configuration
        yaml.append("  ").append(database2Name).append(":\n");
        yaml.append("    name: \"").append(database2Name).append("\"\n");
        yaml.append("    description: \"PostgreSQL database 2 for dual-database integration testing\"\n");
        yaml.append("    url: \"").append(db2Info.getConfigurationConnectionString()).append("\"\n");
        yaml.append("    username: \"").append(db2Info.username()).append("\"\n");
        yaml.append("    password: \"").append(db2Info.password()).append("\"\n");
        yaml.append("    driver: \"org.postgresql.Driver\"\n");
        yaml.append("    pool:\n");
        yaml.append("      maximumPoolSize: 10\n");
        yaml.append("      minimumIdle: 2\n");
        yaml.append("      connectionTimeout: 30000\n");
        yaml.append("      idleTimeout: 600000\n");
        yaml.append("      maxLifetime: 1800000\n");
        yaml.append("      leakDetectionThreshold: 60000\n");
        yaml.append("      connectionTestQuery: \"SELECT 1\"\n");
        
        String result = yaml.toString();
        logger.debug("Generated database configuration:\n{}", result);
        return result;
    }
    
    /**
     * Generate query configurations for both databases
     * 
     * @param database1Name Name of the first database
     * @param database2Name Name of the second database
     * @return YAML content for query configurations
     */
    public String generateQueryConfiguration(String database1Name, String database2Name) {
        logger.info("Generating query configuration for databases: {} and {}", database1Name, database2Name);
        
        StringBuilder yaml = new StringBuilder();
        yaml.append("# Dual PostgreSQL Query Configuration for Integration Testing\n");
        yaml.append("# Generated dynamically for dual-database testing\n\n");
        yaml.append("queries:\n");
        
        // Generate queries for both databases
        List<String> databases = List.of(database1Name, database2Name);
        
        for (String dbName : databases) {
            String dbPrefix = dbName.replace("-", "_");
            
            // All stock trades query
            yaml.append("  ").append(dbPrefix).append("_stock_trades_all:\n");
            yaml.append("    name: \"").append(dbPrefix).append("_stock_trades_all\"\n");
            yaml.append("    description: \"Get all stock trades from ").append(dbName).append(" with pagination\"\n");
            yaml.append("    database: \"").append(dbName).append("\"\n");
            yaml.append("    sql: |\n");
            yaml.append("      SELECT id, symbol, trade_type, quantity, price, total_value,\n");
            yaml.append("             trade_date_time, trader_id, exchange\n");
            yaml.append("      FROM stock_trades\n");
            yaml.append("      ORDER BY trade_date_time DESC\n");
            yaml.append("      LIMIT ? OFFSET ?\n");
            yaml.append("    parameters:\n");
            yaml.append("      - name: \"limit\"\n");
            yaml.append("        type: \"INTEGER\"\n");
            yaml.append("        required: true\n");
            yaml.append("      - name: \"offset\"\n");
            yaml.append("        type: \"INTEGER\"\n");
            yaml.append("        required: true\n\n");
            
            // Count query
            yaml.append("  ").append(dbPrefix).append("_stock_trades_count:\n");
            yaml.append("    name: \"").append(dbPrefix).append("_stock_trades_count\"\n");
            yaml.append("    description: \"Count all stock trades in ").append(dbName).append("\"\n");
            yaml.append("    database: \"").append(dbName).append("\"\n");
            yaml.append("    sql: \"SELECT COUNT(*) as total FROM stock_trades\"\n");
            yaml.append("    parameters: []\n\n");
            
            // By symbol query
            yaml.append("  ").append(dbPrefix).append("_stock_trades_by_symbol:\n");
            yaml.append("    name: \"").append(dbPrefix).append("_stock_trades_by_symbol\"\n");
            yaml.append("    description: \"Get stock trades by symbol from ").append(dbName).append("\"\n");
            yaml.append("    database: \"").append(dbName).append("\"\n");
            yaml.append("    sql: |\n");
            yaml.append("      SELECT id, symbol, trade_type, quantity, price, total_value,\n");
            yaml.append("             trade_date_time, trader_id, exchange\n");
            yaml.append("      FROM stock_trades\n");
            yaml.append("      WHERE symbol = ?\n");
            yaml.append("      ORDER BY trade_date_time DESC\n");
            yaml.append("      LIMIT ? OFFSET ?\n");
            yaml.append("    parameters:\n");
            yaml.append("      - name: \"symbol\"\n");
            yaml.append("        type: \"STRING\"\n");
            yaml.append("        required: true\n");
            yaml.append("      - name: \"limit\"\n");
            yaml.append("        type: \"INTEGER\"\n");
            yaml.append("        required: true\n");
            yaml.append("      - name: \"offset\"\n");
            yaml.append("        type: \"INTEGER\"\n");
            yaml.append("        required: true\n\n");

            // Count by symbol query
            yaml.append("  ").append(dbPrefix).append("_stock_trades_count_by_symbol:\n");
            yaml.append("    name: \"").append(dbPrefix).append("_stock_trades_count_by_symbol\"\n");
            yaml.append("    description: \"Count stock trades by symbol from ").append(dbName).append("\"\n");
            yaml.append("    database: \"").append(dbName).append("\"\n");
            yaml.append("    sql: \"SELECT COUNT(*) as total FROM stock_trades WHERE symbol = ?\"\n");
            yaml.append("    parameters:\n");
            yaml.append("      - name: \"symbol\"\n");
            yaml.append("        type: \"STRING\"\n");
            yaml.append("        required: true\n\n");

            // By trader query
            yaml.append("  ").append(dbPrefix).append("_stock_trades_by_trader:\n");
            yaml.append("    name: \"").append(dbPrefix).append("_stock_trades_by_trader\"\n");
            yaml.append("    description: \"Get stock trades by trader from ").append(dbName).append("\"\n");
            yaml.append("    database: \"").append(dbName).append("\"\n");
            yaml.append("    sql: |\n");
            yaml.append("      SELECT id, symbol, trade_type, quantity, price, total_value,\n");
            yaml.append("             trade_date_time, trader_id, exchange\n");
            yaml.append("      FROM stock_trades\n");
            yaml.append("      WHERE trader_id = ?\n");
            yaml.append("      ORDER BY trade_date_time DESC\n");
            yaml.append("      LIMIT ? OFFSET ?\n");
            yaml.append("    parameters:\n");
            yaml.append("      - name: \"trader_id\"\n");
            yaml.append("        type: \"STRING\"\n");
            yaml.append("        required: true\n");
            yaml.append("      - name: \"limit\"\n");
            yaml.append("        type: \"INTEGER\"\n");
            yaml.append("        required: true\n");
            yaml.append("      - name: \"offset\"\n");
            yaml.append("        type: \"INTEGER\"\n");
            yaml.append("        required: true\n\n");

            // Count by trader query
            yaml.append("  ").append(dbPrefix).append("_stock_trades_count_by_trader:\n");
            yaml.append("    name: \"").append(dbPrefix).append("_stock_trades_count_by_trader\"\n");
            yaml.append("    description: \"Count stock trades by trader from ").append(dbName).append("\"\n");
            yaml.append("    database: \"").append(dbName).append("\"\n");
            yaml.append("    sql: \"SELECT COUNT(*) as total FROM stock_trades WHERE trader_id = ?\"\n");
            yaml.append("    parameters:\n");
            yaml.append("      - name: \"trader_id\"\n");
            yaml.append("        type: \"STRING\"\n");
            yaml.append("        required: true\n\n");
        }
        
        String result = yaml.toString();
        logger.debug("Generated query configuration with {} queries", databases.size() * 6);
        return result;
    }

    /**
     * Generate API endpoint configurations for both databases
     *
     * @param database1Name Name of the first database
     * @param database2Name Name of the second database
     * @return YAML content for endpoint configurations
     */
    public String generateEndpointConfiguration(String database1Name, String database2Name) {
        logger.info("Generating endpoint configuration for databases: {} and {}", database1Name, database2Name);

        StringBuilder yaml = new StringBuilder();
        yaml.append("# Dual PostgreSQL API Endpoints Configuration for Integration Testing\n");
        yaml.append("# Generated dynamically for dual-database testing\n\n");
        yaml.append("endpoints:\n");

        // Generate endpoints for both databases
        List<String> databases = List.of(database1Name, database2Name);

        for (String dbName : databases) {
            String dbPrefix = dbName.replace("-", "_");
            String pathPrefix = dbName.replace("_", "-");

            // All stock trades endpoint
            yaml.append("  ").append(dbPrefix).append("_stock_trades_list:\n");
            yaml.append("    path: \"/api/").append(pathPrefix).append("/stock-trades\"\n");
            yaml.append("    method: \"GET\"\n");
            yaml.append("    description: \"Get all stock trades from ").append(dbName).append(" with pagination\"\n");
            yaml.append("    query: \"").append(dbPrefix).append("_stock_trades_all\"\n");
            yaml.append("    countQuery: \"").append(dbPrefix).append("_stock_trades_count\"\n");
            yaml.append("    pagination:\n");
            yaml.append("      enabled: true\n");
            yaml.append("      defaultSize: 20\n");
            yaml.append("      maxSize: 100\n");
            yaml.append("    parameters:\n");
            yaml.append("      - name: \"page\"\n");
            yaml.append("        type: \"INTEGER\"\n");
            yaml.append("        required: false\n");
            yaml.append("        defaultValue: 0\n");
            yaml.append("        description: \"Page number (0-based)\"\n");
            yaml.append("      - name: \"size\"\n");
            yaml.append("        type: \"INTEGER\"\n");
            yaml.append("        required: false\n");
            yaml.append("        defaultValue: 20\n");
            yaml.append("        description: \"Page size\"\n");
            yaml.append("    response:\n");
            yaml.append("      type: \"PAGED\"\n\n");

            // By symbol endpoint
            yaml.append("  ").append(dbPrefix).append("_stock_trades_by_symbol:\n");
            yaml.append("    path: \"/api/").append(pathPrefix).append("/stock-trades/symbol/{symbol}\"\n");
            yaml.append("    method: \"GET\"\n");
            yaml.append("    description: \"Get stock trades by symbol from ").append(dbName).append("\"\n");
            yaml.append("    query: \"").append(dbPrefix).append("_stock_trades_by_symbol\"\n");
            yaml.append("    countQuery: \"").append(dbPrefix).append("_stock_trades_count_by_symbol\"\n");
            yaml.append("    pagination:\n");
            yaml.append("      enabled: true\n");
            yaml.append("      defaultSize: 20\n");
            yaml.append("      maxSize: 100\n");
            yaml.append("    parameters:\n");
            yaml.append("      - name: \"symbol\"\n");
            yaml.append("        type: \"STRING\"\n");
            yaml.append("        source: \"PATH\"\n");
            yaml.append("        required: true\n");
            yaml.append("        description: \"Stock symbol\"\n");
            yaml.append("      - name: \"page\"\n");
            yaml.append("        type: \"INTEGER\"\n");
            yaml.append("        required: false\n");
            yaml.append("        defaultValue: 0\n");
            yaml.append("      - name: \"size\"\n");
            yaml.append("        type: \"INTEGER\"\n");
            yaml.append("        required: false\n");
            yaml.append("        defaultValue: 20\n");
            yaml.append("    response:\n");
            yaml.append("      type: \"PAGED\"\n\n");

            // By trader endpoint
            yaml.append("  ").append(dbPrefix).append("_stock_trades_by_trader:\n");
            yaml.append("    path: \"/api/").append(pathPrefix).append("/stock-trades/trader/{trader_id}\"\n");
            yaml.append("    method: \"GET\"\n");
            yaml.append("    description: \"Get stock trades by trader from ").append(dbName).append("\"\n");
            yaml.append("    query: \"").append(dbPrefix).append("_stock_trades_by_trader\"\n");
            yaml.append("    countQuery: \"").append(dbPrefix).append("_stock_trades_count_by_trader\"\n");
            yaml.append("    pagination:\n");
            yaml.append("      enabled: true\n");
            yaml.append("      defaultSize: 20\n");
            yaml.append("      maxSize: 100\n");
            yaml.append("    parameters:\n");
            yaml.append("      - name: \"trader_id\"\n");
            yaml.append("        type: \"STRING\"\n");
            yaml.append("        source: \"PATH\"\n");
            yaml.append("        required: true\n");
            yaml.append("        description: \"Trader ID\"\n");
            yaml.append("      - name: \"page\"\n");
            yaml.append("        type: \"INTEGER\"\n");
            yaml.append("        required: false\n");
            yaml.append("        defaultValue: 0\n");
            yaml.append("      - name: \"size\"\n");
            yaml.append("        type: \"INTEGER\"\n");
            yaml.append("        required: false\n");
            yaml.append("        defaultValue: 20\n");
            yaml.append("    response:\n");
            yaml.append("      type: \"PAGED\"\n\n");
        }

        String result = yaml.toString();
        logger.debug("Generated endpoint configuration with {} endpoints", databases.size() * 3);
        return result;
    }

    /**
     * Generate a complete configuration set for dual database testing
     *
     * @param database1Name Name of the first database
     * @param database2Name Name of the second database
     * @return Map containing all configuration types
     */
    public Map<String, String> generateCompleteConfiguration(String database1Name, String database2Name) {
        logger.info("Generating complete configuration set for databases: {} and {}", database1Name, database2Name);

        return Map.of(
            "databases", generateDatabaseConfiguration(database1Name, database2Name),
            "queries", generateQueryConfiguration(database1Name, database2Name),
            "endpoints", generateEndpointConfiguration(database1Name, database2Name)
        );
    }
}
