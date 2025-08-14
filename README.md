# CORDAL - Configuration Orchestrated REST Dynamic API Layer

## 🚀 **What is CORDAL?**

CORDAL is a **generic, configuration-driven REST API framework** built on Java 21 and Javalin 6.1.3. It enables you to create dynamic REST APIs for **any domain** through YAML configuration files rather than hardcoded endpoints.

## 🏗️ **CRITICAL: Core vs. Example Architecture**

### **CORDAL CORE SYSTEM** (Generic Framework)
```
cordal-api-service/          # ✅ Generic REST API framework
cordal-common-library/       # ✅ Shared utilities and models  
cordal-metrics-service/      # ✅ Performance monitoring
generic-config/              # ✅ Core configuration (generic only)
scripts/                     # ✅ Build and deployment scripts
```

### **EXAMPLE IMPLEMENTATIONS** (Domain-Specific)
```
cordal-integration-tests/
├── src/test/java/dev/cordal/integration/examples/     # 📋 Stock trades example classes
├── src/test/resources/config/                         # 📋 Stock trades example configs
└── src/test/resources/sql/                           # 📋 Stock trades example SQL
```

### **🎯 Key Principle**
- **CORE SYSTEM**: Completely generic and domain-agnostic
- **STOCK TRADES**: Pure example to demonstrate framework usage
- **YOUR IMPLEMENTATION**: Replace stock trades with your actual domain

## 🚨 **Important Notice**

**Stock trades functionality is NOT part of the core system!** It's an example implementation used for:
- Demonstrating framework capabilities
- Integration testing
- Providing implementation templates
- Showing best practices

When building your application, replace all stock trades references with your actual domain entities.

## ⚡ **Quick Start**

### 1. **Core System Setup**
```bash
# Clone the repository
git clone <repository-url>
cd cordal-api-mesh

# Build the core system
mvn clean install

# Run the generic API service
cd cordal-api-service
mvn spring-boot:run
```

### 2. **Configure Your Domain**
Create your domain-specific configuration files in `generic-config/`:

**your-domain-databases.yml**:
```yaml
databases:
  your_database:
    name: "your_database"
    url: "jdbc:h2:./data/your-data"
    username: "sa"
    password: ""
    driver: "org.h2.Driver"
```

**your-domain-queries.yml**:
```yaml
queries:
  get_your_entities:
    database: "your_database"
    sql: "SELECT * FROM your_table ORDER BY created_date DESC"
    parameters: []
```

**your-domain-endpoints.yml**:
```yaml
endpoints:
  your_entities_list:
    path: "/api/your-entities"
    method: "GET"
    query: "get_your_entities"
    pagination:
      enabled: true
      defaultSize: 20
```

### 3. **Access Your APIs**
```bash
# Your configured endpoints
curl http://localhost:8080/api/your-entities

# Core system endpoints
curl http://localhost:8080/api/health
curl http://localhost:8080/dashboard
```

## 📚 **Documentation**

- **[Comprehensive Guide](docs/CORDAL_COMPREHENSIVE_GUIDE.md)** - Complete framework documentation
- **[API Reference](docs/API_REFERENCE.md)** - Detailed API documentation
- **[Configuration Guide](docs/CONFIGURATION_GUIDE.md)** - Configuration examples and best practices

## 🧪 **Example Implementation**

The stock trades example in `cordal-integration-tests/` demonstrates:
- Database configuration and connection management
- Query definition and parameter handling
- REST endpoint configuration and routing
- Pagination and response formatting
- Integration testing patterns

**Use it as a template** for your own domain implementation.

## 🛠️ **Technology Stack**

- **Java 21** - Modern Java features and performance
- **Javalin 6.1.3** - Lightweight web framework
- **H2/PostgreSQL** - Database support with connection pooling
- **HikariCP** - High-performance connection pooling
- **Jackson** - JSON processing
- **SLF4J + Logback** - Comprehensive logging
- **Maven** - Build and dependency management

## 🏃‍♂️ **Development Workflow**

1. **Define your domain** (replace stock trades examples)
2. **Configure databases** in `generic-config/your-domain-databases.yml`
3. **Define queries** in `generic-config/your-domain-queries.yml`
4. **Configure endpoints** in `generic-config/your-domain-endpoints.yml`
5. **Run and test** your APIs
6. **Monitor performance** via the dashboard

## 📊 **Built-in Features**

- ✅ **Configuration-driven** - No hardcoded endpoints
- ✅ **Automatic metrics** - Zero-code performance monitoring
- ✅ **Multi-database support** - H2, PostgreSQL, and more
- ✅ **Connection pooling** - Production-ready database connections
- ✅ **Real-time dashboard** - Performance monitoring with charts
- ✅ **Health checks** - Application and database health monitoring
- ✅ **Comprehensive validation** - Configuration validation with detailed errors
- ✅ **Production ready** - Multiple deployment profiles and monitoring

## 🤝 **Contributing**

When contributing to CORDAL:
1. **Keep the core system generic** - No domain-specific code in core modules
2. **Add examples to integration tests** - Domain-specific examples belong in `cordal-integration-tests/`
3. **Update documentation** - Clearly distinguish between core framework and examples
4. **Follow the architecture** - Maintain separation between generic framework and specific implementations

## 📄 **License**

[Add your license information here]

---

**Remember**: CORDAL is a generic framework. Stock trades is just an example. Build amazing APIs for YOUR domain! 🚀
