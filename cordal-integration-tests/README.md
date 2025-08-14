# CORDAL Integration Tests & Examples

## 🎯 **Purpose**

This module contains:
1. **Integration tests** for the CORDAL framework
2. **Example implementations** demonstrating how to use CORDAL for specific domains
3. **Template code** that you can copy and adapt for your own use cases

## 🚨 **IMPORTANT: These Are Examples, Not Core Features**

**Everything in this module is EXAMPLE CODE** that demonstrates how to implement domain-specific functionality using the CORDAL framework. **None of this code is part of the core CORDAL system.**

## 📁 **Directory Structure**

```
cordal-integration-tests/
├── src/test/java/dev/cordal/integration/
│   ├── examples/                          # 📋 EXAMPLE implementations
│   │   ├── StockTradesLegacyRoutes.java   # Example: Custom route registration
│   │   ├── StockTradesSchemaManager.java  # Example: Database schema management
│   │   ├── StockTradesTableCreator.java   # Example: Table creation utility
│   │   ├── StockTradesChecker.java        # Example: Database validation
│   │   ├── StockTradesInitializer.java    # Example: Data initialization
│   │   └── StockTrades*.java              # Example: Various utilities
│   └── tests/                             # 🧪 Integration tests
├── src/test/resources/
│   ├── config/                            # 📋 EXAMPLE configurations
│   │   ├── stocktrades-databases.yml      # Example: Database configuration
│   │   ├── stocktrades-queries.yml        # Example: Query definitions
│   │   ├── stocktrades-api-endpoints.yml  # Example: API endpoint configuration
│   │   ├── postgres-databases.yml         # Example: PostgreSQL configuration
│   │   └── postgres-queries.yml           # Example: PostgreSQL queries
│   └── sql/                               # 📋 EXAMPLE SQL scripts
│       ├── stocktrades-init.sql           # Example: Database initialization
│       ├── setup-stocktrades-database.sql # Example: Database setup
│       └── postgres-*.sql                 # Example: PostgreSQL scripts
└── data/                                  # 🗄️ Test database files
    └── stocktrades.mv.db                  # Example: H2 database file
```

## 📋 **Stock Trades Example**

### **What It Demonstrates**

The stock trades example shows how to implement a complete use case with CORDAL:

1. **Database Configuration** - How to configure external databases
2. **Query Definition** - How to define parameterized SQL queries
3. **API Endpoints** - How to configure REST endpoints with pagination
4. **Schema Management** - How to create and manage database schemas
5. **Data Initialization** - How to populate databases with sample data
6. **Integration Testing** - How to test the complete system

### **Key Example Files**

#### **Database Configuration** (`stocktrades-databases.yml`)
```yaml
databases:
  stocktrades:
    name: "stocktrades"
    description: "EXAMPLE: External database for stock trading data"
    url: "jdbc:h2:../data/stocktrades;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1"
    username: "sa"
    password: ""
    driver: "org.h2.Driver"
```

#### **Query Configuration** (`stocktrades-queries.yml`)
```yaml
queries:
  stock-trades-all:
    name: "stock-trades-all"
    description: "EXAMPLE: Get All Stock Trades with pagination"
    database: "stocktrades"
    sql: |
      SELECT id, symbol, trade_type, quantity, price, total_value,
             trade_date_time, trader_id, exchange
      FROM stock_trades
      ORDER BY trade_date_time DESC
      LIMIT ? OFFSET ?
```

#### **API Configuration** (`stocktrades-api-endpoints.yml`)
```yaml
endpoints:
  stock-trades-list:
    path: "/api/generic/stock-trades"
    method: "GET"
    description: "EXAMPLE: Get all stock trades with pagination"
    query: "stock-trades-all"
    countQuery: "stock-trades-count"
    pagination:
      enabled: true
      defaultSize: 20
      maxSize: 100
```

## 🛠️ **How to Use These Examples**

### **1. Study the Pattern**
- Examine how databases, queries, and endpoints are configured
- Understand the relationship between configuration files
- See how Java classes support the configuration

### **2. Copy and Adapt**
- Copy the example files to your own project
- Replace "stock_trades" with your domain entities
- Update table names, column names, and business logic
- Modify endpoints to match your API requirements

### **3. Example Adaptation**

**From Stock Trades**:
```yaml
queries:
  stock-trades-all:
    database: "stocktrades"
    sql: "SELECT id, symbol, trade_type FROM stock_trades"
```

**To Your Domain**:
```yaml
queries:
  products-all:
    database: "inventory"
    sql: "SELECT id, name, category FROM products"
```

## 🧪 **Running Integration Tests**

```bash
# Run all integration tests
mvn test

# Run specific test class
mvn test -Dtest="StockTradesIntegrationTest"

# Run with specific profile
mvn test -Dspring.profiles.active=integration
```

## 📝 **Creating Your Own Examples**

When creating examples for your domain:

1. **Follow the naming pattern**: `YourDomain*.java`
2. **Use clear documentation**: Mark everything as "EXAMPLE IMPLEMENTATION"
3. **Keep it in integration tests**: Don't put domain-specific code in core modules
4. **Provide complete examples**: Include databases, queries, endpoints, and tests
5. **Document the purpose**: Explain what the example demonstrates

## 🚨 **What NOT to Do**

- ❌ Don't put domain-specific code in core modules (`cordal-api-service`, `cordal-common-library`)
- ❌ Don't put example configurations in `generic-config/`
- ❌ Don't treat examples as part of the core framework
- ❌ Don't assume stock trades functionality is required for CORDAL to work

## ✅ **What TO Do**

- ✅ Keep all domain-specific code in `cordal-integration-tests`
- ✅ Clearly mark everything as "EXAMPLE IMPLEMENTATION"
- ✅ Use examples as templates for your own implementations
- ✅ Understand that CORDAL core is completely generic

## 🎯 **Remember**

**CORDAL is a generic framework. Stock trades is just one example of how to use it. Your implementation will be completely different and specific to your domain!**

The goal is to show you the patterns and best practices, not to provide a stock trading system. Use these examples as a starting point for building your own amazing applications with CORDAL! 🚀
