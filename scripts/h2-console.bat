@echo off
REM Start H2 Web Console to manage databases
REM This provides a web interface to view and manage your H2 databases

echo Starting H2 Web Console...
echo.
echo Console Configuration:
echo - Web Port: 8082
echo - Access URL: http://localhost:8082
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

REM Start H2 web console
echo Starting H2 Web Console on port 8082...
echo Open your browser to: http://localhost:8082
echo.
echo Connection examples:
echo - API Service Config DB: jdbc:h2:tcp://localhost:9092/./data/api-service-config
echo - Metrics DB: jdbc:h2:tcp://localhost:9092/./data/metrics
echo - Username: sa
echo - Password: (leave empty)
echo.

java -cp "%H2_JAR%" org.h2.tools.Server -web -webAllowOthers -webPort 8082

echo.
echo H2 Web Console stopped.
pause
