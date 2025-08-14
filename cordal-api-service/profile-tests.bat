@echo off
REM Test Profiling Script for Windows
REM This script runs tests with profiling enabled and generates performance reports

echo ================================================================================
echo                           TEST PROFILING UTILITY
echo ================================================================================
echo.

REM Check if Maven is available
where mvn >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo ERROR: Maven is not available in PATH
    echo Please install Maven or add it to your PATH
    exit /b 1
)

REM Set default profile
set PROFILE=test-profiling
set CLEAN=false
set SHOW_REPORTS=true

REM Parse command line arguments
:parse_args
if "%1"=="" goto :run_profiling
if "%1"=="--profile" (
    set PROFILE=%2
    shift
    shift
    goto :parse_args
)
if "%1"=="--clean" (
    set CLEAN=true
    shift
    goto :parse_args
)
if "%1"=="--no-reports" (
    set SHOW_REPORTS=false
    shift
    goto :parse_args
)
if "%1"=="--help" (
    goto :show_help
)
shift
goto :parse_args

:show_help
echo Usage: profile-tests.bat [OPTIONS]
echo.
echo Options:
echo   --profile PROFILE    Use specific Maven profile (default: test-profiling)
echo                       Available profiles: test-profiling, test-benchmark
echo   --clean             Clean before running tests
echo   --no-reports        Don't open reports after completion
echo   --help              Show this help message
echo.
echo Examples:
echo   profile-tests.bat                          # Run with default profiling
echo   profile-tests.bat --profile test-benchmark # Run benchmark profiling
echo   profile-tests.bat --clean                  # Clean and run profiling
echo.
exit /b 0

:run_profiling
echo Profile: %PROFILE%
echo Clean build: %CLEAN%
echo Show reports: %SHOW_REPORTS%
echo.

REM Clean if requested
if "%CLEAN%"=="true" (
    echo Cleaning project...
    call mvn clean
    if %ERRORLEVEL% neq 0 (
        echo ERROR: Clean failed
        exit /b 1
    )
    echo.
)

REM Run tests with profiling
echo Running tests with profiling enabled...
echo Command: mvn test -P%PROFILE%
echo.

call mvn test -P%PROFILE%
set TEST_RESULT=%ERRORLEVEL%

echo.
echo ================================================================================
echo                           PROFILING RESULTS
echo ================================================================================

REM Check if profiling reports were generated
set REPORT_DIR=target\test-profiling
if "%PROFILE%"=="test-benchmark" set REPORT_DIR=target\test-benchmark
if "%PROFILE%"=="test-profiling" set REPORT_DIR=target\test-profiling-detailed

if exist "%REPORT_DIR%" (
    echo.
    echo Profiling reports generated in: %REPORT_DIR%
    echo.
    
    REM Show summary if available
    if exist "%REPORT_DIR%\performance-summary.txt" (
        echo PERFORMANCE SUMMARY:
        echo ----------------------------------------
        type "%REPORT_DIR%\performance-summary.txt"
        echo.
    )
    
    REM List available reports
    echo AVAILABLE REPORTS:
    echo ----------------------------------------
    dir /b "%REPORT_DIR%\*.txt" "%REPORT_DIR%\*.csv" 2>nul
    echo.
    
    REM Open reports if requested
    if "%SHOW_REPORTS%"=="true" (
        echo Opening reports directory...
        start "" "%REPORT_DIR%"
        
        REM Open summary report if available
        if exist "%REPORT_DIR%\performance-summary.txt" (
            start "" notepad "%REPORT_DIR%\performance-summary.txt"
        )
    )
    
) else (
    echo WARNING: No profiling reports found in %REPORT_DIR%
    echo This might indicate that profiling was not enabled or no tests were run.
)

REM Show test results
echo.
if %TEST_RESULT% equ 0 (
    echo ✓ All tests passed successfully
) else (
    echo ✗ Some tests failed (exit code: %TEST_RESULT%)
)

echo.
echo ================================================================================
echo                           NEXT STEPS
echo ================================================================================
echo.
echo 1. Review the performance summary above
echo 2. Check detailed reports in: %REPORT_DIR%
echo 3. Focus optimization on the slowest tests identified
echo 4. Consider the optimization recommendations provided
echo.
echo Key reports:
echo   - performance-summary.txt     : Overall performance statistics
echo   - slowest-tests-report.txt    : Top slowest tests with details
echo   - optimization-recommendations.txt : Specific optimization suggestions
echo   - detailed-performance-report.csv  : Complete test timing data
echo.

exit /b %TEST_RESULT%
