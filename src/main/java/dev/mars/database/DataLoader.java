package dev.mars.database;

import dev.mars.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Random;

/**
 * Data loader for populating the database with sample stock trade data
 */
public class DataLoader {
    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);
    
    private final DatabaseManager databaseManager;
    private final AppConfig appConfig;
    private final Random random = new Random();
    
    // Sample data arrays
    private static final String[] SYMBOLS = {
        "AAPL", "GOOGL", "MSFT", "AMZN", "TSLA", "META", "NVDA", "NFLX", 
        "AMD", "INTC", "ORCL", "CRM", "ADBE", "PYPL", "UBER", "SPOT"
    };
    
    private static final String[] EXCHANGES = {
        "NASDAQ", "NYSE", "BATS", "IEX"
    };
    
    private static final String[] TRADE_TYPES = {"BUY", "SELL"};
    
    public DataLoader(DatabaseManager databaseManager, AppConfig appConfig) {
        this.databaseManager = databaseManager;
        this.appConfig = appConfig;
    }
    
    /**
     * Load sample data if configured to do so
     */
    public void loadSampleDataIfNeeded() {
        if (!appConfig.shouldLoadSampleData()) {
            logger.info("Sample data loading is disabled");
            return;
        }
        
        try {
            if (isDataAlreadyLoaded()) {
                logger.info("Sample data already exists, skipping data loading");
                return;
            }
            
            loadSampleData();
            
        } catch (Exception e) {
            logger.error("Failed to load sample data", e);
            throw new RuntimeException("Failed to load sample data", e);
        }
    }
    
    private boolean isDataAlreadyLoaded() throws SQLException {
        String countSql = "SELECT COUNT(*) FROM stock_trades";
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(countSql);
             ResultSet resultSet = statement.executeQuery()) {
            
            if (resultSet.next()) {
                int count = resultSet.getInt(1);
                return count > 0;
            }
            
            return false;
        }
    }
    
    private void loadSampleData() throws SQLException {
        logger.info("Loading {} sample stock trades", appConfig.getSampleDataSize());
        
        String insertSql = """
            INSERT INTO stock_trades (symbol, trade_type, quantity, price, total_value, 
                                    trade_date_time, trader_id, exchange) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(insertSql)) {
            
            connection.setAutoCommit(false);
            
            for (int i = 0; i < appConfig.getSampleDataSize(); i++) {
                // Generate random trade data directly
                String symbol = SYMBOLS[random.nextInt(SYMBOLS.length)];
                String tradeType = TRADE_TYPES[random.nextInt(TRADE_TYPES.length)];
                int quantity = random.nextInt(1000) + 1; // 1 to 1000

                // Generate price between $10 and $500
                BigDecimal price = BigDecimal.valueOf(10 + (random.nextDouble() * 490))
                        .setScale(2, RoundingMode.HALF_UP);

                BigDecimal totalValue = price.multiply(BigDecimal.valueOf(quantity))
                        .setScale(2, RoundingMode.HALF_UP);

                // Generate trade time within last 30 days
                LocalDateTime tradeDateTime = LocalDateTime.now()
                        .minusDays(random.nextInt(30))
                        .minusHours(random.nextInt(24))
                        .minusMinutes(random.nextInt(60));

                String traderId = "TRADER_" + String.format("%04d", random.nextInt(100) + 1);
                String exchange = EXCHANGES[random.nextInt(EXCHANGES.length)];

                statement.setString(1, symbol);
                statement.setString(2, tradeType);
                statement.setInt(3, quantity);
                statement.setBigDecimal(4, price);
                statement.setBigDecimal(5, totalValue);
                statement.setObject(6, tradeDateTime);
                statement.setString(7, traderId);
                statement.setString(8, exchange);
                
                statement.addBatch();
                
                // Execute batch every 50 records
                if ((i + 1) % 50 == 0) {
                    statement.executeBatch();
                }
            }
            
            // Execute remaining batch
            statement.executeBatch();
            connection.commit();
            
            logger.info("Successfully loaded {} sample stock trades", appConfig.getSampleDataSize());
            
        } catch (SQLException e) {
            logger.error("Failed to load sample data", e);
            throw e;
        }
    }
    

}
