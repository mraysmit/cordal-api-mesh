package dev.mars.service;

import dev.mars.database.DatabaseManager;
import dev.mars.dto.PagedResponse;
import dev.mars.dto.StockTradeDto;
import dev.mars.exception.ApiException;
import dev.mars.model.StockTrade;
import dev.mars.repository.StockTradeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for StockTradeService using real components
 */
public class StockTradeServiceTest {

    private StockTradeService stockTradeService;
    private StockTradeRepository stockTradeRepository;
    private DatabaseManager databaseManager;

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
        stockTradeRepository = new StockTradeRepository(databaseManager);
        stockTradeService = new StockTradeService(stockTradeRepository);

        // Insert test data
        insertTestData();
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("config.file");
    }

    @Test
    void testGetAllStockTrades() {
        // Act
        PagedResponse<StockTradeDto> result = stockTradeService.getAllStockTrades(0, 20);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getData()).hasSize(3); // We inserted 3 test records
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(20);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.isHasNext()).isFalse();
        assertThat(result.isHasPrevious()).isFalse();

        // Verify data content
        assertThat(result.getData().get(0).getSymbol()).isIn("AAPL", "GOOGL", "MSFT");
        assertThat(result.getData().get(0).getTradeType()).isIn("BUY", "SELL");
    }

    @Test
    void testGetAllStockTradesAsync() throws ExecutionException, InterruptedException {
        // Act
        CompletableFuture<PagedResponse<StockTradeDto>> future =
            stockTradeService.getAllStockTradesAsync(0, 10);
        PagedResponse<StockTradeDto> result = future.get();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getData()).hasSize(3);
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getTotalElements()).isEqualTo(3);

        // Verify async execution completed
        assertThat(future.isDone()).isTrue();
        assertThat(future.isCompletedExceptionally()).isFalse();
    }

    @Test
    void testGetStockTradeById() {
        // Act
        StockTradeDto result = stockTradeService.getStockTradeById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getSymbol()).isEqualTo("AAPL");
        assertThat(result.getTradeType()).isEqualTo("BUY");
        assertThat(result.getQuantity()).isEqualTo(100);
        assertThat(result.getPrice()).isEqualTo(new BigDecimal("150.50"));
    }

    @Test
    void testGetStockTradeByIdNotFound() {
        // Act & Assert
        assertThatThrownBy(() -> stockTradeService.getStockTradeById(999L))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("Stock trade not found with ID: 999")
            .extracting("statusCode").isEqualTo(404);
    }

    @Test
    void testGetStockTradeByIdAsync() throws ExecutionException, InterruptedException {
        // Act
        CompletableFuture<StockTradeDto> future = stockTradeService.getStockTradeByIdAsync(1L);
        StockTradeDto result = future.get();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getSymbol()).isEqualTo("AAPL");
        assertThat(future.isDone()).isTrue();
    }

    @Test
    void testGetStockTradesBySymbol() {
        // Act
        PagedResponse<StockTradeDto> result = stockTradeService.getStockTradesBySymbol("AAPL", 0, 10);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getData()).hasSize(2); // We have 2 AAPL trades in test data
        assertThat(result.getData().get(0).getSymbol()).isEqualTo("AAPL");
        assertThat(result.getData().get(1).getSymbol()).isEqualTo("AAPL");
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(1);
    }

    @Test
    void testGetStockTradesBySymbolAsync() throws ExecutionException, InterruptedException {
        // Act
        CompletableFuture<PagedResponse<StockTradeDto>> future =
            stockTradeService.getStockTradesBySymbolAsync("AAPL", 0, 10);
        PagedResponse<StockTradeDto> result = future.get();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getData()).hasSize(2);
        assertThat(result.getData().get(0).getSymbol()).isEqualTo("AAPL");
        assertThat(future.isDone()).isTrue();
    }

    @Test
    void testValidatePaginationParameters() {
        // Test negative page
        assertThatThrownBy(() -> stockTradeService.getAllStockTrades(-1, 10))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("Page number cannot be negative");

        // Test zero size
        assertThatThrownBy(() -> stockTradeService.getAllStockTrades(0, 0))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("Page size must be positive");

        // Test size too large
        assertThatThrownBy(() -> stockTradeService.getAllStockTrades(0, 1001))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("Page size cannot exceed 1000");
    }

    @Test
    void testValidateSymbol() {
        // Test null symbol
        assertThatThrownBy(() -> stockTradeService.getStockTradesBySymbol(null, 0, 10))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("Symbol cannot be null or empty");

        // Test empty symbol
        assertThatThrownBy(() -> stockTradeService.getStockTradesBySymbol("", 0, 10))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("Symbol cannot be null or empty");

        // Test symbol too long
        assertThatThrownBy(() -> stockTradeService.getStockTradesBySymbol("VERYLONGSYMBOL", 0, 10))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("Symbol cannot exceed 10 characters");

        // Test symbol with numbers
        assertThatThrownBy(() -> stockTradeService.getStockTradesBySymbol("AAPL123", 0, 10))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("Symbol can only contain letters");
    }

    @Test
    void testValidateStockTradeId() {
        // Test null ID
        assertThatThrownBy(() -> stockTradeService.getStockTradeById(null))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("Invalid stock trade ID");

        // Test negative ID
        assertThatThrownBy(() -> stockTradeService.getStockTradeById(-1L))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("Invalid stock trade ID");

        // Test zero ID
        assertThatThrownBy(() -> stockTradeService.getStockTradeById(0L))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("Invalid stock trade ID");
    }

    @Test
    void testGetHealthStatusAsync() throws ExecutionException, InterruptedException {
        // Act
        CompletableFuture<Boolean> future = stockTradeService.getHealthStatusAsync();
        Boolean result = future.get();

        // Assert
        assertThat(result).isTrue(); // Should be healthy with real database
        assertThat(future.isDone()).isTrue();
    }

    private void insertTestData() throws SQLException {
        String insertSql = """
            INSERT INTO stock_trades (symbol, trade_type, quantity, price, total_value,
                                    trade_date_time, trader_id, exchange)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(insertSql)) {

            LocalDateTime now = LocalDateTime.now();

            // Trade 1 - AAPL BUY
            statement.setString(1, "AAPL");
            statement.setString(2, "BUY");
            statement.setInt(3, 100);
            statement.setBigDecimal(4, BigDecimal.valueOf(150.50));
            statement.setBigDecimal(5, BigDecimal.valueOf(15050.00));
            statement.setObject(6, now.minusHours(1));
            statement.setString(7, "TRADER_001");
            statement.setString(8, "NASDAQ");
            statement.addBatch();

            // Trade 2 - GOOGL SELL
            statement.setString(1, "GOOGL");
            statement.setString(2, "SELL");
            statement.setInt(3, 50);
            statement.setBigDecimal(4, BigDecimal.valueOf(2500.75));
            statement.setBigDecimal(5, BigDecimal.valueOf(125037.50));
            statement.setObject(6, now.minusHours(2));
            statement.setString(7, "TRADER_002");
            statement.setString(8, "NYSE");
            statement.addBatch();

            // Trade 3 - AAPL SELL
            statement.setString(1, "AAPL");
            statement.setString(2, "SELL");
            statement.setInt(3, 75);
            statement.setBigDecimal(4, BigDecimal.valueOf(151.25));
            statement.setBigDecimal(5, BigDecimal.valueOf(11343.75));
            statement.setObject(6, now.minusHours(3));
            statement.setString(7, "TRADER_003");
            statement.setString(8, "NASDAQ");
            statement.addBatch();

            statement.executeBatch();
        }
    }
}
