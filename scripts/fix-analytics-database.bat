@echo off
REM ================================================================================
REM Fix Analytics Database - Create correct tables
REM ================================================================================

echo.
echo ================================================================================
echo Fixing Analytics Database Tables
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
set DB_URL=jdbc:h2:tcp://localhost:9092/./data/analytics;DB_CLOSE_DELAY=-1
set DB_USER=sa
set DB_PASSWORD=

echo [INFO] Database connection:
echo   URL: %DB_URL%
echo   User: %DB_USER%
echo.

echo [SETUP] Fixing analytics database tables...
echo.

REM Create a temporary SQL file with all commands
echo -- Fix analytics database tables > temp_analytics_fix.sql
echo DROP TABLE IF EXISTS stock_trades; >> temp_analytics_fix.sql
echo DROP TABLE IF EXISTS trades; >> temp_analytics_fix.sql
echo. >> temp_analytics_fix.sql
echo CREATE TABLE IF NOT EXISTS trades ( >> temp_analytics_fix.sql
echo     id BIGINT AUTO_INCREMENT PRIMARY KEY, >> temp_analytics_fix.sql
echo     symbol VARCHAR(10) NOT NULL, >> temp_analytics_fix.sql
echo     trade_type VARCHAR(4) NOT NULL CHECK (trade_type IN ('BUY', 'SELL')), >> temp_analytics_fix.sql
echo     quantity INTEGER NOT NULL, >> temp_analytics_fix.sql
echo     price DECIMAL(10,2) NOT NULL, >> temp_analytics_fix.sql
echo     total_value DECIMAL(15,2) NOT NULL, >> temp_analytics_fix.sql
echo     trade_date TIMESTAMP NOT NULL, >> temp_analytics_fix.sql
echo     trader_id VARCHAR(50) NOT NULL, >> temp_analytics_fix.sql
echo     exchange VARCHAR(20) NOT NULL, >> temp_analytics_fix.sql
echo     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP >> temp_analytics_fix.sql
echo ); >> temp_analytics_fix.sql
echo. >> temp_analytics_fix.sql

REM Add sample data
echo INSERT INTO trades (symbol, trade_type, quantity, price, total_value, trade_date, trader_id, exchange) VALUES >> temp_analytics_fix.sql
echo ('AAPL', 'BUY', 1000, 150.25, 150250.00, '2024-01-01 09:30:00', 'TRADER_001', 'NASDAQ'), >> temp_analytics_fix.sql
echo ('GOOGL', 'BUY', 500, 2800.50, 1400250.00, '2024-01-01 09:35:00', 'TRADER_002', 'NASDAQ'), >> temp_analytics_fix.sql
echo ('MSFT', 'SELL', 750, 380.75, 285562.50, '2024-01-01 09:40:00', 'TRADER_001', 'NASDAQ'), >> temp_analytics_fix.sql
echo ('TSLA', 'BUY', 2000, 245.80, 491600.00, '2024-01-01 09:45:00', 'TRADER_003', 'NASDAQ'), >> temp_analytics_fix.sql
echo ('AMZN', 'BUY', 300, 3200.25, 960075.00, '2024-01-01 09:50:00', 'TRADER_002', 'NASDAQ'), >> temp_analytics_fix.sql
echo ('AAPL', 'SELL', 500, 151.00, 75500.00, '2024-01-02 10:00:00', 'TRADER_004', 'NASDAQ'), >> temp_analytics_fix.sql
echo ('GOOGL', 'BUY', 250, 2805.75, 701437.50, '2024-01-02 10:05:00', 'TRADER_001', 'NASDAQ'), >> temp_analytics_fix.sql
echo ('MSFT', 'BUY', 1000, 382.50, 382500.00, '2024-01-02 10:10:00', 'TRADER_003', 'NASDAQ'), >> temp_analytics_fix.sql
echo ('TSLA', 'SELL', 1500, 247.25, 370875.00, '2024-01-02 10:15:00', 'TRADER_002', 'NASDAQ'), >> temp_analytics_fix.sql
echo ('AMZN', 'SELL', 200, 3205.00, 641000.00, '2024-01-02 10:20:00', 'TRADER_004', 'NASDAQ'); >> temp_analytics_fix.sql

echo. >> temp_analytics_fix.sql
echo SELECT 'Analytics database fixed successfully!' AS status; >> temp_analytics_fix.sql
echo SELECT 'Total records: ' ^|^| COUNT(*) AS summary FROM trades; >> temp_analytics_fix.sql

REM Execute the SQL using H2's RunScript tool
java -cp "%H2_JAR%" org.h2.tools.RunScript -url "%DB_URL%" -user "%DB_USER%" -password "%DB_PASSWORD%" -script "temp_analytics_fix.sql"

if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to fix analytics database
    del temp_analytics_fix.sql
    pause
    exit /b 1
)

REM Clean up temporary file
del temp_analytics_fix.sql

echo.
echo [OK] Analytics database fixed successfully!
echo.
echo ================================================================================
echo Analytics Database Ready!
echo ================================================================================
echo.
echo Database Details:
echo   Database Name: analytics
echo   Connection URL: %DB_URL%
echo   Username: %DB_USER%
echo   Password: [empty]
echo   Table: trades (with correct schema)
echo.
echo The analytics database is now ready for use!
echo.
pause
