package dev.cordal.config;

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

    private final GenericApiConfig genericApiConfig;

    @Inject
    public SwaggerConfig(GenericApiConfig genericApiConfig) {
        this.genericApiConfig = genericApiConfig;
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
            genericApiConfig.getServerHost(),
            genericApiConfig.getServerPort());

        return """
        {
          "openapi": "3.0.3",
          "info": {
            "title": "Generic API - CORDAL Configuration Orchestrated REST Dynamic API Layer",
            "description": "High-performance configuration-driven REST API system with YAML-configured endpoints, comprehensive performance monitoring and analytics",
            "version": "1.0.0",
            "contact": {
              "name": "CORDAL Development Team",
              "email": "dev@cordal.com",
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
              "description": "YAML-configured stock trading operations and data retrieval via generic API system"
            },
            {
              "name": "Performance Metrics",
              "description": "Performance monitoring, metrics collection and analysis"
            },
            {
              "name": "Generic API",
              "description": "Configuration-driven generic API endpoints"
            },
            {
              "name": "Configuration",
              "description": "API configuration management and introspection"
            },
            {
              "name": "Validation",
              "description": "Configuration validation and integrity checking"
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
            "/api/generic/stock-trades": {
              "get": {
                "tags": ["Stock Trades"],
                "summary": "Get all stock trades (Generic API)",
                "description": "Retrieve a paginated list of all stock trades using the YAML-configured generic API system",
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
                            "data": {
                              "type": "array",
                              "items": {
                                "type": "object",
                                "properties": {
                                  "id": {"type": "integer"},
                                  "symbol": {"type": "string"},
                                  "trade_type": {"type": "string"},
                                  "quantity": {"type": "integer"},
                                  "price": {"type": "number"},
                                  "total_value": {"type": "number"},
                                  "trade_date_time": {"type": "string"},
                                  "trader_id": {"type": "string"},
                                  "exchange": {"type": "string"}
                                }
                              }
                            },
                            "totalElements": {"type": "integer"},
                            "totalPages": {"type": "integer"},
                            "page": {"type": "integer"},
                            "size": {"type": "integer"}
                          }
                        }
                      }
                    }
                  }
                }
              }
            },
            "/api/generic/stock-trades/{id}": {
              "get": {
                "tags": ["Stock Trades"],
                "summary": "Get stock trade by ID",
                "description": "Retrieve a specific stock trade by its ID using the generic API system",
                "parameters": [
                  {
                    "name": "id",
                    "in": "path",
                    "required": true,
                    "description": "Stock trade ID",
                    "schema": {"type": "integer"}
                  }
                ],
                "responses": {
                  "200": {
                    "description": "Successfully retrieved stock trade",
                    "content": {
                      "application/json": {
                        "schema": {
                          "type": "object",
                          "properties": {
                            "data": {
                              "type": "array",
                              "items": {
                                "type": "object",
                                "properties": {
                                  "id": {"type": "integer"},
                                  "symbol": {"type": "string"},
                                  "trade_type": {"type": "string"},
                                  "quantity": {"type": "integer"},
                                  "price": {"type": "number"},
                                  "total_value": {"type": "number"},
                                  "trade_date_time": {"type": "string"},
                                  "trader_id": {"type": "string"},
                                  "exchange": {"type": "string"}
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  },
                  "404": {
                    "description": "Stock trade not found"
                  }
                }
              }
            },
            "/api/generic/stock-trades/symbol/{symbol}": {
              "get": {
                "tags": ["Stock Trades"],
                "summary": "Get stock trades by symbol",
                "description": "Retrieve stock trades for a specific symbol with pagination",
                "parameters": [
                  {
                    "name": "symbol",
                    "in": "path",
                    "required": true,
                    "description": "Stock symbol (e.g., AAPL, GOOGL)",
                    "schema": {"type": "string"}
                  },
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
                    "description": "Successfully retrieved stock trades for symbol"
                  }
                }
              }
            },
            "/api/generic/stock-trades/trader/{trader_id}": {
              "get": {
                "tags": ["Stock Trades"],
                "summary": "Get stock trades by trader",
                "description": "Retrieve stock trades for a specific trader with pagination",
                "parameters": [
                  {
                    "name": "trader_id",
                    "in": "path",
                    "required": true,
                    "description": "Trader ID",
                    "schema": {"type": "string"}
                  },
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
                    "description": "Successfully retrieved stock trades for trader"
                  }
                }
              }
            },
            "/api/generic/stock-trades/date-range": {
              "get": {
                "tags": ["Stock Trades"],
                "summary": "Get stock trades by date range",
                "description": "Retrieve stock trades within a specific date range with pagination",
                "parameters": [
                  {
                    "name": "start_date",
                    "in": "query",
                    "required": true,
                    "description": "Start date (yyyy-MM-dd HH:mm:ss)",
                    "schema": {"type": "string"}
                  },
                  {
                    "name": "end_date",
                    "in": "query",
                    "required": true,
                    "description": "End date (yyyy-MM-dd HH:mm:ss)",
                    "schema": {"type": "string"}
                  },
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
                    "description": "Successfully retrieved stock trades for date range"
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
            },
            "/api/generic/health": {
              "get": {
                "tags": ["Generic API"],
                "summary": "Generic API health check",
                "description": "Check the health status of the generic API service",
                "responses": {
                  "200": {
                    "description": "Generic API service is healthy",
                    "content": {
                      "application/json": {
                        "schema": {
                          "type": "object",
                          "properties": {
                            "status": {"type": "string", "example": "UP"},
                            "service": {"type": "string", "example": "Generic API Service"},
                            "availableEndpoints": {"type": "integer", "example": 5},
                            "timestamp": {"type": "integer", "example": 1751213478431}
                          }
                        }
                      }
                    }
                  }
                }
              }
            },
            "/api/generic/endpoints": {
              "get": {
                "tags": ["Generic API"],
                "summary": "Get available endpoints",
                "description": "Retrieve all available generic API endpoints with their configurations",
                "responses": {
                  "200": {
                    "description": "Successfully retrieved available endpoints",
                    "content": {
                      "application/json": {
                        "schema": {
                          "type": "object",
                          "properties": {
                            "totalEndpoints": {"type": "integer", "example": 5},
                            "endpoints": {
                              "type": "object",
                              "additionalProperties": {
                                "type": "object",
                                "properties": {
                                  "path": {"type": "string", "example": "/api/generic/stock-trades"},
                                  "method": {"type": "string", "example": "GET"},
                                  "description": {"type": "string", "example": "Get all stock trades with pagination"}
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            },
            "/api/generic/config": {
              "get": {
                "tags": ["Configuration"],
                "summary": "Get complete configuration",
                "description": "Retrieve complete API configuration including endpoints and queries",
                "responses": {
                  "200": {
                    "description": "Successfully retrieved complete configuration",
                    "content": {
                      "application/json": {
                        "schema": {
                          "type": "object",
                          "properties": {
                            "summary": {
                              "type": "object",
                              "properties": {
                                "totalEndpoints": {"type": "integer", "example": 5},
                                "totalQueries": {"type": "integer", "example": 11},
                                "timestamp": {"type": "integer", "example": 1751213478431}
                              }
                            },
                            "endpoints": {"type": "object"},
                            "queries": {"type": "object"}
                          }
                        }
                      }
                    }
                  }
                }
              }
            },
            "/api/generic/config/queries": {
              "get": {
                "tags": ["Configuration"],
                "summary": "Get all query configurations",
                "description": "Retrieve all available query configurations with SQL and parameters",
                "responses": {
                  "200": {
                    "description": "Successfully retrieved query configurations",
                    "content": {
                      "application/json": {
                        "schema": {
                          "type": "object",
                          "properties": {
                            "totalQueries": {"type": "integer", "example": 11},
                            "queries": {
                              "type": "object",
                              "additionalProperties": {
                                "type": "object",
                                "properties": {
                                  "name": {"type": "string", "example": "stock-trades-all"},
                                  "description": {"type": "string", "example": "Get All Stock Trades"},
                                  "sql": {"type": "string", "example": "SELECT * FROM stock_trades LIMIT ? OFFSET ?"}
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            },
            "/api/generic/config/queries/{queryName}": {
              "get": {
                "tags": ["Configuration"],
                "summary": "Get specific query configuration",
                "description": "Retrieve configuration for a specific named query",
                "parameters": [
                  {
                    "name": "queryName",
                    "in": "path",
                    "required": true,
                    "description": "Name of the query to retrieve",
                    "schema": {
                      "type": "string",
                      "example": "stock-trades-all"
                    }
                  }
                ],
                "responses": {
                  "200": {
                    "description": "Successfully retrieved query configuration",
                    "content": {
                      "application/json": {
                        "schema": {
                          "type": "object",
                          "properties": {
                            "name": {"type": "string", "example": "stock-trades-all"},
                            "description": {"type": "string", "example": "Get All Stock Trades"},
                            "sql": {"type": "string", "example": "SELECT * FROM stock_trades LIMIT ? OFFSET ?"},
                            "parameters": {
                              "type": "array",
                              "items": {
                                "type": "object",
                                "properties": {
                                  "name": {"type": "string"},
                                  "type": {"type": "string"},
                                  "required": {"type": "boolean"}
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  },
                  "404": {
                    "description": "Query not found",
                    "content": {
                      "application/json": {
                        "schema": {
                          "type": "object",
                          "properties": {
                            "error": {"type": "string", "example": "Query not found: nonexistent-query"}
                          }
                        }
                      }
                    }
                  }
                }
              }
            },
            "/api/generic/config/databases": {
              "get": {
                "tags": ["Configuration"],
                "summary": "Get all database configurations",
                "description": "Retrieve all available database configurations with connection details",
                "responses": {
                  "200": {
                    "description": "Successfully retrieved database configurations",
                    "content": {
                      "application/json": {
                        "schema": {
                          "type": "object",
                          "properties": {
                            "totalDatabases": {"type": "integer", "example": 2},
                            "databases": {
                              "type": "object",
                              "additionalProperties": {
                                "type": "object",
                                "properties": {
                                  "name": {"type": "string", "example": "stock-trades-db"},
                                  "description": {"type": "string", "example": "Primary database for stock trading application"},
                                  "url": {"type": "string", "example": "jdbc:h2:./data/stocktrades"},
                                  "driver": {"type": "string", "example": "org.h2.Driver"}
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            },
            "/api/generic/config/databases/{databaseName}": {
              "get": {
                "tags": ["Configuration"],
                "summary": "Get specific database configuration",
                "description": "Retrieve configuration for a specific named database",
                "parameters": [
                  {
                    "name": "databaseName",
                    "in": "path",
                    "required": true,
                    "description": "Name of the database to retrieve",
                    "schema": {
                      "type": "string",
                      "example": "stock-trades-db"
                    }
                  }
                ],
                "responses": {
                  "200": {
                    "description": "Successfully retrieved database configuration",
                    "content": {
                      "application/json": {
                        "schema": {
                          "type": "object",
                          "properties": {
                            "name": {"type": "string", "example": "stock-trades-db"},
                            "description": {"type": "string", "example": "Primary database for stock trading application"},
                            "url": {"type": "string", "example": "jdbc:h2:./data/stocktrades"},
                            "username": {"type": "string", "example": "sa"},
                            "driver": {"type": "string", "example": "org.h2.Driver"},
                            "pool": {
                              "type": "object",
                              "properties": {
                                "maximumPoolSize": {"type": "integer", "example": 10},
                                "minimumIdle": {"type": "integer", "example": 2},
                                "connectionTimeout": {"type": "integer", "example": 30000}
                              }
                            }
                          }
                        }
                      }
                    }
                  },
                  "404": {
                    "description": "Database not found",
                    "content": {
                      "application/json": {
                        "schema": {
                          "type": "object",
                          "properties": {
                            "error": {"type": "string", "example": "Database not found: nonexistent-db"}
                          }
                        }
                      }
                    }
                  }
                }
              }
            },
            "/api/generic/config/relationships": {
              "get": {
                "tags": ["Configuration"],
                "summary": "Get configuration relationships",
                "description": "Retrieve complete configuration relationships showing how endpoints connect to queries and databases",
                "responses": {
                  "200": {
                    "description": "Successfully retrieved configuration relationships",
                    "content": {
                      "application/json": {
                        "schema": {
                          "type": "object",
                          "properties": {
                            "summary": {
                              "type": "object",
                              "properties": {
                                "totalEndpoints": {"type": "integer", "example": 5},
                                "totalQueries": {"type": "integer", "example": 11},
                                "totalDatabases": {"type": "integer", "example": 2},
                                "timestamp": {"type": "integer", "example": 1751213478431}
                              }
                            },
                            "endpoints": {"type": "object"},
                            "queries": {"type": "object"},
                            "databases": {"type": "object"}
                          }
                        }
                      }
                    }
                  }
                }
              }
            },
            "/api/generic/config/validate": {
              "get": {
                "tags": ["Validation"],
                "summary": "Validate all configurations",
                "description": "Validate the integrity of all configuration files (endpoints, queries, databases, and relationships)",
                "responses": {
                  "200": {
                    "description": "Configuration validation results",
                    "content": {
                      "application/json": {
                        "schema": {
                          "type": "object",
                          "properties": {
                            "status": {"type": "string", "enum": ["VALID", "INVALID"], "example": "VALID"},
                            "message": {"type": "string", "example": "All configurations are valid"},
                            "errors": {"type": "array", "items": {"type": "string"}},
                            "warnings": {"type": "array", "items": {"type": "string"}},
                            "errorCount": {"type": "integer", "example": 0},
                            "warningCount": {"type": "integer", "example": 0},
                            "timestamp": {"type": "integer", "example": 1751213478431}
                          }
                        }
                      }
                    }
                  }
                }
              }
            },
            "/api/generic/config/validate/endpoints": {
              "get": {
                "tags": ["Validation"],
                "summary": "Validate endpoint configurations",
                "description": "Validate the integrity of endpoint configurations",
                "responses": {
                  "200": {
                    "description": "Endpoint validation results",
                    "content": {
                      "application/json": {
                        "schema": {
                          "type": "object",
                          "properties": {
                            "status": {"type": "string", "enum": ["VALID", "INVALID"], "example": "VALID"},
                            "errors": {"type": "array", "items": {"type": "string"}},
                            "warnings": {"type": "array", "items": {"type": "string"}},
                            "errorCount": {"type": "integer", "example": 0},
                            "warningCount": {"type": "integer", "example": 0},
                            "totalEndpoints": {"type": "integer", "example": 5},
                            "timestamp": {"type": "integer", "example": 1751213478431}
                          }
                        }
                      }
                    }
                  }
                }
              }
            },
            "/api/generic/config/validate/queries": {
              "get": {
                "tags": ["Validation"],
                "summary": "Validate query configurations",
                "description": "Validate the integrity of query configurations",
                "responses": {
                  "200": {
                    "description": "Query validation results",
                    "content": {
                      "application/json": {
                        "schema": {
                          "type": "object",
                          "properties": {
                            "status": {"type": "string", "enum": ["VALID", "INVALID"], "example": "VALID"},
                            "errors": {"type": "array", "items": {"type": "string"}},
                            "warnings": {"type": "array", "items": {"type": "string"}},
                            "errorCount": {"type": "integer", "example": 0},
                            "warningCount": {"type": "integer", "example": 0},
                            "totalQueries": {"type": "integer", "example": 11},
                            "timestamp": {"type": "integer", "example": 1751213478431}
                          }
                        }
                      }
                    }
                  }
                }
              }
            },
            "/api/generic/config/validate/databases": {
              "get": {
                "tags": ["Validation"],
                "summary": "Validate database configurations",
                "description": "Validate the integrity of database configurations",
                "responses": {
                  "200": {
                    "description": "Database validation results",
                    "content": {
                      "application/json": {
                        "schema": {
                          "type": "object",
                          "properties": {
                            "status": {"type": "string", "enum": ["VALID", "INVALID"], "example": "VALID"},
                            "errors": {"type": "array", "items": {"type": "string"}},
                            "warnings": {"type": "array", "items": {"type": "string"}},
                            "errorCount": {"type": "integer", "example": 0},
                            "warningCount": {"type": "integer", "example": 0},
                            "totalDatabases": {"type": "integer", "example": 2},
                            "timestamp": {"type": "integer", "example": 1751213478431}
                          }
                        }
                      }
                    }
                  }
                }
              }
            },
            "/api/generic/config/validate/relationships": {
              "get": {
                "tags": ["Validation"],
                "summary": "Validate configuration relationships",
                "description": "Validate the integrity of relationships between endpoints, queries, and databases",
                "responses": {
                  "200": {
                    "description": "Relationship validation results",
                    "content": {
                      "application/json": {
                        "schema": {
                          "type": "object",
                          "properties": {
                            "status": {"type": "string", "enum": ["VALID", "INVALID"], "example": "VALID"},
                            "errors": {"type": "array", "items": {"type": "string"}},
                            "warnings": {"type": "array", "items": {"type": "string"}},
                            "errorCount": {"type": "integer", "example": 0},
                            "warningCount": {"type": "integer", "example": 0},
                            "timestamp": {"type": "integer", "example": 1751213478431}
                          }
                        }
                      }
                    }
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
            genericApiConfig.getServerHost(),
            genericApiConfig.getServerPort());

        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>CORDAL - API Documentation</title>
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

                <h1>🚀 Javalin API Mesh</h1>
                <p>High-performance generic API system with YAML-configured endpoints, comprehensive performance monitoring and dual dashboard architecture.</p>

                <h2>🏥 Health & System</h2>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/health</span>
                    <div class="description">Check the health status of the application</div>
                </div>

                <h2>📈 Stock Trades API (Generic System)</h2>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/generic/stock-trades</span>
                    <span class="badge">Paginated</span>
                    <span class="badge">YAML-Configured</span>
                    <div class="description">Get all stock trades with pagination support. Supports async processing. Configured via YAML.</div>
                </div>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/generic/stock-trades/{id}</span>
                    <span class="badge">YAML-Configured</span>
                    <div class="description">Get a specific stock trade by ID using the generic API system</div>
                </div>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/generic/stock-trades/symbol/{symbol}</span>
                    <span class="badge">Paginated</span>
                    <span class="badge">YAML-Configured</span>
                    <div class="description">Get stock trades by symbol with pagination</div>
                </div>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/generic/stock-trades/trader/{trader_id}</span>
                    <span class="badge">Paginated</span>
                    <span class="badge">YAML-Configured</span>
                    <div class="description">Get stock trades by trader ID with pagination</div>
                </div>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/generic/stock-trades/date-range</span>
                    <span class="badge">Paginated</span>
                    <span class="badge">YAML-Configured</span>
                    <div class="description">Get stock trades by date range with pagination</div>
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

                <h2>🔧 Generic API System</h2>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/generic/health</span>
                    <div class="description">Generic API health check with endpoint count</div>
                </div>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/generic/endpoints</span>
                    <div class="description">Get all available generic API endpoints with their configurations</div>
                </div>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/generic/endpoints/{endpointName}</span>
                    <div class="description">Get specific endpoint configuration by name</div>
                </div>

                <h2>⚙️ Configuration Management</h2>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/generic/config</span>
                    <div class="description">Get complete API configuration (endpoints + queries + databases)</div>
                </div>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/generic/config/queries</span>
                    <div class="description">Get all query configurations</div>
                </div>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/generic/config/queries/{queryName}</span>
                    <div class="description">Get specific query configuration by name</div>
                </div>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/generic/config/databases</span>
                    <div class="description">Get all database configurations</div>
                </div>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/generic/config/databases/{databaseName}</span>
                    <div class="description">Get specific database configuration by name</div>
                </div>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/generic/config/relationships</span>
                    <div class="description">Get configuration relationships between endpoints, queries, and databases</div>
                </div>

                <h2>✅ Configuration Validation</h2>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/generic/config/validate</span>
                    <span class="badge">Validation</span>
                    <div class="description">Validate all configurations (endpoints, queries, databases, relationships)</div>
                </div>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/generic/config/validate/endpoints</span>
                    <span class="badge">Validation</span>
                    <div class="description">Validate endpoint configurations</div>
                </div>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/generic/config/validate/queries</span>
                    <span class="badge">Validation</span>
                    <div class="description">Validate query configurations</div>
                </div>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/generic/config/validate/databases</span>
                    <span class="badge">Validation</span>
                    <div class="description">Validate database configurations</div>
                </div>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/generic/config/validate/relationships</span>
                    <span class="badge">Validation</span>
                    <div class="description">Validate configuration relationships and integrity</div>
                </div>

                <h2>🔍 Granular Configuration APIs</h2>

                <h3>📋 Endpoint Configuration Details</h3>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/generic/config/endpoints/schema</span>
                    <span class="badge">Schema</span>
                    <div class="description">Get endpoint configuration schema (field names and data types)</div>
                </div>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/generic/config/endpoints/parameters</span>
                    <span class="badge">Parameters</span>
                    <div class="description">Get only parameters from all endpoint configurations</div>
                </div>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/generic/config/endpoints/database-connections</span>
                    <span class="badge">Connections</span>
                    <div class="description">Get database connections referenced by endpoints (via queries)</div>
                </div>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/generic/config/endpoints/summary</span>
                    <span class="badge">Summary</span>
                    <div class="description">Get endpoint configuration summary with statistics</div>
                </div>

                <h3>📝 Query Configuration Details</h3>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/generic/config/queries/schema</span>
                    <span class="badge">Schema</span>
                    <div class="description">Get query configuration schema (field names and data types)</div>
                </div>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/generic/config/queries/parameters</span>
                    <span class="badge">Parameters</span>
                    <div class="description">Get only parameters from all query configurations</div>
                </div>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/generic/config/queries/database-connections</span>
                    <span class="badge">Connections</span>
                    <div class="description">Get database connections referenced by queries</div>
                </div>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/generic/config/queries/summary</span>
                    <span class="badge">Summary</span>
                    <div class="description">Get query configuration summary with statistics</div>
                </div>

                <h3>🗄️ Database Configuration Details</h3>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/generic/config/databases/schema</span>
                    <span class="badge">Schema</span>
                    <div class="description">Get database configuration schema (field names and data types)</div>
                </div>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/generic/config/databases/parameters</span>
                    <span class="badge">Parameters</span>
                    <div class="description">Get only connection parameters (pool settings) from database configurations</div>
                </div>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/generic/config/databases/connections</span>
                    <span class="badge">Connections</span>
                    <div class="description">Get only connection details from database configurations</div>
                </div>
                <div class="endpoint">
                    <span class="method get">GET</span>
                    <span class="url">%s/api/generic/config/databases/summary</span>
                    <span class="badge">Summary</span>
                    <div class="description">Get database configuration summary with statistics</div>
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
        """.formatted(baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl);
    }

    /**
     * Get Swagger UI URL
     */
    public String getSwaggerUrl() {
        return String.format("http://%s:%d/swagger",
            genericApiConfig.getServerHost(),
            genericApiConfig.getServerPort());
    }

    /**
     * Get API Docs URL
     */
    public String getApiDocsUrl() {
        return String.format("http://%s:%d/api-docs",
            genericApiConfig.getServerHost(),
            genericApiConfig.getServerPort());
    }

    /**
     * Get OpenAPI JSON URL
     */
    public String getOpenApiJsonUrl() {
        return String.format("http://%s:%d/openapi.json",
            genericApiConfig.getServerHost(),
            genericApiConfig.getServerPort());
    }
}
