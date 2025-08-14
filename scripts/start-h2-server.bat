@echo off
REM Start H2 Database Server for cordal application
REM This script starts H2 in TCP server mode to allow multiple connections

echo Starting H2 Database Server...
echo.
echo Server Configuration:
echo - TCP Port: 9092
echo - Base Directory: ./data
echo - Allow external connections: Yes
echo.

REM Find H2 jar in Maven repository
set H2_JAR=%USERPROFILE%\.m2\repository\com\h2database\h2\2.2.224\h2-2.2.224.jar

if not exist "%H2_JAR%" (
    echo ERROR: H2 jar not found at %H2_JAR%
    echo Please ensure H2 dependency is downloaded via Maven
    echo Run: mvn dependency:resolve
    pause
    exit /b 1
)

echo Using H2 jar: %H2_JAR%
echo.

REM Create data directory if it doesn't exist
if not exist "data" mkdir data

REM Start H2 server
echo Starting H2 TCP Server on port 9092...
java -cp "%H2_JAR%" org.h2.tools.Server -tcp -tcpAllowOthers -tcpPort 9092 -baseDir ./data -ifNotExists

echo.
echo H2 Server stopped.
pause
