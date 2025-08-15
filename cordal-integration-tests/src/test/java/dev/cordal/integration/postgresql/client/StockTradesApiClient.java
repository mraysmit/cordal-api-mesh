package dev.cordal.integration.postgresql.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * REST API client for testing stock trades endpoints
 * Provides methods to interact with the Generic API Service as an external client would
 */
public class StockTradesApiClient {
    private static final Logger logger = LoggerFactory.getLogger(StockTradesApiClient.class);
    
    private final String baseUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public StockTradesApiClient(String baseUrl, ObjectMapper objectMapper) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        
        logger.debug("Created StockTradesApiClient for base URL: {}", this.baseUrl);
    }
    
    /**
     * Check if the API service is healthy and responding
     * 
     * @return true if the service is healthy, false otherwise
     */
    public boolean isHealthy() {
        try {
            String url = baseUrl + "/api/health";
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                boolean healthy = response.isSuccessful();
                logger.debug("Health check: {} - {}", url, healthy ? "OK" : "FAILED");
                return healthy;
            }
        } catch (Exception e) {
            logger.debug("Health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get all stock trades with pagination
     * 
     * @param databasePath Database path prefix (e.g., "trades-db-1")
     * @param page Page number (0-based)
     * @param size Page size
     * @return JSON response containing paginated stock trades
     * @throws IOException if request fails
     */
    public JsonNode getAllStockTrades(String databasePath, int page, int size) throws IOException {
        String url = String.format("%s/api/%s/stock-trades?page=%d&size=%d", 
                                  baseUrl, databasePath, page, size);
        
        logger.debug("Getting all stock trades: {}", url);
        return makeGetRequest(url);
    }
    
    /**
     * Get stock trades by symbol
     * 
     * @param databasePath Database path prefix (e.g., "trades-db-1")
     * @param symbol Stock symbol to filter by
     * @param page Page number (0-based)
     * @param size Page size
     * @return JSON response containing filtered stock trades
     * @throws IOException if request fails
     */
    public JsonNode getStockTradesBySymbol(String databasePath, String symbol, int page, int size) throws IOException {
        String url = String.format("%s/api/%s/stock-trades/symbol/%s?page=%d&size=%d", 
                                  baseUrl, databasePath, symbol, page, size);
        
        logger.debug("Getting stock trades by symbol: {}", url);
        return makeGetRequest(url);
    }
    
    /**
     * Get stock trades by trader ID
     * 
     * @param databasePath Database path prefix (e.g., "trades-db-1")
     * @param traderId Trader ID to filter by
     * @param page Page number (0-based)
     * @param size Page size
     * @return JSON response containing filtered stock trades
     * @throws IOException if request fails
     */
    public JsonNode getStockTradesByTrader(String databasePath, String traderId, int page, int size) throws IOException {
        String url = String.format("%s/api/%s/stock-trades/trader/%s?page=%d&size=%d", 
                                  baseUrl, databasePath, traderId, page, size);
        
        logger.debug("Getting stock trades by trader: {}", url);
        return makeGetRequest(url);
    }
    
    /**
     * Get configuration validation results
     * 
     * @return JSON response containing validation results
     * @throws IOException if request fails
     */
    public JsonNode getConfigurationValidation() throws IOException {
        String url = baseUrl + "/api/generic/config/validate";
        
        logger.debug("Getting configuration validation: {}", url);
        return makeGetRequest(url);
    }
    
    /**
     * Get list of configured endpoints
     * 
     * @return JSON response containing endpoint configurations
     * @throws IOException if request fails
     */
    public JsonNode getConfiguredEndpoints() throws IOException {
        String url = baseUrl + "/api/generic/config/endpoints";
        
        logger.debug("Getting configured endpoints: {}", url);
        return makeGetRequest(url);
    }
    
    /**
     * Get list of configured queries
     * 
     * @return JSON response containing query configurations
     * @throws IOException if request fails
     */
    public JsonNode getConfiguredQueries() throws IOException {
        String url = baseUrl + "/api/generic/config/queries";
        
        logger.debug("Getting configured queries: {}", url);
        return makeGetRequest(url);
    }
    
    /**
     * Get list of configured databases
     * 
     * @return JSON response containing database configurations
     * @throws IOException if request fails
     */
    public JsonNode getConfiguredDatabases() throws IOException {
        String url = baseUrl + "/api/generic/config/databases";
        
        logger.debug("Getting configured databases: {}", url);
        return makeGetRequest(url);
    }
    
    /**
     * Get Swagger/OpenAPI specification
     * 
     * @return Raw response body as string
     * @throws IOException if request fails
     */
    public String getSwaggerSpec() throws IOException {
        String url = baseUrl + "/api-docs";
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Request failed: " + response.code() + " " + response.message());
            }
            
            String responseBody = response.body().string();
            logger.debug("Retrieved Swagger spec ({} bytes)", responseBody.length());
            return responseBody;
        }
    }
    
    /**
     * Test endpoint connectivity and response time
     * 
     * @param databasePath Database path prefix
     * @return Response time in milliseconds, or -1 if failed
     */
    public long testEndpointPerformance(String databasePath) {
        try {
            String url = String.format("%s/api/%s/stock-trades?page=0&size=1", baseUrl, databasePath);
            
            long startTime = System.currentTimeMillis();
            
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                long endTime = System.currentTimeMillis();
                long responseTime = endTime - startTime;
                
                if (response.isSuccessful()) {
                    logger.debug("Endpoint performance test: {} - {}ms", url, responseTime);
                    return responseTime;
                } else {
                    logger.warn("Endpoint performance test failed: {} - {}", url, response.code());
                    return -1;
                }
            }
        } catch (Exception e) {
            logger.warn("Endpoint performance test error: {}", e.getMessage());
            return -1;
        }
    }

    /**
     * Test invalid database path (intentional error test)
     * This method is designed for testing error handling and will NOT throw exceptions for expected errors
     *
     * @param invalidDatabasePath Invalid database path to test
     * @param page Page number
     * @param size Page size
     * @return ApiTestResult indicating success or expected error
     */
    public ApiTestResult testInvalidDatabasePath(String invalidDatabasePath, int page, int size) {
        String url = String.format("%s/api/%s/stock-trades?page=%d&size=%d",
                                  baseUrl, invalidDatabasePath, page, size);

        logger.debug("INTENTIONAL ERROR TEST: Testing invalid database path: {}", url);
        return makeGetRequestForTest(url, true, "Invalid database path test");
    }

    /**
     * Test invalid page parameter (intentional error test)
     * This method is designed for testing error handling and will NOT throw exceptions for expected errors
     *
     * @param databasePath Valid database path
     * @param invalidPage Invalid page number (e.g., negative)
     * @param size Page size
     * @return ApiTestResult indicating success or expected error
     */
    public ApiTestResult testInvalidPageParameter(String databasePath, int invalidPage, int size) {
        String url = String.format("%s/api/%s/stock-trades?page=%d&size=%d",
                                  baseUrl, databasePath, invalidPage, size);

        logger.debug("INTENTIONAL ERROR TEST: Testing invalid page parameter: {}", url);
        return makeGetRequestForTest(url, true, "Invalid page parameter test");
    }

    /**
     * Make a GET request and parse the JSON response
     * 
     * @param url URL to request
     * @return Parsed JSON response
     * @throws IOException if request fails or JSON parsing fails
     */
    private JsonNode makeGetRequest(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Request failed: " + response.code() + " " + response.message() + 
                                    " for URL: " + url);
            }
            
            String responseBody = response.body().string();
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            
            logger.debug("GET {} - {} ({} bytes)", url, response.code(), responseBody.length());
            return jsonResponse;
        }
    }

    /**
     * Make a GET request for testing purposes (does not throw exceptions for expected errors)
     *
     * @param url URL to request
     * @param expectError Whether an error is expected (for intentional error testing)
     * @param testDescription Description of the test for logging
     * @return ApiTestResult with success/error information
     */
    private ApiTestResult makeGetRequestForTest(String url, boolean expectError, String testDescription) {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

            if (response.isSuccessful()) {
                if (expectError) {
                    logger.debug("INTENTIONAL ERROR TEST: {} - Expected error but got success: {}",
                               testDescription, response.code());
                    return ApiTestResult.unexpectedError(response.code(),
                        "Expected error but got success", responseBody);
                } else {
                    logger.debug("Test request successful: {} - {}", testDescription, response.code());
                    return ApiTestResult.success(response.code(), responseBody);
                }
            } else {
                if (expectError) {
                    logger.debug("INTENTIONAL ERROR TEST: {} - Got expected error: {} {}",
                               testDescription, response.code(), response.message());
                    return ApiTestResult.expectedError(response.code(),
                        response.code() + " " + response.message(), responseBody);
                } else {
                    logger.debug("Test request failed unexpectedly: {} - {} {}",
                               testDescription, response.code(), response.message());
                    return ApiTestResult.unexpectedError(response.code(),
                        response.code() + " " + response.message(), responseBody);
                }
            }
        } catch (Exception e) {
            logger.debug("Test request exception: {} - {}", testDescription, e.getMessage());
            return ApiTestResult.exception(e);
        }
    }

    /**
     * Close the HTTP client and release resources
     */
    public void close() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
        logger.debug("StockTradesApiClient closed");
    }
    
    /**
     * Get the base URL for this client
     * 
     * @return Base URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }
}
