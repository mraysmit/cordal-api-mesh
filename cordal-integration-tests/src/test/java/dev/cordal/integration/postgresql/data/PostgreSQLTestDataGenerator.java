package dev.cordal.integration.postgresql.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

/**
 * Generates realistic test data for PostgreSQL stock trades tables
 * Creates varied and realistic stock trade records for integration testing
 */
public class PostgreSQLTestDataGenerator {
    private static final Logger logger = LoggerFactory.getLogger(PostgreSQLTestDataGenerator.class);
    
    // Stock symbols for realistic test data
    private static final List<String> STOCK_SYMBOLS = List.of(
        "AAPL", "GOOGL", "MSFT", "AMZN", "TSLA", "META", "NVDA", "NFLX", 
        "ORCL", "CRM", "ADBE", "INTC", "AMD", "PYPL", "UBER", "SPOT",
        "TWTR", "SNAP", "SQ", "SHOP", "ZM", "DOCU", "OKTA", "SNOW"
    );
    
    // Trader IDs for realistic test data
    private static final List<String> TRADER_IDS = List.of(
        "TRADER001", "TRADER002", "TRADER003", "TRADER004", "TRADER005",
        "TRADER006", "TRADER007", "TRADER008", "TRADER009", "TRADER010",
        "ALGO_TRADER_A", "ALGO_TRADER_B", "ALGO_TRADER_C", "INSTITUTIONAL_001",
        "INSTITUTIONAL_002", "RETAIL_001", "RETAIL_002", "HEDGE_FUND_A"
    );
    
    // Stock exchanges
    private static final List<String> EXCHANGES = List.of(
        "NYSE", "NASDAQ", "AMEX", "BATS", "IEX"
    );
    
    // Trade types
    private static final List<String> TRADE_TYPES = List.of("BUY", "SELL");
    
    private final Random random;
    private final String databaseName;
    
    public PostgreSQLTestDataGenerator(String databaseName) {
        this.databaseName = databaseName;
        this.random = new Random(42); // Fixed seed for reproducible test data
    }
    
