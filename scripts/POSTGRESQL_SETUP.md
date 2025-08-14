# PostgreSQL Setup Instructions

This document provides instructions for setting up PostgreSQL to work with the Generic API Service.

## Prerequisites

1. PostgreSQL server installed and running on localhost:5432
2. PostgreSQL client tools (psql) available
3. Database user with appropriate permissions

## Database Setup

### Step 1: Create the stocktrades database

Connect to PostgreSQL as a superuser and create the database:

```bash
# Connect to PostgreSQL (adjust connection parameters as needed)
psql -h localhost -U postgres

# Create the stocktrades database
CREATE DATABASE stocktrades;

# Exit psql
\q
```

### Step 2: Initialize the stock_trades table

Run the initialization script to create the table and insert sample data:

```bash
# Run the initialization script
psql -h localhost -U postgres -d stocktrades -f scripts/postgres-init.sql
```

Alternatively, you can run the script manually:

```bash
# Connect to the stocktrades database
psql -h localhost -U postgres -d stocktrades

# Copy and paste the contents of scripts/postgres-init.sql
# Or use \i command to execute the file
\i scripts/postgres-init.sql

# Verify the setup
SELECT COUNT(*) FROM stock_trades;
SELECT DISTINCT symbol FROM stock_trades ORDER BY symbol;

# Exit psql
\q
```

## Configuration

The PostgreSQL database configuration is defined in:
- **Database**: `generic-config/postgres-databases.yml`
- **Queries**: `generic-config/postgres-queries.yml`
- **Endpoints**: `generic-config/postgres-endpoints.yml`

### Default Connection Settings

- **Host**: localhost
- **Port**: 5432
- **Database**: stocktrades
- **Username**: postgres
- **Password**: postgres

Update the connection settings in `generic-config/postgres-databases.yml` if your PostgreSQL setup uses different credentials.

## Verification

After setup, you can verify the configuration by:

1. **Running the Generic API Service**:
   ```bash
   cd cordal-api-service
   mvn exec:java
   ```

2. **Checking the validation logs**: The service will validate that the PostgreSQL database and tables are accessible.

3. **Testing the API endpoints**: Once the service starts, you can test the PostgreSQL endpoints:
   - `GET /api/postgres/trades` - List all trades
   - `GET /api/postgres/trades/count` - Count of trades
   - `GET /api/postgres/trades/symbol/AAPL` - Trades for AAPL symbol

## Troubleshooting

### Connection Issues

If you encounter connection issues:

1. **Check PostgreSQL is running**:
   ```bash
   # On Windows
   net start postgresql-x64-14
   
   # On Linux/Mac
   sudo systemctl status postgresql
   ```

2. **Verify connection settings**:
   ```bash
   psql -h localhost -U postgres -d stocktrades -c "SELECT 1;"
   ```

3. **Check firewall settings**: Ensure port 5432 is accessible.

### Authentication Issues

If you get authentication errors:

1. **Update pg_hba.conf** to allow local connections
2. **Set password for postgres user**:
   ```sql
   ALTER USER postgres PASSWORD 'postgres';
   ```

### Table Not Found

If you get "table does not exist" errors:

1. **Verify you're connected to the right database**:
   ```sql
   SELECT current_database();
   ```

2. **Check if table exists**:
   ```sql
   \dt
   SELECT * FROM information_schema.tables WHERE table_name = 'stock_trades';
   ```

3. **Re-run the initialization script** if the table is missing.

## Schema Information

The `stock_trades` table has the following structure:

```sql
CREATE TABLE stock_trades (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(10) NOT NULL,
    trade_type VARCHAR(4) NOT NULL CHECK (trade_type IN ('BUY', 'SELL')),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    price DECIMAL(10,2) NOT NULL CHECK (price > 0),
    total_value DECIMAL(15,2) NOT NULL,
    trade_date_time TIMESTAMP NOT NULL,
    trader_id VARCHAR(50) NOT NULL,
    exchange VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

The table includes indexes on commonly queried columns for better performance.
