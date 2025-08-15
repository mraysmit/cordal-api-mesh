@echo off
REM ================================================================================
REM Setup postgres-trades H2 Database
REM ================================================================================
REM This script initializes the postgres-trades database using H2 (PostgreSQL-compatible)
REM ================================================================================

echo.
echo ================================================================================
echo Setting up postgres-trades H2 Database
echo ================================================================================
echo.

REM Set H2 jar path
set H2_JAR=%USERPROFILE%\.m2\repository\com\h2database\h2\2.2.224\h2-2.2.224.jar

REM Check if H2 jar exists
if not exist "%H2_JAR%" (
    echo ERROR: H2 jar not found at %H2_JAR%
    echo Please ensure H2 is available in your Maven repository
    pause
    exit /b 1
)

echo [INFO] Using H2 jar: %H2_JAR%
echo.

REM Database connection parameters
set DB_URL=jdbc:h2:tcp://localhost:9092/./data/postgres-trades;DB_CLOSE_DELAY=-1
set DB_USER=sa
set DB_PASSWORD=

echo [INFO] Database connection:
echo   URL: %DB_URL%
echo   User: %DB_USER%
echo.

echo [SETUP] Initializing postgres-trades database...
echo.

REM Create a temporary SQL file with all commands
echo -- Initialize postgres-trades H2 database > temp_postgres_trades_init.sql
echo DROP TABLE IF EXISTS stock_trades; >> temp_postgres_trades_init.sql
echo. >> temp_postgres_trades_init.sql
echo CREATE TABLE IF NOT EXISTS stock_trades ( >> temp_postgres_trades_init.sql
echo     id BIGINT AUTO_INCREMENT PRIMARY KEY, >> temp_postgres_trades_init.sql
echo     symb VARCHAR(10) NOT NULL, >> temp_postgres_trades_init.sql
echo     trade_type VARCHAR(4) NOT NULL CHECK (trade_type IN ('BUY', 'SELL')), >> temp_postgres_trades_init.sql
echo     quantity INTEGER NOT NULL CHECK (quantity ^> 0), >> temp_postgres_trades_init.sql
echo     price DECIMAL(10, 2) NOT NULL CHECK (price ^> 0), >> temp_postgres_trades_init.sql
echo     total_val DECIMAL(15, 2) NOT NULL, >> temp_postgres_trades_init.sql
echo     trade_date_time TIMESTAMP NOT NULL, >> temp_postgres_trades_init.sql
echo     trader_id VARCHAR(50) NOT NULL, >> temp_postgres_trades_init.sql
echo     exchange VARCHAR(20) NOT NULL, >> temp_postgres_trades_init.sql
echo     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, >> temp_postgres_trades_init.sql
echo     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP >> temp_postgres_trades_init.sql
echo ); >> temp_postgres_trades_init.sql
echo. >> temp_postgres_trades_init.sql
echo CREATE INDEX IF NOT EXISTS idx_stock_trades_symb ON stock_trades(symb); >> temp_postgres_trades_init.sql
echo CREATE INDEX IF NOT EXISTS idx_stock_trades_trader_id ON stock_trades(trader_id); >> temp_postgres_trades_init.sql
echo CREATE INDEX IF NOT EXISTS idx_stock_trades_trade_date_time ON stock_trades(trade_date_time); >> temp_postgres_trades_init.sql
echo CREATE INDEX IF NOT EXISTS idx_stock_trades_exchange ON stock_trades(exchange); >> temp_postgres_trades_init.sql
echo CREATE INDEX IF NOT EXISTS idx_stock_trades_trade_type ON stock_trades(trade_type); >> temp_postgres_trades_init.sql
echo. >> temp_postgres_trades_init.sql

REM Add sample data
echo INSERT INTO stock_trades (symb, trade_type, quantity, price, total_val, trade_date_time, trader_id, exchange) VALUES >> temp_postgres_trades_init.sql
echo ('AAPL', 'BUY', 100, 150.50, 15050.00, '2024-01-01 09:30:00', 'TRADER_001', 'NASDAQ'), >> temp_postgres_trades_init.sql
echo ('GOOGL', 'SELL', 50, 2500.75, 125037.50, '2024-01-01 09:35:00', 'TRADER_002', 'NYSE'), >> temp_postgres_trades_init.sql
echo ('AAPL', 'SELL', 75, 151.25, 11343.75, '2024-01-01 09:40:00', 'TRADER_003', 'NASDAQ'), >> temp_postgres_trades_init.sql
echo ('MSFT', 'BUY', 200, 380.25, 76050.00, '2024-01-01 09:45:00', 'TRADER_001', 'NASDAQ'), >> temp_postgres_trades_init.sql
echo ('TSLA', 'BUY', 150, 245.80, 36870.00, '2024-01-01 09:50:00', 'TRADER_004', 'NASDAQ'), >> temp_postgres_trades_init.sql
echo ('AMZN', 'SELL', 30, 3200.00, 96000.00, '2024-01-01 10:00:00', 'TRADER_005', 'NASDAQ'), >> temp_postgres_trades_init.sql
echo ('NVDA', 'BUY', 80, 520.25, 41620.00, '2024-01-01 10:15:00', 'TRADER_001', 'NASDAQ'), >> temp_postgres_trades_init.sql
echo ('META', 'SELL', 60, 350.75, 21045.00, '2024-01-01 10:30:00', 'TRADER_003', 'NASDAQ'), >> temp_postgres_trades_init.sql
echo ('NFLX', 'BUY', 40, 480.50, 19220.00, '2024-01-01 10:45:00', 'TRADER_002', 'NASDAQ'), >> temp_postgres_trades_init.sql
echo ('AMD', 'SELL', 120, 125.00, 15000.00, '2024-01-01 11:00:00', 'TRADER_004', 'NASDAQ'); >> temp_postgres_trades_init.sql

echo. >> temp_postgres_trades_init.sql
echo SELECT 'postgres-trades database initialized successfully!' AS status; >> temp_postgres_trades_init.sql
echo SELECT 'Total records: ' ^|^| COUNT(*) AS summary FROM stock_trades; >> temp_postgres_trades_init.sql

REM Execute the SQL using H2's RunScript tool
java -cp "%H2_JAR%" org.h2.tools.RunScript -url "%DB_URL%" -user "%DB_USER%" -password "%DB_PASSWORD%" -script "temp_postgres_trades_init.sql"

if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to initialize postgres-trades database
    del temp_postgres_trades_init.sql
    pause
    exit /b 1
)

REM Clean up temporary file
del temp_postgres_trades_init.sql

echo.
echo [OK] postgres-trades database setup completed successfully!
echo.
echo ================================================================================
echo postgres-trades H2 Database Ready!
echo ================================================================================
echo.
echo Database Details:
echo   Database Name: postgres-trades
echo   Connection URL: %DB_URL%
echo   Username: %DB_USER%
echo   Password: [empty]
echo.
echo The postgres-trades database is now ready for use!
echo.
pause
