package dev.mars.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mars.Application;
import dev.mars.generic.model.GenericResponse;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for Generic API endpoints
 */
class GenericApiIntegrationTest {

    private Application application;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Use test configuration
        System.setProperty("config.file", "application-test.yml");
        application = new Application();
        application.start();
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() {
        if (application != null) {
            try {
                application.stop();
            } catch (Exception e) {
                // Ignore cleanup errors in tests
            }
        }
        System.clearProperty("config.file");
    }

    @Test
    void testGenericApiHealth() throws Exception {
        String baseUrl = "http://localhost:" + application.getPort();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(baseUrl + "/api/generic/health")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);

            String responseBody = response.body().string();
            @SuppressWarnings("unchecked")
            Map<String, Object> health = objectMapper.readValue(responseBody, Map.class);

            assertThat(health.get("status")).isEqualTo("UP");
            assertThat(health.get("service")).isEqualTo("Generic API Service");
            assertThat(health).containsKey("availableEndpoints");
            assertThat(health).containsKey("timestamp");
        }
    }

    @Test
    void testGetAvailableEndpoints() throws Exception {
        String baseUrl = "http://localhost:" + application.getPort();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(baseUrl + "/api/generic/endpoints")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);

            String responseBody = response.body().string();
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

            // The response should have the structure: {"totalEndpoints": 5, "endpoints": {...}}
            assertThat(responseMap).containsKey("totalEndpoints");
            assertThat(responseMap).containsKey("endpoints");

            @SuppressWarnings("unchecked")
            Map<String, Object> endpoints = (Map<String, Object>) responseMap.get("endpoints");
            assertThat(endpoints).isNotEmpty();
            assertThat(endpoints).containsKey("stock-trades-list");
        }
    }

    @Test
    void testGetEndpointConfiguration() throws Exception {
        String baseUrl = "http://localhost:" + application.getPort();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(baseUrl + "/api/generic/endpoints/stock-trades-list")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);

            String responseBody = response.body().string();
            @SuppressWarnings("unchecked")
            Map<String, Object> config = objectMapper.readValue(responseBody, Map.class);

            assertThat(config.get("path")).isEqualTo("/api/generic/stock-trades");
            assertThat(config.get("method")).isEqualTo("GET");
            assertThat(config.get("query")).isEqualTo("stock-trades-all");
        }
    }

    @Test
    void testGetEndpointConfiguration_NotFound() throws Exception {
        String baseUrl = "http://localhost:" + application.getPort();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(baseUrl + "/api/generic/endpoints/nonexistent-endpoint")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(404);
        }
    }

    @Test
    void testGenericStockTradesEndpoint() throws Exception {
        String baseUrl = "http://localhost:" + application.getPort();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(baseUrl + "/api/generic/stock-trades")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);

            String responseBody = response.body().string();
            GenericResponse genericResponse = objectMapper.readValue(responseBody, GenericResponse.class);

            assertThat(genericResponse.getType()).isEqualTo("PAGED");
            assertThat(genericResponse.getData()).isNotNull();
            assertThat(genericResponse.getPagination()).isNotNull();
            assertThat(genericResponse.getPagination().getPage()).isEqualTo(0);
            assertThat(genericResponse.getPagination().getSize()).isEqualTo(20);
        }
    }

    @Test
    void testGenericStockTradesEndpoint_WithPagination() throws Exception {
        String baseUrl = "http://localhost:" + application.getPort();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(baseUrl + "/api/generic/stock-trades?page=1&size=5")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);

            String responseBody = response.body().string();
            GenericResponse genericResponse = objectMapper.readValue(responseBody, GenericResponse.class);

            assertThat(genericResponse.getType()).isEqualTo("PAGED");
            assertThat(genericResponse.getPagination().getPage()).isEqualTo(1);
            assertThat(genericResponse.getPagination().getSize()).isEqualTo(5);
        }
    }

    @Test
    void testGenericStockTradesEndpoint_AsyncMode() throws Exception {
        String baseUrl = "http://localhost:" + application.getPort();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(baseUrl + "/api/generic/stock-trades?async=true")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);

            String responseBody = response.body().string();
            @SuppressWarnings("unchecked")
            Map<String, Object> asyncResponse = objectMapper.readValue(responseBody, Map.class);

            assertThat(asyncResponse).containsKey("message");
            assertThat(asyncResponse).containsKey("requestId");
            assertThat(asyncResponse.get("message")).isEqualTo("Request submitted for async processing");
        }
    }

    @Test
    void testGenericEndpoint_InvalidPagination() throws Exception {
        String baseUrl = "http://localhost:" + application.getPort();

        OkHttpClient client = new OkHttpClient();
        
        // Test negative page
        Request request1 = new Request.Builder()
                .url(baseUrl + "/api/generic/stock-trades?page=-1")
                .build();

        try (Response response1 = client.newCall(request1).execute()) {
            assertThat(response1.code()).isEqualTo(400);
        }

        // Test zero size
        Request request2 = new Request.Builder()
                .url(baseUrl + "/api/generic/stock-trades?size=0")
                .build();

        try (Response response2 = client.newCall(request2).execute()) {
            assertThat(response2.code()).isEqualTo(400);
        }

        // Test size exceeding maximum
        Request request3 = new Request.Builder()
                .url(baseUrl + "/api/generic/stock-trades?size=200")
                .build();

        try (Response response3 = client.newCall(request3).execute()) {
            assertThat(response3.code()).isEqualTo(400);
        }
    }

    @Test
    void testGenericEndpoint_NonexistentEndpoint() throws Exception {
        String baseUrl = "http://localhost:" + application.getPort();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(baseUrl + "/api/generic/nonexistent-endpoint")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(404);

            String responseBody = response.body().string();
            @SuppressWarnings("unchecked")
            Map<String, Object> errorResponse = objectMapper.readValue(responseBody, Map.class);

            assertThat(errorResponse).containsKey("error");
            assertThat(errorResponse.get("error")).toString().contains("Endpoint not found");
        }
    }

    @Test
    void testGenericStockTradeById() throws Exception {
        String baseUrl = "http://localhost:" + application.getPort();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(baseUrl + "/api/generic/stock-trades/1")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 200) {
                String responseBody = response.body().string();
                GenericResponse genericResponse = objectMapper.readValue(responseBody, GenericResponse.class);

                assertThat(genericResponse.getType()).isEqualTo("SINGLE");
                assertThat(genericResponse.getData()).isNotNull();
                assertThat(genericResponse.getPagination()).isNull();

                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) genericResponse.getData();
                assertThat(data).containsKey("id");
                assertThat(data.get("id")).isEqualTo(1);
            } else {
                // If no data exists with ID 1, should return 404
                assertThat(response.code()).isEqualTo(404);
            }
        }
    }

    @Test
    void testGenericResponseStructure() throws Exception {
        String baseUrl = "http://localhost:" + application.getPort();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(baseUrl + "/api/generic/stock-trades")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);

            String responseBody = response.body().string();
            GenericResponse genericResponse = objectMapper.readValue(responseBody, GenericResponse.class);

            // Verify response structure
            assertThat(genericResponse.getType()).isNotNull();
            assertThat(genericResponse.getData()).isNotNull();
            assertThat(genericResponse.getTimestamp()).isNotNull();
            assertThat(genericResponse.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());

            // For paged responses, verify pagination info
            if ("PAGED".equals(genericResponse.getType())) {
                assertThat(genericResponse.getPagination()).isNotNull();
                assertThat(genericResponse.getPagination().getPage()).isGreaterThanOrEqualTo(0);
                assertThat(genericResponse.getPagination().getSize()).isGreaterThan(0);
                assertThat(genericResponse.getPagination().getTotalElements()).isGreaterThanOrEqualTo(0);
                assertThat(genericResponse.getPagination().getTotalPages()).isGreaterThanOrEqualTo(0);
            }

            // Verify data structure
            if (genericResponse.getData() instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) genericResponse.getData();
                if (!dataList.isEmpty()) {
                    Map<String, Object> firstItem = dataList.get(0);
                    assertThat(firstItem).containsKey("id");
                    assertThat(firstItem).containsKey("symbol");
                    assertThat(firstItem).containsKey("trade_type");
                }
            }
        }
    }

    @Test
    void testContentTypeHeaders() throws Exception {
        String baseUrl = "http://localhost:" + application.getPort();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(baseUrl + "/api/generic/stock-trades")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.header("Content-Type")).contains("application/json");
        }
    }

    @Test
    void testGetCompleteConfiguration() throws Exception {
        String baseUrl = "http://localhost:" + application.getPort();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(baseUrl + "/api/generic/config")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);

            String responseBody = response.body().string();
            @SuppressWarnings("unchecked")
            Map<String, Object> config = objectMapper.readValue(responseBody, Map.class);

            // Verify response structure
            assertThat(config).containsKey("summary");
            assertThat(config).containsKey("endpoints");
            assertThat(config).containsKey("queries");

            @SuppressWarnings("unchecked")
            Map<String, Object> summary = (Map<String, Object>) config.get("summary");
            assertThat(summary).containsKey("totalEndpoints");
            assertThat(summary).containsKey("totalQueries");
            assertThat(summary).containsKey("timestamp");

            @SuppressWarnings("unchecked")
            Map<String, Object> endpoints = (Map<String, Object>) config.get("endpoints");
            assertThat(endpoints).isNotEmpty();
            assertThat(endpoints).containsKey("stock-trades-list");

            @SuppressWarnings("unchecked")
            Map<String, Object> queries = (Map<String, Object>) config.get("queries");
            assertThat(queries).isNotEmpty();
            assertThat(queries).containsKey("stock-trades-all");
            assertThat(queries).containsKey("stock-trades-count");
        }
    }

    @Test
    void testGetQueryConfigurations() throws Exception {
        String baseUrl = "http://localhost:" + application.getPort();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(baseUrl + "/api/generic/config/queries")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);

            String responseBody = response.body().string();
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

            // Verify response structure
            assertThat(responseMap).containsKey("totalQueries");
            assertThat(responseMap).containsKey("queries");

            @SuppressWarnings("unchecked")
            Map<String, Object> queries = (Map<String, Object>) responseMap.get("queries");
            assertThat(queries).isNotEmpty();
            assertThat(queries).containsKey("stock-trades-all");
            assertThat(queries).containsKey("stock-trades-count");
            assertThat(queries).containsKey("stock-trades-by-id");

            // Verify query structure
            @SuppressWarnings("unchecked")
            Map<String, Object> stockTradesAll = (Map<String, Object>) queries.get("stock-trades-all");
            assertThat(stockTradesAll).containsKey("name");
            assertThat(stockTradesAll).containsKey("description");
            assertThat(stockTradesAll).containsKey("sql");
            assertThat(stockTradesAll.get("name")).isEqualTo("stock-trades-all");
            assertThat(stockTradesAll.get("description")).toString().contains("Get All Stock Trades");
        }
    }

    @Test
    void testGetSpecificQueryConfiguration() throws Exception {
        String baseUrl = "http://localhost:" + application.getPort();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(baseUrl + "/api/generic/config/queries/stock-trades-all")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);

            String responseBody = response.body().string();
            @SuppressWarnings("unchecked")
            Map<String, Object> query = objectMapper.readValue(responseBody, Map.class);

            // Verify query structure
            assertThat(query).containsKey("name");
            assertThat(query).containsKey("description");
            assertThat(query).containsKey("sql");
            assertThat(query.get("name")).isEqualTo("stock-trades-all");
            assertThat(query.get("description")).toString().contains("Get All Stock Trades");
            assertThat(query.get("sql")).toString().contains("SELECT");
        }
    }

    @Test
    void testGetNonexistentQueryConfiguration() throws Exception {
        String baseUrl = "http://localhost:" + application.getPort();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(baseUrl + "/api/generic/config/queries/nonexistent-query")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(404);

            String responseBody = response.body().string();
            @SuppressWarnings("unchecked")
            Map<String, Object> errorResponse = objectMapper.readValue(responseBody, Map.class);

            assertThat(errorResponse).containsKey("error");
            assertThat(errorResponse.get("error")).toString().contains("Query not found");
        }
    }

    @Test
    void testSwaggerDocumentationIncludesConfigurationEndpoints() throws Exception {
        String baseUrl = "http://localhost:" + application.getPort();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(baseUrl + "/openapi.json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);

            String responseBody = response.body().string();
            @SuppressWarnings("unchecked")
            Map<String, Object> openApiSpec = objectMapper.readValue(responseBody, Map.class);

            // Verify OpenAPI structure
            assertThat(openApiSpec).containsKey("paths");

            @SuppressWarnings("unchecked")
            Map<String, Object> paths = (Map<String, Object>) openApiSpec.get("paths");

            // Verify configuration endpoints are documented
            assertThat(paths).containsKey("/api/generic/config");
            assertThat(paths).containsKey("/api/generic/config/queries");
            assertThat(paths).containsKey("/api/generic/config/queries/{queryName}");
            assertThat(paths).containsKey("/api/generic/health");
            assertThat(paths).containsKey("/api/generic/endpoints");

            // Verify tags include Configuration
            assertThat(openApiSpec).containsKey("tags");
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> tags = (java.util.List<Map<String, Object>>) openApiSpec.get("tags");

            boolean hasConfigurationTag = tags.stream()
                .anyMatch(tag -> "Configuration".equals(tag.get("name")));
            assertThat(hasConfigurationTag).isTrue();

            boolean hasGenericApiTag = tags.stream()
                .anyMatch(tag -> "Generic API".equals(tag.get("name")));
            assertThat(hasGenericApiTag).isTrue();
        }
    }

    @Test
    void testApiDocsIncludesConfigurationEndpoints() throws Exception {
        String baseUrl = "http://localhost:" + application.getPort();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(baseUrl + "/api-docs")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);

            String responseBody = response.body().string();

            // Verify configuration endpoints are documented in HTML
            assertThat(responseBody).contains("Configuration Management");
            assertThat(responseBody).contains("/api/generic/config");
            assertThat(responseBody).contains("/api/generic/config/queries");
            assertThat(responseBody).contains("Generic API");
            assertThat(responseBody).contains("/api/generic/health");
            assertThat(responseBody).contains("/api/generic/endpoints");
        }
    }
}
