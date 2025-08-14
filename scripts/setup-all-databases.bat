@echo off
echo Setting up CORDAL databases with sample data...

echo.
echo Creating data directory if it doesn't exist...
if not exist "..\data" mkdir "..\data"

echo.
echo Setting up Analytics database...
java -cp "cordal-api-service/target/lib/*" org.h2.tools.RunScript -url "jdbc:h2:../data/analytics;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1" -user sa -password "" -script "scripts/setup-analytics-database.sql"

echo.
echo Setting up Stock Trades database...
java -cp "cordal-api-service/target/lib/*" org.h2.tools.RunScript -url "jdbc:h2:../data/stocktrades;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1" -user sa -password "" -script "scripts/setup-stocktrades-database.sql"

echo.
echo Setting up Data Warehouse database...
java -cp "cordal-api-service/target/lib/*" org.h2.tools.RunScript -url "jdbc:h2:../data/datawarehouse;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1" -user sa -password "" -script "scripts/setup-datawarehouse-database.sql"

echo.
echo Database setup complete!
echo.
echo You can now start CORDAL and all endpoints should be functional.
echo Run: java -jar cordal-api-service/target/generic-api-service-1.0-SNAPSHOT-executable.jar
pause
