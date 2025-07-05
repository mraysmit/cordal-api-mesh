@echo off
echo ========================================
echo Configuration Validation Testing
echo ========================================
echo.
echo This script demonstrates the new validation flags:
echo 1. validation.runOnStartup - Run validation during normal startup
echo 2. validation.validateOnly - Run only validation and exit
echo 3. --validate-only command line flag
echo.

echo Testing 1: Default configuration (no validation)
echo ------------------------------------------------
echo Current application.yml has validation.runOnStartup=false and validation.validateOnly=false
echo This should start normally without validation.
echo.
pause

echo Testing 2: Command line validation flag
echo ----------------------------------------
echo Running with --validate-only flag to run validation and exit
echo This will use the ConfigurationValidator to check all configurations.
echo.
echo Command: java -cp target/classes;target/lib/* dev.mars.generic.GenericApiApplication --validate-only
echo.
echo Note: This may fail if configuration files are missing, which is expected behavior.
echo The validation will show detailed results and exit without starting the server.
echo.
pause

echo Testing 3: Configuration file with validation enabled
echo -----------------------------------------------------
echo You can create a custom application.yml with:
echo   validation:
echo     runOnStartup: true   # Run validation during startup
echo     validateOnly: false  # Continue with normal startup after validation
echo.
echo Or for validate-only mode:
echo   validation:
echo     runOnStartup: false  # Not needed when validateOnly is true
echo     validateOnly: true   # Run validation and exit
echo.

echo ========================================
echo Validation Features Summary
echo ========================================
echo.
echo 1. YAML Configuration Flags:
echo    - validation.runOnStartup: Run validation during normal app startup
echo    - validation.validateOnly: Run only validation and exit (no server start)
echo.
echo 2. Command Line Arguments:
echo    - --validate-only: Override config and run validation only
echo    - --validate: Same as --validate-only (shorter form)
echo.
echo 3. Validation Process:
echo    - Configuration Chain Validation (endpoints -> queries -> databases)
echo    - Database Schema Validation (tables, fields, query compatibility)
echo    - Comprehensive error reporting with ASCII tables
echo    - Fatal error handling with detailed logging
echo.
echo 4. Use Cases:
echo    - CI/CD pipeline configuration validation
echo    - Development environment setup verification
echo    - Production deployment pre-checks
echo    - Troubleshooting configuration issues
echo.
echo ========================================
echo Implementation Complete!
echo ========================================
pause