    /**
     * Generate and insert sample stock trade data into the database
     * 
     * @param connection Database connection
     * @param recordCount Number of records to generate
     * @throws SQLException if database operation fails
     */
    public void generateAndInsertTestData(Connection connection, int recordCount) throws SQLException {
        logger.info("Generating {} test stock trade records for database: {}", recordCount, databaseName);
        
        String insertSql = """
            INSERT INTO stock_trades (symbol, trade_type, quantity, price, total_value, 
                                    trade_date_time, trader_id, exchange) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
            connection.setAutoCommit(false); // Use transaction for better performance
            
            for (int i = 0; i < recordCount; i++) {
                StockTradeRecord record = generateStockTradeRecord(i);
                
                statement.setString(1, record.symbol());
                statement.setString(2, record.tradeType());
                statement.setInt(3, record.quantity());
                statement.setBigDecimal(4, record.price());
                statement.setBigDecimal(5, record.totalValue());
                statement.setTimestamp(6, record.tradeDateTime());
                statement.setString(7, record.traderId());
                statement.setString(8, record.exchange());
                
                statement.addBatch();
                
                // Execute batch every 50 records for better performance
                if ((i + 1) % 50 == 0) {
                    statement.executeBatch();
                    logger.debug("Inserted batch of 50 records, total: {}", i + 1);
                }
            }
            
            // Execute remaining records
            statement.executeBatch();
            connection.commit();
            
            logger.info("Successfully generated and inserted {} stock trade records for database: {}", 
                       recordCount, databaseName);
            
        } catch (SQLException e) {
            connection.rollback();
            logger.error("Failed to generate test data for database: {}", databaseName, e);
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }
    
    /**
     * Generate a single realistic stock trade record
     */
    private StockTradeRecord generateStockTradeRecord(int index) {
        String symbol = STOCK_SYMBOLS.get(random.nextInt(STOCK_SYMBOLS.size()));
        String tradeType = TRADE_TYPES.get(random.nextInt(TRADE_TYPES.size()));
        String traderId = TRADER_IDS.get(random.nextInt(TRADER_IDS.size()));
        String exchange = EXCHANGES.get(random.nextInt(EXCHANGES.size()));
        
        // Generate realistic quantities (1-10000 shares, with bias toward smaller quantities)
        int quantity = generateRealisticQuantity();
        
        // Generate realistic prices based on symbol (different price ranges for different stocks)
        BigDecimal price = generateRealisticPrice(symbol);
        
        // Calculate total value
        BigDecimal totalValue = price.multiply(BigDecimal.valueOf(quantity))
                                   .setScale(2, RoundingMode.HALF_UP);
        
        // Generate realistic trade date/time (within last 30 days)
        Timestamp tradeDateTime = generateRealisticTradeDateTime();
        
        return new StockTradeRecord(
            symbol, tradeType, quantity, price, totalValue, 
            tradeDateTime, traderId, exchange
        );
    }
    
    /**
     * Generate realistic quantity with bias toward smaller trades
     */
    private int generateRealisticQuantity() {
        double rand = random.nextDouble();
        if (rand < 0.6) {
            // 60% of trades are small (1-100 shares)
            return random.nextInt(100) + 1;
        } else if (rand < 0.85) {
            // 25% of trades are medium (101-1000 shares)
            return random.nextInt(900) + 101;
        } else {
            // 15% of trades are large (1001-10000 shares)
            return random.nextInt(9000) + 1001;
        }
    }
    
    /**
     * Generate realistic prices based on stock symbol
     */
    private BigDecimal generateRealisticPrice(String symbol) {
        // Different price ranges for different types of stocks
        double basePrice = switch (symbol) {
            case "AAPL", "GOOGL", "MSFT", "AMZN" -> 150.0 + (random.nextDouble() * 200.0); // $150-$350
            case "TSLA", "NVDA", "NFLX" -> 200.0 + (random.nextDouble() * 300.0); // $200-$500
            case "META", "ORCL", "CRM" -> 100.0 + (random.nextDouble() * 150.0); // $100-$250
            default -> 20.0 + (random.nextDouble() * 80.0); // $20-$100
        };
        
        return BigDecimal.valueOf(basePrice).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Generate realistic trade date/time within the last 30 days
     */
    private Timestamp generateRealisticTradeDateTime() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);
        
        // Generate random time between 30 days ago and now
        long daysBetween = ChronoUnit.DAYS.between(thirtyDaysAgo, now);
        long randomDays = random.nextLong(daysBetween + 1);
        
        LocalDateTime randomDate = thirtyDaysAgo.plus(randomDays, ChronoUnit.DAYS);
        
        // Add random hours and minutes (trading hours: 9:30 AM - 4:00 PM EST)
        int randomHour = 9 + random.nextInt(7); // 9 AM to 3 PM
        int randomMinute = random.nextInt(60);
        int randomSecond = random.nextInt(60);
        
        randomDate = randomDate.withHour(randomHour)
                              .withMinute(randomMinute)
                              .withSecond(randomSecond)
                              .withNano(0);
        
        return Timestamp.valueOf(randomDate);
    }
    
    /**
     * Record representing a stock trade for test data generation
     */
    public record StockTradeRecord(
        String symbol,
        String tradeType,
        int quantity,
        BigDecimal price,
        BigDecimal totalValue,
        Timestamp tradeDateTime,
        String traderId,
        String exchange
    ) {}
    
    /**
     * Get the list of stock symbols used in test data generation
     */
    public static List<String> getTestStockSymbols() {
        return STOCK_SYMBOLS;
    }
    
    /**
     * Get the list of trader IDs used in test data generation
     */
    public static List<String> getTestTraderIds() {
        return TRADER_IDS;
    }
    
    /**
     * Get the list of exchanges used in test data generation
     */
    public static List<String> getTestExchanges() {
        return EXCHANGES;
    }
}
