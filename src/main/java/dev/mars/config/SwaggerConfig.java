package dev.mars.config;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Configuration for Swagger/OpenAPI documentation
 */
@Singleton
public class SwaggerConfig {
    private static final Logger logger = LoggerFactory.getLogger(SwaggerConfig.class);

    private final AppConfig appConfig;

    @Inject
    public SwaggerConfig(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    /**
     * Configure Swagger UI for Javalin
     */
    public void configureSwagger(Javalin app) {
        logger.info("Configuring Swagger UI for Javalin application");

        // Create OpenAPI JSON endpoint
        app.get("/openapi.json", ctx -> {
            ctx.contentType("application/json");
            ctx.result(generateOpenApiJson());
        });

        // Create Swagger UI endpoint
        app.get("/swagger", ctx -> {
            ctx.contentType("text/html");
            ctx.result(generateSwaggerHtml());
        });

        // Create API documentation endpoint
        app.get("/api-docs", ctx -> {
            ctx.contentType("text/html");
            ctx.result(generateApiDocsHtml());
        });

        logger.info("Swagger UI configured successfully");
    }

    /**
     * Generate OpenAPI JSON specification
     */
    private String generateOpenApiJson() {
        String baseUrl = String.format("http://%s:%d",
            appConfig.getServerHost(),
            appConfig.getServerPort());

        return """
        {
          "openapi": "3.0.3",
          "info": {
            "title": "Javalin API Mesh",
            "description": "High-performance stock trading API with comprehensive performance monitoring and dual dashboard architecture",
            "version": "1.0.0",
            "contact": {
              "name": "Mars Development Team",
              "email": "dev@mars.com",
              "url": "https://github.com/mars-dev/javalin-api-mesh"
            },
            "license": {
              "name": "MIT License",
              "url": "https://opensource.org/licenses/MIT"
            }
          },
          "servers": [
            {
              "url": "%s",
              "description": "Development Server"
            }
          ],
          "tags": [
            {
              "name": "Health",
              "description": "System health and status endpoints"
            },
            {
              "name": "Stock Trades",
              "description": "Stock trading operations and data retrieval"
            },
            {
              "name": "Performance Metrics",
              "description": "Performance monitoring, metrics collection and analysis"
            }
          ],
          "paths": {
            "/api/health": {
              "get": {
                "tags": ["Health"],
                "summary": "Health check",
                "description": "Check the health status of the application",
                "responses": {
                  "200": {
                    "description": "Application is healthy",
                    "content": {
                      "application/json": {
                        "schema": {
                          "type": "object",
                          "properties": {
                            "status": {"type": "string", "example": "OK"},
                            "timestamp": {"type": "string", "example": "2025-06-29T12:00:00"}
                          }
                        }
                      }
                    }
                  }
                }
              }
            },
            "/api/stock-trades": {
              "get": {
                "tags": ["Stock Trades"],
                "summary": "Get all stock trades",
                "description": "Retrieve a paginated list of all stock trades",
                "parameters": [
                  {
                    "name": "page",
                    "in": "query",
                    "description": "Page number (0-based)",
                    "schema": {"type": "integer", "default": 0}
                  },
                  {
                    "name": "size",
                    "in": "query",
                    "description": "Number of items per page",
                    "schema": {"type": "integer", "default": 20}
                  },
                  {
                    "name": "async",
                    "in": "query",
                    "description": "Enable async processing",
                    "schema": {"type": "boolean", "default": false}
                  }
                ],
                "responses": {
                  "200": {
                    "description": "Successfully retrieved stock trades",
                    "content": {
                      "application/json": {
                        "schema": {
                          "type": "object",
                          "properties": {
                            "content": {
                              "type": "array",
                              "items": {
                                "type": "object",
                                "properties": {
                                  "id": {"type": "integer"},
                                  "symbol": {"type": "string"},
                                  "quantity": {"type": "integer"},
                                  "price": {"type": "number"},
                                  "timestamp": {"type": "string"}
                                }
                              }
                            },
                            "totalElements": {"type": "integer"},
                            "totalPages": {"type": "integer"},
                            "currentPage": {"type": "integer"},
                            "pageSize": {"type": "integer"}
                          }
                        }
                      }
                    }
                  }
                }
              }
            },
            "/api/performance-metrics": {
              "get": {
                "tags": ["Performance Metrics"],
                "summary": "Get all performance metrics",
                "description": "Retrieve a paginated list of all performance metrics",
                "parameters": [
                  {
                    "name": "page",
                    "in": "query",
                    "description": "Page number (0-based)",
                    "schema": {"type": "integer", "default": 0}
                  },
                  {
                    "name": "size",
                    "in": "query",
                    "description": "Number of items per page (max 100)",
                    "schema": {"type": "integer", "default": 20}
                  }
                ],
                "responses": {
                  "200": {
                    "description": "Successfully retrieved performance metrics"
                  }
                }
              },
              "post": {
                "tags": ["Performance Metrics"],
                "summary": "Create performance metrics",
                "description": "Create new performance metrics entry for testing and monitoring",
                "requestBody": {
                  "description": "Performance metrics data",
                  "required": true,
                  "content": {
                    "application/json": {
                      "schema": {
                        "type": "object",
                        "properties": {
                          "testName": {"type": "string"},
                          "testType": {"type": "string"},
                          "timestamp": {"type": "string"},
                          "totalRequests": {"type": "integer"},
                          "totalTimeMs": {"type": "integer"},
                          "averageResponseTimeMs": {"type": "number"},
                          "testPassed": {"type": "boolean"}
                        }
                      }
                    }
                  }
                },
                "responses": {
                  "201": {
                    "description": "Performance metrics created successfully"
                  }
                }
              }
            },
            "/api/performance-metrics/summary": {
              "get": {
                "tags": ["Performance Metrics"],
                "summary": "Get performance summary",
                "description": "Retrieve comprehensive performance statistics and summary data",
                "responses": {
                  "200": {
                    "description": "Successfully retrieved performance summary"
                  }
                }
              }
            }
          }
        }
        """.formatted(baseUrl);
    }

    /**
     * Generate Swagger UI HTML
     */
    private String generateSwaggerHtml() {
        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <title>Javalin API Mesh - Swagger UI</title>
            <link rel="stylesheet" type="text/css" href="https://unpkg.com/swagger-ui-dist@5.1.0/swagger-ui.css" />
            <style>
                html { box-sizing: border-box; overflow: -moz-scrollbars-vertical; overflow-y: scroll; }
                *, *:before, *:after { box-sizing: inherit; }
                body { margin:0; background: #fafafa; }
            </style>
        </head>
        <body>
            <div id="swagger-ui"></div>
            <script src="https://unpkg.com/swagger-ui-dist@5.1.0/swagger-ui-bundle.js"></script>
            <script src="https://unpkg.com/swagger-ui-dist@5.1.0/swagger-ui-standalone-preset.js"></script>
            <script>
                window.onload = function() {
                    const ui = SwaggerUIBundle({
                        url: '/openapi.json',
                        dom_id: '#swagger-ui',
                        deepLinking: true,
                        presets: [
                            SwaggerUIBundle.presets.apis,
                            SwaggerUIStandalonePreset
                        ],
                        plugins: [
                            SwaggerUIBundle.plugins.DownloadUrl
                        ],
                        layout: "StandaloneLayout"
                    });
                };
            </script>
        </body>
        </html>
        """;
    }

    /**
     * Generate API documentation HTML
     */
    private String generateApiDocsHtml() {
        String baseUrl = String.format("http://%s:%d",
            appConfig.getServerHost(),
            appConfig.getServerPort());

        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Javalin API Mesh - API Documentation</title>
            <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 0; padding: 20px; background: #f5f5f5; }
                .container { max-width: 1200px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                h1 { color: #2c3e50; border-bottom: 3px solid #3498db; padding-bottom: 10px; }
                h2 { color: #34495e; margin-top: 30px; }
                .endpoint { background: #f8f9fa; padding: 15px; margin: 10px 0; border-radius: 5px; border-left: 4px solid #3498db; }
                .method { display: inline-block; padding: 4px 8px; border-radius: 3px; color: white; font-weight: bold; margin-right: 10px; }
                .get { background: #28a745; }
                .post { background: #007bff; }
                .put { background: #ffc107; color: #212529; }
                .delete { background: #dc3545; }
                .url { font-family: monospace; background: #e9ecef; padding: 2px 6px; border-radius: 3px; }
                .nav { background: #2c3e50; padding: 15px; margin: -30px -30px 30px -30px; border-radius: 8px 8px 0 0; }
                .nav a { color: white; text-decoration: none; margin-right: 20px; padding: 8px 16px; border-radius: 4px; transition: background 0.3s; }
                .nav a:hover { background: #34495e; }
                .description { color: #6c757d; margin-top: 5px; }
                .badge { background: #17a2b8; color: white; padding: 2px 6px; border-radius: 3px; font-size: 12px; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="nav">
                    <a href="/swagger">🔧 Swagger UI</a>
                    <a href="/dashboard">📊 Dashboard</a>
                    <a href="/api/health">🏥 Health Check</a>
                    <a href="/openapi.json">📄 OpenAPI JSON</a>
                </div>

                <h1>🚀 Javalin API Mesh - API Documentation</h1>
                <p>High-performance stock trading API with comprehensive performance monitoring and dual dashboard architecture.</p>

                <h2>🏥 Health & System</h2>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/health</span>
                    <div class="description">Check the health status of the application</div>
                </div>

                <h2>📈 Stock Trades API</h2>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/stock-trades</span>
                    <span class="badge">Paginated</span>
                    <div class="description">Get all stock trades with pagination support. Supports async processing.</div>
                </div>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/stock-trades/{id}</span>
                    <div class="description">Get a specific stock trade by ID</div>
                </div>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/stock-trades/symbol/{symbol}</span>
                    <div class="description">Get stock trades by symbol</div>
                </div>

                <h2>📊 Performance Metrics API</h2>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/performance-metrics</span>
                    <span class="badge">Paginated</span>
                    <div class="description">Get all performance metrics with pagination</div>
                </div>
                <div class="endpoint">
                    <span class="method post">POST</span>
                    <span class="url">%s/api/performance-metrics</span>
                    <span class="badge">Testing</span>
                    <div class="description">Create new performance metrics entry for testing purposes</div>
                </div>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/performance-metrics/summary</span>
                    <div class="description">Get comprehensive performance statistics and summary data</div>
                </div>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/performance-metrics/trends</span>
                    <div class="description">Get performance trends for dashboard charts</div>
                </div>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/performance-metrics/test-types</span>
                    <div class="description">Get available test types</div>
                </div>

                <h2>📱 Dashboards & Documentation</h2>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/dashboard</span>
                    <div class="description">Interactive performance monitoring dashboard</div>
                </div>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/swagger</span>
                    <div class="description">Swagger UI for interactive API testing</div>
                </div>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api-docs</span>
                    <div class="description">This API documentation page</div>
                </div>
            </div>
        </body>
        </html>
        """.formatted(baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl);
    }

    /**
     * Get Swagger UI URL
     */
    public String getSwaggerUrl() {
        return String.format("http://%s:%d/swagger",
            appConfig.getServerHost(),
            appConfig.getServerPort());
    }

    /**
     * Get API Docs URL
     */
    public String getApiDocsUrl() {
        return String.format("http://%s:%d/api-docs",
            appConfig.getServerHost(),
            appConfig.getServerPort());
    }

    /**
     * Get OpenAPI JSON URL
     */
    public String getOpenApiJsonUrl() {
        return String.format("http://%s:%d/openapi.json",
            appConfig.getServerHost(),
            appConfig.getServerPort());
    }
}
