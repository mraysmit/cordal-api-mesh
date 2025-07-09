# PostgreSQL Database Setup for postgres-trades

This directory contains SQL scripts and utilities to set up the PostgreSQL database required for the Generic API Service `postgres-trades` configuration.

## 📋 Prerequisites

1. **PostgreSQL Server**: PostgreSQL must be installed and running
   - Download from: https://www.postgresql.org/download/
   - Ensure PostgreSQL service is running
   - Default port 5432 should be available

2. **PostgreSQL User**: User `postgres` with password `postgres` must exist
   - This is typically created during PostgreSQL installation
   - Or create manually: `CREATE USER postgres WITH PASSWORD 'postgres' SUPERUSER;`

3. **Network Access**: PostgreSQL must accept connections on localhost:5432
   - Check `postgresql.conf` and `pg_hba.conf` if needed

## 🚀 Quick Setup (Recommended)

### Option 1: Automated Setup (Windows)
```bash
# Run from project root directory
scripts\setup-postgres-database.bat
```

### Option 2: Manual Setup
```bash
# 1. Create database and setup tables
psql -U postgres -h localhost -f scripts/postgres-setup.sql

# OR if database already exists:
psql -U postgres -h localhost -d postgres-trades -f scripts/postgres-setup-simple.sql
```

## 📁 Files Description

| File | Description |
|------|-------------|
| `postgres-setup.sql` | Complete setup script (creates database + tables + data) |
| `postgres-setup-simple.sql` | Table setup only (assumes database exists) |
| `setup-postgres-database.bat` | Windows batch script for automated setup |
| `PostgreSQL-Setup-README.md` | This documentation file |

## 🗃️ Database Schema

### Table: `stock_trades`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGSERIAL | PRIMARY KEY | Auto-incrementing trade ID |
| `symb` | VARCHAR(10) | NOT NULL | Stock symbol (e.g., 'AAPL', 'GOOGL') |
| `trade_type` | VARCHAR(4) | NOT NULL, CHECK | Trade type ('BUY' or 'SELL') |
| `quantity` | INTEGER | NOT NULL, > 0 | Number of shares traded |
| `price` | DECIMAL(10,2) | NOT NULL, > 0 | Price per share |
| `total_val` | DECIMAL(15,2) | NOT NULL, > 0 | Total trade value (quantity × price) |
| `trade_date_time` | TIMESTAMP | NOT NULL | When the trade occurred |
| `trader_id` | VARCHAR(50) | NOT NULL | ID of the trader |
| `exchange` | VARCHAR(20) | NOT NULL | Exchange name (e.g., 'NASDAQ', 'NYSE') |
| `created_at` | TIMESTAMP | DEFAULT NOW() | Record creation timestamp |
| `updated_at` | TIMESTAMP | DEFAULT NOW() | Record update timestamp (auto-updated) |

### Indexes Created
- `idx_stock_trades_symb` - For symbol-based queries
- `idx_stock_trades_trader_id` - For trader-based queries  
- `idx_stock_trades_trade_date_time` - For date range queries
- `idx_stock_trades_exchange` - For exchange-based queries
- `idx_stock_trades_trade_type` - For trade type queries

## ⚠️ Important: Column Name Differences

**Note**: There's a discrepancy between the H2 database schema and PostgreSQL query expectations:

| H2 Column Name | PostgreSQL Query Expects | Solution |
|----------------|-------------------------|----------|
| `SYMBOL` | `symb` | PostgreSQL table uses `symb` |
| `TOTAL_VALUE` | `total_val` | PostgreSQL table uses `total_val` |

The PostgreSQL setup uses the column names expected by the queries in `postgres-queries.yml` to ensure compatibility.

## 📊 Sample Data

The setup scripts insert 20 sample stock trades including:
- **Technology stocks**: AAPL, GOOGL, MSFT, TSLA, NVDA, AMD
- **Financial stocks**: JPM, BAC, WFC  
- **Healthcare stocks**: JNJ, PFE
- **Energy stocks**: XOM, CVX
- **Consumer goods**: KO, PG
- **Multiple exchanges**: NASDAQ, NYSE
- **Various traders**: TRADER_001 through TRADER_008
- **Date range**: 2024-01-01 to 2024-01-02

## 🔧 Verification

After setup, verify the installation:

```sql
-- Connect to the database
psql -U postgres -h localhost -d postgres-trades

-- Check table structure
\d stock_trades

-- Check sample data
SELECT COUNT(*) FROM stock_trades;
SELECT DISTINCT symb FROM stock_trades ORDER BY symb;
SELECT DISTINCT exchange FROM stock_trades;
```

## 🧪 Testing with Generic API Service

After PostgreSQL setup, test the connection:

```bash
# Run the bootstrap demo
scripts\run-bootstrap-demo.bat

# Or start the service directly
cd generic-api-service
mvn exec:java
```

The bootstrap should now show:
```
| postgres-trades      | OK       | Connected and ready                              |
```

## 🌐 API Endpoints Available

Once PostgreSQL is set up, these endpoints will be functional:

- `GET /api/postgres/trades` - List all trades
- `GET /api/postgres/trades/count` - Get trade count
- `GET /api/postgres/trades/symbol/{symbol}` - Get trades by symbol
- `GET /api/postgres/trades/{id}` - Get trade by ID
- `GET /api/postgres/trades/trader/{trader_id}` - Get trades by trader
- `GET /api/postgres/trades/date-range` - Get trades by date range
- `GET /api/postgres/trades/exchange/{exchange}` - Get trades by exchange
- `GET /api/postgres/trades/recent` - Get recent trades
- `GET /api/postgres/trades/type/{trade_type}` - Get trades by type

## 🔍 Troubleshooting

### Connection Issues
```bash
# Test PostgreSQL connection
psql -U postgres -h localhost -c "SELECT version();"
```

### Permission Issues
```sql
-- Grant permissions if needed
GRANT ALL PRIVILEGES ON DATABASE "postgres-trades" TO postgres;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO postgres;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO postgres;
```

### Port Issues
```bash
# Check if PostgreSQL is running on port 5432
netstat -an | findstr 5432
```

### Service Issues
```bash
# Windows: Check PostgreSQL service
sc query postgresql-x64-15

# Start service if needed
net start postgresql-x64-15
```

## 📝 Configuration

The PostgreSQL database configuration is defined in:
- `generic-config/postgres-databases.yml` - Database connection settings
- `generic-config/postgres-queries.yml` - SQL queries for API endpoints
- `generic-config/postgres-endpoints.yml` - API endpoint definitions

## ✅ Success Criteria

Setup is successful when:
1. ✅ PostgreSQL server is running
2. ✅ Database `postgres-trades` exists
3. ✅ Table `stock_trades` exists with correct schema
4. ✅ Sample data is loaded (20 records)
5. ✅ Bootstrap demo shows postgres-trades as "OK"
6. ✅ API endpoints return data without errors

## 🆘 Support

If you encounter issues:
1. Check PostgreSQL service is running
2. Verify connection parameters in `postgres-databases.yml`
3. Ensure user `postgres` has correct permissions
4. Check PostgreSQL logs for detailed error messages
5. Run the verification queries to confirm setup
