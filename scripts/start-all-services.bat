@echo off
REM Master Startup Script for All Services (Windows)
REM This script starts both Generic API Service and Metrics Service

setlocal enabledelayedexpansion

REM Set script directory and project root
set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%..

echo ================================================================================
echo                        CORDAL - All Services
echo ================================================================================
echo.

REM Check if both executable JARs exist
set GENERIC_API_JAR=%PROJECT_ROOT%\cordal-api-service\target\cordal-api-service-1.0-SNAPSHOT-executable.jar
set METRICS_JAR=%PROJECT_ROOT%\cordal-metrics-service\target\cordal-metrics-service-1.0-SNAPSHOT-executable.jar

set MISSING_JARS=false
if not exist "%GENERIC_API_JAR%" (
    echo [ERROR] Generic API Service executable JAR not found: %GENERIC_API_JAR%
    set MISSING_JARS=true
)
if not exist "%METRICS_JAR%" (
    echo [ERROR] Metrics Service executable JAR not found: %METRICS_JAR%
    set MISSING_JARS=true
)

if "%MISSING_JARS%"=="true" (
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

REM Parse command line arguments
set START_MODE=both
set VALIDATE_ONLY=false

:parse_args
if "%~1"=="" goto start_services
if "%~1"=="--generic-api-only" set START_MODE=generic-api
if "%~1"=="--metrics-only" set START_MODE=metrics
if "%~1"=="--validate-only" set VALIDATE_ONLY=true
if "%~1"=="--validate" set VALIDATE_ONLY=true
if "%~1"=="--help" goto show_help
if "%~1"=="-h" goto show_help
shift
goto parse_args

:show_help
echo Usage: %~nx0 [OPTIONS]
echo.
echo Options:
echo   --generic-api-only             Start only Generic API Service
echo   --metrics-only                 Start only Metrics Service
echo   --validate-only, --validate    Run configuration validation only and exit
echo   --help, -h                     Show this help message
echo.
echo Default behavior: Start both services in separate windows
echo.
echo Examples:
echo   %~nx0                          Start both services
echo   %~nx0 --generic-api-only       Start only Generic API Service
echo   %~nx0 --metrics-only           Start only Metrics Service
echo   %~nx0 --validate-only          Validate configurations and exit
echo.
pause
exit /b 0

:start_services
echo [INFO] Starting services in mode: %START_MODE%
if "%VALIDATE_ONLY%"=="true" echo [INFO] Validation mode enabled
echo.

if "%START_MODE%"=="generic-api" goto start_generic_api_only
if "%START_MODE%"=="metrics" goto start_metrics_only
if "%START_MODE%"=="both" goto start_both_services

:start_generic_api_only
echo [INFO] Starting Generic API Service only...
if "%VALIDATE_ONLY%"=="true" (
    call "%SCRIPT_DIR%start-cordal-api-service.bat" --validate-only
) else (
    call "%SCRIPT_DIR%start-cordal-api-service.bat"
)
goto end

:start_metrics_only
echo [INFO] Starting Metrics Service only...
call "%SCRIPT_DIR%start-cordal-metrics-service.bat"
goto end

:start_both_services
echo [INFO] Starting both services in separate windows...
echo.

REM Start Generic API Service in a new window
echo [INFO] Starting Generic API Service in new window...
if "%VALIDATE_ONLY%"=="true" (
    start "Generic API Service" cmd /k ""%SCRIPT_DIR%start-cordal-api-service.bat" --validate-only"
) else (
    start "Generic API Service" cmd /k ""%SCRIPT_DIR%start-cordal-api-service.bat""
)

REM Wait a moment before starting the second service
timeout /t 3 /nobreak >nul

REM Start Metrics Service in a new window (only if not in validation mode)
if not "%VALIDATE_ONLY%"=="true" (
    echo [INFO] Starting Metrics Service in new window...
    start "Metrics Service" cmd /k ""%SCRIPT_DIR%start-cordal-metrics-service.bat""
)

echo.
echo [INFO] Services are starting in separate windows
if not "%VALIDATE_ONLY%"=="true" (
    echo [INFO] Generic API Service: http://localhost:8080
    echo [INFO] Metrics Service: http://localhost:8081
    echo [INFO] Check the individual windows for startup progress
)
echo.
echo Press any key to exit this script (services will continue running)...
pause >nul

:end
endlocal
