@echo off
REM JAR Analysis Tool for Javalin API Mesh (Windows)
REM This script analyzes built JARs to show dependencies, sizes, and contents

setlocal enabledelayedexpansion

REM Set script directory and project root
set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%..

echo ================================================================================
echo                        JAR Analysis Tool
echo ================================================================================
echo.

REM Default options
set SHOW_SIZES=false
set SHOW_DEPENDENCIES=false
set SHOW_CONTENTS=false
set SHOW_ALL=true
set TARGET_MODULE=
set TARGET_PROFILE=

REM Parse command line arguments
:parse_args
if "%~1"=="" goto end_parse
if "%~1"=="-h" goto show_help
if "%~1"=="--help" goto show_help
if "%~1"=="-s" (
    set SHOW_SIZES=true
    set SHOW_ALL=false
    shift
    goto parse_args
)
if "%~1"=="--sizes" (
    set SHOW_SIZES=true
    set SHOW_ALL=false
    shift
    goto parse_args
)
if "%~1"=="-d" (
    set SHOW_DEPENDENCIES=true
    set SHOW_ALL=false
    shift
    goto parse_args
)
if "%~1"=="--dependencies" (
    set SHOW_DEPENDENCIES=true
    set SHOW_ALL=false
    shift
    goto parse_args
)
if "%~1"=="-c" (
    set SHOW_CONTENTS=true
    set SHOW_ALL=false
    shift
    goto parse_args
)
if "%~1"=="--contents" (
    set SHOW_CONTENTS=true
    set SHOW_ALL=false
    shift
    goto parse_args
)
if "%~1"=="-a" (
    set SHOW_ALL=true
    shift
    goto parse_args
)
if "%~1"=="--all" (
    set SHOW_ALL=true
    shift
    goto parse_args
)
if "%~1"=="-m" (
    set TARGET_MODULE=%~2
    shift
    shift
    goto parse_args
)
if "%~1"=="--module" (
    set TARGET_MODULE=%~2
    shift
    shift
    goto parse_args
)
if "%~1"=="-p" (
    set TARGET_PROFILE=%~2
    shift
    shift
    goto parse_args
)
if "%~1"=="--profile" (
    set TARGET_PROFILE=%~2
    shift
    shift
    goto parse_args
)
echo Unknown option: %~1
goto show_help

:end_parse

REM Set what to show if --all is selected
if "%SHOW_ALL%"=="true" (
    set SHOW_SIZES=true
    set SHOW_DEPENDENCIES=true
    set SHOW_CONTENTS=true
)

REM Function to format file size
goto main

:format_size
set size=%~1
if %size% GTR 1048576 (
    set /a mb_size=%size% / 1048576
    set formatted_size=!mb_size! MB
) else if %size% GTR 1024 (
    set /a kb_size=%size% / 1024
    set formatted_size=!kb_size! KB
) else (
    set formatted_size=%size% bytes
)
goto :eof

:show_help
echo Usage: %0 [OPTIONS]
echo.
echo Options:
echo   -h, --help              Show this help message
echo   -s, --sizes             Show JAR sizes only
echo   -d, --dependencies      Show dependency analysis
echo   -c, --contents          Show JAR contents
echo   -a, --all               Show all analysis (default)
echo   -m, --module MODULE     Analyze specific module (generic-api-service, metrics-service)
echo   -p, --profile PROFILE   Analyze specific profile JARs (executable, thin, optimized, dev)
echo.
echo Examples:
echo   %0                      # Analyze all JARs
echo   %0 --sizes              # Show sizes only
echo   %0 --module generic-api-service --profile executable
echo   %0 --dependencies       # Show dependency breakdown
exit /b 0

:main
echo Analyzing JARs in: %PROJECT_ROOT%
echo.

REM Find JARs based on criteria
set JAR_COUNT=0
if defined TARGET_MODULE (
    if defined TARGET_PROFILE (
        for %%f in ("%PROJECT_ROOT%\%TARGET_MODULE%\target\*-%TARGET_PROFILE%.jar") do (
            if exist "%%f" (
                set /a JAR_COUNT+=1
                set JAR_!JAR_COUNT!=%%f
            )
        )
    ) else (
        for %%f in ("%PROJECT_ROOT%\%TARGET_MODULE%\target\*.jar") do (
            if exist "%%f" (
                set /a JAR_COUNT+=1
                set JAR_!JAR_COUNT!=%%f
            )
        )
    )
) else (
    if defined TARGET_PROFILE (
        for /r "%PROJECT_ROOT%" %%f in (*-%TARGET_PROFILE%.jar) do (
            if exist "%%f" (
                echo %%f | findstr /v "integration-tests" >nul
                if !errorlevel! equ 0 (
                    set /a JAR_COUNT+=1
                    set JAR_!JAR_COUNT!=%%f
                )
            )
        )
    ) else (
        for /r "%PROJECT_ROOT%" %%f in (*.jar) do (
            if exist "%%f" (
                echo %%f | findstr /v "integration-tests" >nul
                if !errorlevel! equ 0 (
                    set /a JAR_COUNT+=1
                    set JAR_!JAR_COUNT!=%%f
                )
            )
        )
    )
)

if %JAR_COUNT% equ 0 (
    echo No JARs found matching criteria.
    echo Make sure to build the project first:
    echo   mvn clean package
    exit /b 1
)

REM Show sizes
if "%SHOW_SIZES%"=="true" (
    echo JAR Sizes:
    for /l %%i in (1,1,%JAR_COUNT%) do (
        set jar_file=!JAR_%%i!
        for %%j in ("!jar_file!") do (
            set jar_name=%%~nxj
            set jar_size=%%~zj
            call :format_size !jar_size!
            echo   !jar_name!: !formatted_size!
        )
    )
    echo.
)

REM Show dependencies (simplified for Windows)
if "%SHOW_DEPENDENCIES%"=="true" (
    echo Dependency Analysis:
    for /l %%i in (1,1,%JAR_COUNT%) do (
        set jar_file=!JAR_%%i!
        for %%j in ("!jar_file!") do (
            set jar_name=%%~nxj
            echo   Dependencies in !jar_name!:
            echo     [Use jar -tf command manually for detailed analysis]
        )
    )
    echo.
)

REM Show contents (simplified for Windows)
if "%SHOW_CONTENTS%"=="true" (
    echo Content Analysis:
    for /l %%i in (1,1,%JAR_COUNT%) do (
        set jar_file=!JAR_%%i!
        for %%j in ("!jar_file!") do (
            set jar_name=%%~nxj
            echo   Contents of !jar_name!:
            echo     [Use jar -tf "!jar_file!" for detailed contents]
        )
    )
    echo.
)

echo ================================================================================
echo Analysis complete!
echo ================================================================================
echo.
echo For detailed dependency and content analysis, use:
echo   jar -tf "path\to\jar\file.jar"
echo   jar -tf "path\to\jar\file.jar" ^| findstr "\.class$"
