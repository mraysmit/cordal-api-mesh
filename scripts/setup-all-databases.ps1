# PowerShell script to setup CORDAL databases with sample data
Write-Host "Setting up CORDAL databases with sample data..." -ForegroundColor Green

# Create data directory if it doesn't exist
Write-Host "`nCreating data directory if it doesn't exist..." -ForegroundColor Yellow
if (!(Test-Path "../data")) {
    New-Item -ItemType Directory -Path "../data" -Force
    Write-Host "Created data directory" -ForegroundColor Green
} else {
    Write-Host "Data directory already exists" -ForegroundColor Green
}

# Find H2 JAR file
$h2JarPath = Get-ChildItem -Path "cordal-api-service/target/lib" -Filter "h2-*.jar" | Select-Object -First 1
if (!$h2JarPath) {
    Write-Host "ERROR: H2 JAR not found. Please run 'mvn clean package' first." -ForegroundColor Red
    exit 1
}

$h2JarFullPath = $h2JarPath.FullName
Write-Host "Using H2 JAR: $h2JarFullPath" -ForegroundColor Cyan

# Setup Analytics database
Write-Host "`nSetting up Analytics database..." -ForegroundColor Yellow
$analyticsResult = java -cp "$h2JarFullPath" org.h2.tools.RunScript -url "jdbc:h2:../data/analytics;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1" -user sa -password "" -script "scripts/setup-analytics-database.sql"
if ($LASTEXITCODE -eq 0) {
    Write-Host "Analytics database setup completed successfully" -ForegroundColor Green
} else {
    Write-Host "ERROR: Analytics database setup failed" -ForegroundColor Red
}

# Setup Stock Trades database
Write-Host "`nSetting up Stock Trades database..." -ForegroundColor Yellow
$stocktradesResult = java -cp "$h2JarFullPath" org.h2.tools.RunScript -url "jdbc:h2:../data/stocktrades;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1" -user sa -password "" -script "scripts/setup-stocktrades-database.sql"
if ($LASTEXITCODE -eq 0) {
    Write-Host "Stock Trades database setup completed successfully" -ForegroundColor Green
} else {
    Write-Host "ERROR: Stock Trades database setup failed" -ForegroundColor Red
}

# Setup Data Warehouse database
Write-Host "`nSetting up Data Warehouse database..." -ForegroundColor Yellow
$datawarehouseResult = java -cp "$h2JarFullPath" org.h2.tools.RunScript -url "jdbc:h2:../data/datawarehouse;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1" -user sa -password "" -script "scripts/setup-datawarehouse-database.sql"
if ($LASTEXITCODE -eq 0) {
    Write-Host "Data Warehouse database setup completed successfully" -ForegroundColor Green
} else {
    Write-Host "ERROR: Data Warehouse database setup failed" -ForegroundColor Red
}

Write-Host "`nDatabase setup complete!" -ForegroundColor Green
Write-Host "`nYou can now start CORDAL and all endpoints should be functional." -ForegroundColor Cyan
Write-Host "Run: java -jar cordal-api-service/target/generic-api-service-1.0-SNAPSHOT-executable.jar" -ForegroundColor Cyan
