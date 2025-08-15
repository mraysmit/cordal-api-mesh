@echo off
REM ================================================================================
REM Setup Stock Trades H2 Database
REM ================================================================================

echo.
echo ================================================================================
echo Setting up Stock Trades H2 Database
echo ================================================================================
echo.

REM Set H2 jar path
set H2_JAR=%USERPROFILE%\.m2\repository\com\h2database\h2\2.2.224\h2-2.2.224.jar

REM Check if H2 jar exists
if not exist "%H2_JAR%" (
    echo ERROR: H2 jar not found at %H2_JAR%
    pause
    exit /b 1
)

echo [INFO] Using H2 jar: %H2_JAR%
echo.

REM Database connection parameters
set DB_URL=jdbc:h2:tcp://localhost:9092/./data/stocktrades;DB_CLOSE_DELAY=-1
set DB_USER=sa
set DB_PASSWORD=

echo [INFO] Database connection:
echo   URL: %DB_URL%
echo   User: %DB_USER%
echo.

echo [SETUP] Creating stocktrades database schema and data...
echo.

REM Create a temporary SQL file with all commands
echo -- Stock Trades Database Schema and Data > temp_stocktrades_setup.sql
echo DROP TABLE IF EXISTS stock_trades; >> temp_stocktrades_setup.sql
echo. >> temp_stocktrades_setup.sql
echo CREATE TABLE IF NOT EXISTS stock_trades ( >> temp_stocktrades_setup.sql
echo     id BIGINT AUTO_INCREMENT PRIMARY KEY, >> temp_stocktrades_setup.sql
echo     symb VARCHAR(10) NOT NULL, >> temp_stocktrades_setup.sql
echo     trade_type VARCHAR(4) NOT NULL CHECK (trade_type IN ('BUY', 'SELL')), >> temp_stocktrades_setup.sql
echo     quantity INTEGER NOT NULL, >> temp_stocktrades_setup.sql
echo     price DECIMAL(10,2) NOT NULL, >> temp_stocktrades_setup.sql
echo     total_val DECIMAL(15,2) NOT NULL, >> temp_stocktrades_setup.sql
echo     trade_date_time TIMESTAMP NOT NULL, >> temp_stocktrades_setup.sql
echo     trader_id VARCHAR(50) NOT NULL, >> temp_stocktrades_setup.sql
echo     exchange VARCHAR(20) NOT NULL, >> temp_stocktrades_setup.sql
echo     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, >> temp_stocktrades_setup.sql
echo     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP >> temp_stocktrades_setup.sql
echo ); >> temp_stocktrades_setup.sql
echo. >> temp_stocktrades_setup.sql

REM Add sample data
echo INSERT INTO stock_trades (symb, trade_type, quantity, price, total_val, trade_date_time, trader_id, exchange) VALUES >> temp_stocktrades_setup.sql
echo ('AAPL', 'BUY', 1000, 150.25, 150250.00, '2024-01-01 09:30:00', 'TRADER_001', 'NASDAQ'), >> temp_stocktrades_setup.sql
echo ('GOOGL', 'BUY', 500, 2800.50, 1400250.00, '2024-01-01 09:35:00', 'TRADER_002', 'NASDAQ'), >> temp_stocktrades_setup.sql
echo ('MSFT', 'SELL', 750, 380.75, 285562.50, '2024-01-01 09:40:00', 'TRADER_001', 'NASDAQ'), >> temp_stocktrades_setup.sql
echo ('TSLA', 'BUY', 2000, 245.80, 491600.00, '2024-01-01 09:45:00', 'TRADER_003', 'NASDAQ'), >> temp_stocktrades_setup.sql
echo ('AMZN', 'BUY', 300, 3200.25, 960075.00, '2024-01-01 09:50:00', 'TRADER_002', 'NASDAQ'), >> temp_stocktrades_setup.sql
echo ('AAPL', 'SELL', 500, 151.00, 75500.00, '2024-01-02 10:00:00', 'TRADER_004', 'NASDAQ'), >> temp_stocktrades_setup.sql
echo ('GOOGL', 'BUY', 250, 2805.75, 701437.50, '2024-01-02 10:05:00', 'TRADER_001', 'NASDAQ'), >> temp_stocktrades_setup.sql
echo ('MSFT', 'BUY', 1000, 382.50, 382500.00, '2024-01-02 10:10:00', 'TRADER_003', 'NASDAQ'), >> temp_stocktrades_setup.sql
echo ('TSLA', 'SELL', 1500, 247.25, 370875.00, '2024-01-02 10:15:00', 'TRADER_002', 'NASDAQ'), >> temp_stocktrades_setup.sql
echo ('AMZN', 'SELL', 200, 3205.00, 641000.00, '2024-01-02 10:20:00', 'TRADER_004', 'NASDAQ'), >> temp_stocktrades_setup.sql
echo ('NVDA', 'BUY', 800, 450.75, 360600.00, '2024-01-03 11:00:00', 'TRADER_005', 'NASDAQ'), >> temp_stocktrades_setup.sql
echo ('META', 'BUY', 600, 320.50, 192300.00, '2024-01-03 11:15:00', 'TRADER_001', 'NASDAQ'), >> temp_stocktrades_setup.sql
echo ('NFLX', 'SELL', 400, 480.25, 192100.00, '2024-01-03 11:30:00', 'TRADER_002', 'NASDAQ'), >> temp_stocktrades_setup.sql
echo ('ORCL', 'BUY', 1200, 95.75, 114900.00, '2024-01-03 11:45:00', 'TRADER_003', 'NASDAQ'), >> temp_stocktrades_setup.sql
echo ('CRM', 'SELL', 350, 210.80, 73780.00, '2024-01-03 12:00:00', 'TRADER_004', 'NYSE'); >> temp_stocktrades_setup.sql

echo. >> temp_stocktrades_setup.sql
echo SELECT 'Stock trades database setup completed successfully!' AS status; >> temp_stocktrades_setup.sql
echo SELECT 'Total records: ' ^|^| COUNT(*) AS summary FROM stock_trades; >> temp_stocktrades_setup.sql

REM Execute the SQL using H2's RunScript tool
java -cp "%H2_JAR%" org.h2.tools.RunScript -url "%DB_URL%" -user "%DB_USER%" -password "%DB_PASSWORD%" -script "temp_stocktrades_setup.sql"

if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to setup stocktrades database
    del temp_stocktrades_setup.sql
    pause
    exit /b 1
)

REM Clean up temporary file
del temp_stocktrades_setup.sql

echo.
echo [OK] Stock trades database setup completed successfully!
echo.
echo ================================================================================
echo Stock Trades Database Ready!
echo ================================================================================
echo.
echo Database Details:
echo   Database Name: stocktrades
echo   Connection URL: %DB_URL%
echo   Username: %DB_USER%
echo   Password: [empty]
echo   Table: stock_trades (with sample data)
echo.
echo The stocktrades database is now ready for use!
echo.
pause
