package dev.mars.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.mars.Application;
import dev.mars.config.GuiceModule;
import dev.mars.database.DatabaseManager;
import dev.mars.dto.PagedResponse;
import dev.mars.dto.StockTradeDto;
import dev.mars.model.StockTrade;
import io.javalin.Javalin;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for the Stock Trade API
 */
class StockTradeApiIntegrationTest {

    private Application application;
    private Injector injector;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Set system property to use test configuration
        System.setProperty("config.file", "application-test.yml");

        application = new Application();
        application.start();
        injector = application.getInjector();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Clean database before each test
        DatabaseManager databaseManager = injector.getInstance(DatabaseManager.class);
        databaseManager.cleanDatabase();

        // Also clean metrics database to prevent interference
        try {
            var metricsDatabaseManager = injector.getInstance(dev.mars.database.MetricsDatabaseManager.class);
            metricsDatabaseManager.cleanDatabase();
        } catch (Exception e) {
            // Ignore if metrics database manager not available
        }

        // Insert test data
        insertTestData();
    }

    @AfterEach
    void tearDown() {
        if (application != null) {
            try {
                application.stop();
                // Wait for proper shutdown
                Thread.sleep(500);
            } catch (Exception e) {
                // Ignore cleanup errors in tests
            }
            application = null; // Clear the reference to allow new instance creation
        }
        System.clearProperty("config.file");
    }

    @Test
    void testHealthEndpoint() throws Exception {
        String baseUrl = "http://localhost:" + application.getPort();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(baseUrl + "/api/health")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);

            String responseBody = response.body().string();
            assertThat(responseBody).isNotEmpty();
            assertThat(responseBody).contains("status");
            assertThat(responseBody).contains("UP");
        }
    }

    @Test
    void testGetAllStockTrades() throws Exception {
        String baseUrl = "http://localhost:" + application.getPort();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(baseUrl + "/api/stock-trades")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);

            String responseBody = response.body().string();
            PagedResponse<?> pagedResponse = objectMapper.readValue(responseBody, PagedResponse.class);

            assertThat(pagedResponse.getData()).isNotEmpty();
            assertThat(pagedResponse.getPage()).isEqualTo(0);
            assertThat(pagedResponse.getSize()).isEqualTo(20);
            assertThat(pagedResponse.getTotalElements()).isGreaterThan(0);
        }
    }

    @Test
    void testGetAllStockTradesWithPagination() throws Exception {
        String baseUrl = "http://localhost:" + application.getPort();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(baseUrl + "/api/stock-trades?page=0&size=2")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);

            String responseBody = response.body().string();
            PagedResponse<?> pagedResponse = objectMapper.readValue(responseBody, PagedResponse.class);

            assertThat(pagedResponse.getData()).hasSize(2);
            assertThat(pagedResponse.getPage()).isEqualTo(0);
            assertThat(pagedResponse.getSize()).isEqualTo(2);
        }
    }

    @Test
    void testGetStockTradeById() throws Exception {
        String baseUrl = "http://localhost:" + application.getPort();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(baseUrl + "/api/stock-trades/1")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);

            String responseBody = response.body().string();
            StockTradeDto tradeDto = objectMapper.readValue(responseBody, StockTradeDto.class);

            assertThat(tradeDto.getId()).isEqualTo(1L);
            assertThat(tradeDto.getSymbol()).isNotNull();
            assertThat(tradeDto.getTradeType()).isIn("BUY", "SELL");
        }
    }

    @Test
    void testGetStockTradeByIdNotFound() throws Exception {
        String baseUrl = "http://localhost:" + application.getPort();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(baseUrl + "/api/stock-trades/999")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(404);

            String responseBody = response.body().string();
            assertThat(responseBody).contains("NOT_FOUND");
            assertThat(responseBody).contains("The requested resource was not found");
        }
    }

    @Test
    void testGetStockTradesBySymbol() throws Exception {
        String baseUrl = "http://localhost:" + application.getPort();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(baseUrl + "/api/stock-trades/symbol/AAPL")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);

            String responseBody = response.body().string();
            PagedResponse<?> pagedResponse = objectMapper.readValue(responseBody, PagedResponse.class);

            assertThat(pagedResponse.getData()).isNotEmpty();
            // All trades should have AAPL symbol
            // Note: This would require more complex JSON parsing to verify the symbol
        }
    }

    @Test
    void testGetStockTradesBySymbolNotFound() throws Exception {
        String baseUrl = "http://localhost:" + application.getPort();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(baseUrl + "/api/stock-trades/symbol/NOTFOUND")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);

            String responseBody = response.body().string();
            PagedResponse<?> pagedResponse = objectMapper.readValue(responseBody, PagedResponse.class);

            assertThat(pagedResponse.getData()).isEmpty();
            assertThat(pagedResponse.getTotalElements()).isEqualTo(0);
        }
    }

    @Test
    void testInvalidPaginationParameters() throws Exception {
        String baseUrl = "http://localhost:" + application.getPort();
        OkHttpClient client = new OkHttpClient();

        // Test negative page
        Request request1 = new Request.Builder()
                .url(baseUrl + "/api/stock-trades?page=-1")
                .build();
        try (Response response1 = client.newCall(request1).execute()) {
            assertThat(response1.code()).isEqualTo(400);
        }

        // Test zero size
        Request request2 = new Request.Builder()
                .url(baseUrl + "/api/stock-trades?size=0")
                .build();
        try (Response response2 = client.newCall(request2).execute()) {
            assertThat(response2.code()).isEqualTo(400);
        }

        // Test size too large
        Request request3 = new Request.Builder()
                .url(baseUrl + "/api/stock-trades?size=1001")
                .build();
        try (Response response3 = client.newCall(request3).execute()) {
            assertThat(response3.code()).isEqualTo(400);
        }
    }

    @Test
    void testInvalidSymbol() throws Exception {
        String baseUrl = "http://localhost:" + application.getPort();
        OkHttpClient client = new OkHttpClient();

        // Test symbol with numbers
        Request request1 = new Request.Builder()
                .url(baseUrl + "/api/stock-trades/symbol/AAPL123")
                .build();
        try (Response response1 = client.newCall(request1).execute()) {
            assertThat(response1.code()).isEqualTo(400);
        }

        // Test symbol too long
        Request request2 = new Request.Builder()
                .url(baseUrl + "/api/stock-trades/symbol/VERYLONGSYMBOL")
                .build();
        try (Response response2 = client.newCall(request2).execute()) {
            assertThat(response2.code()).isEqualTo(400);
        }
    }

    @Test
    void testInvalidStockTradeId() throws Exception {
        String baseUrl = "http://localhost:" + application.getPort();
        OkHttpClient client = new OkHttpClient();

        // Test invalid ID format
        Request request1 = new Request.Builder()
                .url(baseUrl + "/api/stock-trades/invalid")
                .build();
        try (Response response1 = client.newCall(request1).execute()) {
            assertThat(response1.code()).isEqualTo(400);
        }

        // Test negative ID
        Request request2 = new Request.Builder()
                .url(baseUrl + "/api/stock-trades/-1")
                .build();
        try (Response response2 = client.newCall(request2).execute()) {
            assertThat(response2.code()).isEqualTo(400);
        }
    }

    @Test
    void testAsyncEndpoints() throws Exception {
        String baseUrl = "http://localhost:" + application.getPort();
        OkHttpClient client = new OkHttpClient();

        // Test async get all
        Request request1 = new Request.Builder()
                .url(baseUrl + "/api/stock-trades?async=true")
                .build();
        try (Response response1 = client.newCall(request1).execute()) {
            assertThat(response1.code()).isEqualTo(200);
        }

        // Test async get by id
        Request request2 = new Request.Builder()
                .url(baseUrl + "/api/stock-trades/1?async=true")
                .build();
        try (Response response2 = client.newCall(request2).execute()) {
            assertThat(response2.code()).isEqualTo(200);
        }

        // Test async get by symbol
        Request request3 = new Request.Builder()
                .url(baseUrl + "/api/stock-trades/symbol/AAPL?async=true")
                .build();
        try (Response response3 = client.newCall(request3).execute()) {
            assertThat(response3.code()).isEqualTo(200);
        }
    }

    @Test
    void testNotFoundEndpoint() throws Exception {
        String baseUrl = "http://localhost:" + application.getPort();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(baseUrl + "/api/nonexistent")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(404);

            String responseBody = response.body().string();
            assertThat(responseBody).contains("NOT_FOUND");
            assertThat(responseBody).contains("The requested resource was not found");
        }
    }

    private void insertTestData() {
        try {
            DatabaseManager databaseManager = injector.getInstance(DatabaseManager.class);
            
            String insertSql = """
                INSERT INTO stock_trades (symbol, trade_type, quantity, price, total_value, 
                                        trade_date_time, trader_id, exchange) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(insertSql)) {
                
                // Insert test data
                LocalDateTime now = LocalDateTime.now();
                
                // Trade 1
                statement.setString(1, "AAPL");
                statement.setString(2, "BUY");
                statement.setInt(3, 100);
                statement.setBigDecimal(4, BigDecimal.valueOf(150.50));
                statement.setBigDecimal(5, BigDecimal.valueOf(15050.00));
                statement.setObject(6, now.minusHours(1));
                statement.setString(7, "TRADER_001");
                statement.setString(8, "NASDAQ");
                statement.addBatch();
                
                // Trade 2
                statement.setString(1, "GOOGL");
                statement.setString(2, "SELL");
                statement.setInt(3, 50);
                statement.setBigDecimal(4, BigDecimal.valueOf(2500.75));
                statement.setBigDecimal(5, BigDecimal.valueOf(125037.50));
                statement.setObject(6, now.minusHours(2));
                statement.setString(7, "TRADER_002");
                statement.setString(8, "NYSE");
                statement.addBatch();
                
                // Trade 3 - Another AAPL trade
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
            
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert test data", e);
        }
    }
}
