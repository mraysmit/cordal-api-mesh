@echo off
REM Generic API Service Startup Script for Windows
REM This script starts the Generic API Service using the executable JAR

setlocal enabledelayedexpansion

REM Set script directory and project root
set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%..
set SERVICE_DIR=%PROJECT_ROOT%\cordal-api-service

echo ================================================================================
echo                        Generic API Service Startup
echo ================================================================================
echo.

REM Check if executable JAR exists
set EXECUTABLE_JAR=%SERVICE_DIR%\target\cordal-api-service-1.0-SNAPSHOT-executable.jar
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
set JVM_OPTS=-Xms512m -Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication

REM Set application properties
set APP_OPTS=

REM Check for command line arguments
set VALIDATE_ONLY=false
:parse_args
if "%~1"=="" goto start_service
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
echo   --validate-only, --validate    Run configuration validation only and exit
echo   --help, -h                     Show this help message
echo.
echo Environment Variables:
echo   JVM_OPTS                       Additional JVM options (default: %JVM_OPTS%)
echo   APP_OPTS                       Additional application options
echo.
echo Examples:
echo   %~nx0                          Start the service normally
echo   %~nx0 --validate-only          Validate configuration and exit
echo.
pause
exit /b 0

:start_service
echo [INFO] Starting Generic API Service...
echo [INFO] JAR: %EXECUTABLE_JAR%
echo [INFO] JVM Options: %JVM_OPTS%
if not "%APP_OPTS%"=="" echo [INFO] App Options: %APP_OPTS%
echo [INFO] Working Directory: %SERVICE_DIR%
echo.

REM Change to service directory for proper relative path resolution
cd /d "%SERVICE_DIR%"

REM Add validation flag if requested
if "%VALIDATE_ONLY%"=="true" (
    echo [INFO] Running configuration validation only...
    set APP_OPTS=%APP_OPTS% --validate-only
)

REM Start the service
echo [INFO] Executing: java %JVM_OPTS% -jar "%EXECUTABLE_JAR%" %APP_OPTS%
echo.
java %JVM_OPTS% -jar "%EXECUTABLE_JAR%" %APP_OPTS%

REM Check exit code
if errorlevel 1 (
    echo.
    echo [ERROR] Generic API Service failed to start or exited with error
    echo Check the logs above for details
    pause
    exit /b 1
) else (
    echo.
    echo [INFO] Generic API Service stopped normally
)

endlocal
