@echo off
echo ========================================
echo System Bootstrap Demonstration
echo ========================================
echo.
echo This script will:
echo 1. Build the project
echo 2. Run the bootstrap demonstration
echo 3. Show system startup and API testing
echo.
echo Press any key to continue...
pause >nul

echo.
echo Building project...
call mvn clean install -q
if %ERRORLEVEL% neq 0 (
    echo ERROR: Build failed!
    pause
    exit /b 1
)

echo.
echo Starting bootstrap demonstration...
echo.
cd generic-api-service
call mvn exec:java -Dexec.mainClass="dev.mars.bootstrap.SystemBootstrapDemo" -q

echo.
echo Bootstrap demonstration completed.
echo Press any key to exit...
pause >nul
