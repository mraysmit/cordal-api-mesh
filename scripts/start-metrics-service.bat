@echo off
REM Metrics Service Startup Script for Windows
REM This script starts the Metrics Service using the executable JAR

setlocal enabledelayedexpansion

REM Set script directory and project root
set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%..
set SERVICE_DIR=%PROJECT_ROOT%\cordal-metrics-service

echo ================================================================================
echo                          Metrics Service Startup
echo ================================================================================
echo.

REM Check if executable JAR exists
set EXECUTABLE_JAR=%SERVICE_DIR%\target\cordal-metrics-service-1.0-SNAPSHOT-executable.jar
if not exist "%EXECUTABLE_JAR%" (
    echo [ERROR] Executable JAR not found: %EXECUTABLE_JAR%
    echo.
    echo Please build the project first:
    echo   cd "%PROJECT_ROOT%"
    echo   mvn clean package -DskipTests
    echo.
    pause
    exit /b 1
)

REM Check Java version
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java is not installed or not in PATH
    echo Please install Java 21 or later
    pause
    exit /b 1
)

REM Set JVM options for better performance
set JVM_OPTS=-Xms256m -Xmx1g -XX:+UseG1GC -XX:+UseStringDeduplication

REM Set application properties
set APP_OPTS=

REM Check for command line arguments
:parse_args
if "%~1"=="" goto start_service
if "%~1"=="--help" goto show_help
if "%~1"=="-h" goto show_help
shift
goto parse_args

:show_help
echo Usage: %~nx0 [OPTIONS]
echo.
echo Options:
echo   --help, -h                     Show this help message
echo.
echo Environment Variables:
echo   JVM_OPTS                       Additional JVM options (default: %JVM_OPTS%)
echo   APP_OPTS                       Additional application options
echo.
echo Examples:
echo   %~nx0                          Start the service normally
echo.
pause
exit /b 0

:start_service
echo [INFO] Starting Metrics Service...
echo [INFO] JAR: %EXECUTABLE_JAR%
echo [INFO] JVM Options: %JVM_OPTS%
if not "%APP_OPTS%"=="" echo [INFO] App Options: %APP_OPTS%
echo [INFO] Working Directory: %SERVICE_DIR%
echo.

REM Change to service directory for proper relative path resolution
cd /d "%SERVICE_DIR%"

REM Start the service
echo [INFO] Executing: java %JVM_OPTS% -jar "%EXECUTABLE_JAR%" %APP_OPTS%
echo.
java %JVM_OPTS% -jar "%EXECUTABLE_JAR%" %APP_OPTS%

REM Check exit code
if errorlevel 1 (
    echo.
    echo [ERROR] Metrics Service failed to start or exited with error
    echo Check the logs above for details
    pause
    exit /b 1
) else (
    echo.
    echo [INFO] Metrics Service stopped normally
)

endlocal
