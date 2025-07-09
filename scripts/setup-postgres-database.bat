@echo off
REM ================================================================================
REM PostgreSQL Database Setup Script for postgres-trades
REM ================================================================================
REM This script sets up the PostgreSQL database and tables required for the
REM Generic API Service postgres-trades database configuration.
REM
REM Prerequisites:
REM 1. PostgreSQL must be installed and running
REM 2. PostgreSQL bin directory must be in PATH (or modify PSQL_PATH below)
REM 3. PostgreSQL user 'postgres' with password 'postgres' must exist
REM
REM Usage: Run this script from the project root directory
REM ================================================================================

echo.
echo ================================================================================
echo PostgreSQL Database Setup for postgres-trades
echo ================================================================================
echo.

REM Check if PostgreSQL is available
where psql >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: PostgreSQL psql command not found in PATH
    echo.
    echo Please ensure PostgreSQL is installed and the bin directory is in your PATH.
    echo Alternatively, you can modify this script to set the full path to psql.exe
    echo.
    echo Example PostgreSQL installation paths:
    echo   C:\Program Files\PostgreSQL\15\bin\psql.exe
    echo   C:\Program Files\PostgreSQL\14\bin\psql.exe
    echo.
    pause
    exit /b 1
)

echo [INFO] PostgreSQL psql command found
echo.

REM Set connection parameters
set PGHOST=localhost
set PGPORT=5432
set PGUSER=postgres
set PGPASSWORD=postgres

echo [INFO] Connection parameters:
echo   Host: %PGHOST%
echo   Port: %PGPORT%
echo   User: %PGUSER%
echo   Password: [hidden]
echo.

REM Test PostgreSQL connection
echo [TEST] Testing PostgreSQL connection...
psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -d postgres -c "SELECT version();" >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Cannot connect to PostgreSQL server
    echo.
    echo Please check:
    echo 1. PostgreSQL server is running
    echo 2. Connection parameters are correct
    echo 3. User 'postgres' exists with password 'postgres'
    echo 4. PostgreSQL is accepting connections on localhost:5432
    echo.
    pause
    exit /b 1
)

echo [OK] PostgreSQL connection successful
echo.

REM Check if database already exists
echo [CHECK] Checking if postgres-trades database exists...
psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -d postgres -t -c "SELECT 1 FROM pg_database WHERE datname='postgres-trades';" | findstr "1" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [INFO] Database 'postgres-trades' already exists
    set DB_EXISTS=1
) else (
    echo [INFO] Database 'postgres-trades' does not exist
    set DB_EXISTS=0
)
echo.

REM Create database if it doesn't exist
if %DB_EXISTS% EQU 0 (
    echo [CREATE] Creating postgres-trades database...
    psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -d postgres -c "CREATE DATABASE \"postgres-trades\" WITH OWNER = postgres ENCODING = 'UTF8';"
    if %ERRORLEVEL% NEQ 0 (
        echo ERROR: Failed to create database
        pause
        exit /b 1
    )
    echo [OK] Database created successfully
    echo.
)

REM Setup tables and data
echo [SETUP] Setting up tables and sample data...
psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -d postgres-trades -f scripts\postgres-setup-simple.sql
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to setup tables and data
    pause
    exit /b 1
)

echo [OK] Tables and data setup completed
echo.

REM Verify setup
echo [VERIFY] Verifying database setup...
psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -d postgres-trades -c "SELECT 'Table exists: stock_trades' AS status; SELECT 'Total records: ' || COUNT(*) AS summary FROM stock_trades;"
if %ERRORLEVEL% NEQ 0 (
    echo WARNING: Verification failed, but setup may still be successful
    echo.
)

echo.
echo ================================================================================
echo PostgreSQL Setup Completed Successfully!
echo ================================================================================
echo.
echo Database Details:
echo   Database Name: postgres-trades
echo   Connection URL: jdbc:postgresql://localhost:5432/postgres-trades?currentSchema=public
echo   Username: postgres
echo   Password: postgres
echo.
echo Next Steps:
echo 1. Run the bootstrap demo to test the connection:
echo    scripts\run-bootstrap-demo.bat
echo.
echo 2. Or test the Generic API Service directly:
echo    cd generic-api-service
echo    mvn exec:java
echo.
echo The postgres-trades database is now ready for use with the Generic API Service!
echo.
pause
