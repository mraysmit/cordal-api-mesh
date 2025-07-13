@echo off
REM Build Script for Executable JARs (Windows)
REM This script builds the project and creates executable fat JARs

setlocal enabledelayedexpansion

REM Set script directory and project root
set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%..

echo ================================================================================
echo                        Building Executable JARs
echo ================================================================================
echo.

REM Check if Maven is available
mvn --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Maven is not installed or not in PATH
    echo Please install Apache Maven 3.6+ and add it to your PATH
    pause
    exit /b 1
)

REM Parse command line arguments
set SKIP_TESTS=true
set CLEAN_BUILD=true
set PROFILE=

:parse_args
if "%~1"=="" goto start_build
if "%~1"=="--run-tests" set SKIP_TESTS=false
if "%~1"=="--no-clean" set CLEAN_BUILD=false
if "%~1"=="--profile" (
    set PROFILE=-P%~2
    shift
)
if "%~1"=="--help" goto show_help
if "%~1"=="-h" goto show_help
shift
goto parse_args

:show_help
echo Usage: %~nx0 [OPTIONS]
echo.
echo Options:
echo   --run-tests                    Run tests during build (default: skip tests)
echo   --no-clean                     Skip clean phase (default: run clean)
echo   --profile PROFILE              Activate Maven profile
echo   --help, -h                     Show this help message
echo.
echo Examples:
echo   %~nx0                          Build with default settings (clean, skip tests)
echo   %~nx0 --run-tests              Build and run all tests
echo   %~nx0 --no-clean               Build without cleaning first
echo   %~nx0 --profile test-profiling Build with test profiling enabled
echo.
pause
exit /b 0

:start_build
echo [INFO] Build Configuration:
echo [INFO]   Project Root: %PROJECT_ROOT%
echo [INFO]   Clean Build: %CLEAN_BUILD%
echo [INFO]   Skip Tests: %SKIP_TESTS%
if not "%PROFILE%"=="" echo [INFO]   Maven Profile: %PROFILE%
echo.

REM Change to project root directory
cd /d "%PROJECT_ROOT%"

REM Build Maven command
set MVN_CMD=mvn
if "%CLEAN_BUILD%"=="true" set MVN_CMD=%MVN_CMD% clean
set MVN_CMD=%MVN_CMD% package
if "%SKIP_TESTS%"=="true" set MVN_CMD=%MVN_CMD% -DskipTests
if not "%PROFILE%"=="" set MVN_CMD=%MVN_CMD% %PROFILE%

echo [INFO] Executing: %MVN_CMD%
echo.

REM Execute Maven build
%MVN_CMD%

if errorlevel 1 (
    echo.
    echo [ERROR] Build failed!
    echo Check the output above for details
    pause
    exit /b 1
)

echo.
echo ================================================================================
echo                            Build Completed Successfully
echo ================================================================================
echo.

REM Check if executable JARs were created
set GENERIC_API_JAR=%PROJECT_ROOT%\generic-api-service\target\generic-api-service-1.0-SNAPSHOT-executable.jar
set METRICS_JAR=%PROJECT_ROOT%\metrics-service\target\metrics-service-1.0-SNAPSHOT-executable.jar

echo [INFO] Checking created artifacts:
if exist "%GENERIC_API_JAR%" (
    echo [OK] Generic API Service executable JAR: %GENERIC_API_JAR%
    for %%F in ("%GENERIC_API_JAR%") do echo      Size: %%~zF bytes
) else (
    echo [ERROR] Generic API Service executable JAR not found!
)

if exist "%METRICS_JAR%" (
    echo [OK] Metrics Service executable JAR: %METRICS_JAR%
    for %%F in ("%METRICS_JAR%") do echo      Size: %%~zF bytes
) else (
    echo [ERROR] Metrics Service executable JAR not found!
)

echo.
echo [INFO] You can now start the services using:
echo [INFO]   %SCRIPT_DIR%start-all-services.bat
echo [INFO]   %SCRIPT_DIR%start-generic-api-service.bat
echo [INFO]   %SCRIPT_DIR%start-metrics-service.bat
echo.

endlocal
