@echo off
echo ========================================
echo Setting up H2 Databases
echo ========================================
echo.

echo Setting up stocktrades database...
java -cp "C:\Users\mraysmit\.m2\repository\com\h2database\h2\2.2.224\h2-2.2.224.jar" org.h2.tools.RunScript -url "jdbc:h2:./data/stocktrades;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1" -user sa -password "" -script scripts/stocktrades-init.sql

echo.
echo Setting up analytics database...
java -cp "C:\Users\mraysmit\.m2\repository\com\h2database\h2\2.2.224\h2-2.2.224.jar" org.h2.tools.RunScript -url "jdbc:h2:./data/analytics;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1" -user sa -password "" -script scripts/analytics-init.sql

echo.
echo Setting up datawarehouse database...
java -cp "C:\Users\mraysmit\.m2\repository\com\h2database\h2\2.2.224\h2-2.2.224.jar" org.h2.tools.RunScript -url "jdbc:h2:./data/datawarehouse;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1" -user sa -password "" -script scripts/datawarehouse-init.sql

echo.
echo ========================================
echo H2 Databases setup complete!
echo ========================================
pause
