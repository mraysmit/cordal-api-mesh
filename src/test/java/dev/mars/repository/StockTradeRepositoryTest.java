package dev.mars.repository;

import dev.mars.database.DatabaseManager;
import dev.mars.exception.ApiException;
import dev.mars.model.StockTrade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for StockTradeRepository using real database
 */
public class StockTradeRepositoryTest {

    private StockTradeRepository repository;
    private DatabaseManager databaseManager;
    private LocalDateTime testDateTime;

    @BeforeEach
    void setUp() throws SQLException {
        // Use test configuration
        System.setProperty("config.file", "application-test.yml");

        // Create components manually to avoid Guice module complexity in tests
        var appConfig = new dev.mars.config.AppConfig();
        var databaseConfig = new dev.mars.config.DatabaseConfig(appConfig);
        databaseManager = new DatabaseManager(databaseConfig);
        // Initialize schema explicitly since we're not using the Guice module
        databaseManager.initializeSchema();
        // Clean database before each test
        databaseManager.cleanDatabase();
        repository = new StockTradeRepository(databaseManager);

        testDateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

        // Insert test data
        insertTestData();
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("config.file");
    }

    @Test
    void testFindAll() {
        // Act
        List<StockTrade> result = repository.findAll(0, 10);

        // Assert
        assertThat(result).hasSize(3); // We inserted 3 test records
        assertThat(result.get(0).getSymbol()).isIn("AAPL", "GOOGL", "MSFT");
        assertThat(result.get(0).getTradeType()).isIn("BUY", "SELL");

        // Test pagination
        List<StockTrade> page1 = repository.findAll(0, 2);
        List<StockTrade> page2 = repository.findAll(1, 2);

        assertThat(page1).hasSize(2);
        assertThat(page2).hasSize(1); // Last page with 1 record
    }

    @Test
    void testFindById() {
        // Act
        Optional<StockTrade> result = repository.findById(1L);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getSymbol()).isEqualTo("AAPL");
        assertThat(result.get().getTradeType()).isEqualTo("BUY");
        assertThat(result.get().getQuantity()).isEqualTo(100);
    }

    @Test
    void testFindByIdNotFound() {
        // Act
        Optional<StockTrade> result = repository.findById(999L);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void testFindBySymbol() {
        // Act
        List<StockTrade> result = repository.findBySymbol("AAPL", 0, 5);

        // Assert
        assertThat(result).hasSize(2); // We have 2 AAPL trades in test data
        assertThat(result.get(0).getSymbol()).isEqualTo("AAPL");
        assertThat(result.get(1).getSymbol()).isEqualTo("AAPL");

        // Test case insensitive search
        List<StockTrade> resultLowerCase = repository.findBySymbol("aapl", 0, 5);
        assertThat(resultLowerCase).hasSize(2);
    }

    @Test
    void testFindBySymbolNotFound() {
        // Act
        List<StockTrade> result = repository.findBySymbol("NONEXISTENT", 0, 5);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void testCount() {
        // Act
        long result = repository.count();

        // Assert
        assertThat(result).isEqualTo(3L); // We inserted 3 test records
    }

    @Test
    void testCountBySymbol() {
        // Act
        long result = repository.countBySymbol("AAPL");

        // Assert
        assertThat(result).isEqualTo(2L); // 2 AAPL trades in test data

        // Test non-existent symbol
        long resultNonExistent = repository.countBySymbol("NONEXISTENT");
        assertThat(resultNonExistent).isEqualTo(0L);
    }

    @Test
    void testPaginationBoundaries() {
        // Test first page
        List<StockTrade> firstPage = repository.findAll(0, 2);
        assertThat(firstPage).hasSize(2);

        // Test last page
        List<StockTrade> lastPage = repository.findAll(1, 2);
        assertThat(lastPage).hasSize(1);

        // Test beyond available data
        List<StockTrade> emptyPage = repository.findAll(10, 10);
        assertThat(emptyPage).isEmpty();
    }

    @Test
    void testSymbolCaseHandling() {
        // Test that lowercase symbol is handled correctly
        List<StockTrade> result = repository.findBySymbol("aapl", 0, 10);
        assertThat(result).hasSize(2); // Should find AAPL trades

        long count = repository.countBySymbol("aapl");
        assertThat(count).isEqualTo(2L);
    }

    @Test
    void testDataIntegrity() {
        // Verify that all inserted data is correctly stored and retrieved
        List<StockTrade> allTrades = repository.findAll(0, 10);

        // Check that we have the expected trades
        boolean hasAppleBuy = allTrades.stream()
            .anyMatch(t -> "AAPL".equals(t.getSymbol()) && "BUY".equals(t.getTradeType()));
        boolean hasAppleSell = allTrades.stream()
            .anyMatch(t -> "AAPL".equals(t.getSymbol()) && "SELL".equals(t.getTradeType()));
        boolean hasGoogleSell = allTrades.stream()
            .anyMatch(t -> "GOOGL".equals(t.getSymbol()) && "SELL".equals(t.getTradeType()));

        assertThat(hasAppleBuy).isTrue();
        assertThat(hasAppleSell).isTrue();
        assertThat(hasGoogleSell).isTrue();
    }

    private void insertTestData() throws SQLException {
        String insertSql = """
            INSERT INTO stock_trades (symbol, trade_type, quantity, price, total_value,
                                    trade_date_time, trader_id, exchange)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(insertSql)) {

            // Trade 1 - AAPL BUY
            statement.setString(1, "AAPL");
            statement.setString(2, "BUY");
            statement.setInt(3, 100);
            statement.setBigDecimal(4, BigDecimal.valueOf(150.50));
            statement.setBigDecimal(5, BigDecimal.valueOf(15050.00));
            statement.setObject(6, testDateTime);
            statement.setString(7, "TRADER_001");
            statement.setString(8, "NASDAQ");
            statement.addBatch();

            // Trade 2 - GOOGL SELL
            statement.setString(1, "GOOGL");
            statement.setString(2, "SELL");
            statement.setInt(3, 50);
            statement.setBigDecimal(4, BigDecimal.valueOf(2500.75));
            statement.setBigDecimal(5, BigDecimal.valueOf(125037.50));
            statement.setObject(6, testDateTime.minusHours(1));
            statement.setString(7, "TRADER_002");
            statement.setString(8, "NYSE");
            statement.addBatch();

            // Trade 3 - AAPL SELL
            statement.setString(1, "AAPL");
            statement.setString(2, "SELL");
            statement.setInt(3, 75);
            statement.setBigDecimal(4, BigDecimal.valueOf(151.25));
            statement.setBigDecimal(5, BigDecimal.valueOf(11343.75));
            statement.setObject(6, testDateTime.minusHours(2));
            statement.setString(7, "TRADER_003");
            statement.setString(8, "NASDAQ");
            statement.addBatch();

            statement.executeBatch();
        }
    }
}
