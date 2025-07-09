# IDE Setup and Troubleshooting Guide

This guide explains how to run the SystemBootstrapDemo and other components from your IDE, and troubleshoots common issues.

## 🔧 IDE Configuration Issue

### Problem
When running `SystemBootstrapDemo` from your IDE, you may encounter database connection failures with messages like:
```
Database 'stocktrades' is unavailable
Database 'analytics' is unavailable  
Database 'datawarehouse' is unavailable
```

While PostgreSQL databases work fine, H2 databases fail to connect.

### Root Cause
The issue occurs because of **working directory differences**:

- **Command Line Execution**: Runs from `generic-api-service/` directory
  - Database paths like `../data/stocktrades` resolve to `javalin-api-mesh/data/stocktrades` ✅
  - Config paths like `../generic-config` resolve to `javalin-api-mesh/generic-config` ✅

- **IDE Execution**: Runs from project root `javalin-api-mesh/` directory  
  - Database paths like `../data/stocktrades` resolve to `../data/stocktrades` (outside project) ❌
  - Config paths like `../generic-config` resolve to `../generic-config` (outside project) ❌

## ✅ Solution Implemented

The `SystemBootstrapDemo` now automatically detects IDE execution and adjusts paths:

### Automatic Detection
```java
// Detects if running from project root (IDE) vs generic-api-service (command line)
boolean runningFromProjectRoot = currentDir.endsWith("javalin-api-mesh") && 
                                !currentDir.endsWith("generic-api-service");
```

### Automatic Path Adjustment
When IDE execution is detected:
1. **Working Directory**: Changes from `javalin-api-mesh/` to `javalin-api-mesh/generic-api-service/`
2. **Config Directory**: Sets to `../generic-config` (relative to new working directory)
3. **Database Paths**: Now resolve correctly as `../data/stocktrades` etc.

## 🚀 How to Run from IDE

### Option 1: Run SystemBootstrapDemo Main Method
1. Open `generic-api-service/src/main/java/dev/mars/bootstrap/SystemBootstrapDemo.java`
2. Right-click on the `main` method
3. Select "Run" or "Debug"
4. The automatic path adjustment will handle the rest

### Option 2: Run with Run Configuration
1. Create a new Run Configuration in your IDE
2. **Main Class**: `dev.mars.bootstrap.SystemBootstrapDemo`
3. **Module**: `generic-api-service`
4. **Working Directory**: Leave as project root (auto-adjustment will handle it)

### Option 3: Manual Working Directory (Alternative)
If you prefer to set the working directory manually:
1. Create a Run Configuration
2. **Main Class**: `dev.mars.bootstrap.SystemBootstrapDemo`
3. **Working Directory**: Set to `{PROJECT_ROOT}/generic-api-service`
4. This bypasses the auto-detection and uses the command-line paths

## 🔍 Verification

After running from IDE, you should see logs like:
```
[INIT] Current working directory: C:\path\to\javalin-api-mesh
[INIT] Detected IDE execution from project root, changing working directory...
[INIT] Changed working directory to: C:\path\to\javalin-api-mesh\generic-api-service
[INIT] Set config directories to: ../generic-config
```

And the database status should show:
```
+----------------------+----------+--------------------------------------------------+
| Database             | Status   | Details                                          |
+----------------------+----------+--------------------------------------------------+
| analytics            | OK       | Connected and ready                              |
| datawarehouse        | OK       | Connected and ready                              |
| postgres-trades      | OK       | Connected and ready                              |
| stocktrades          | OK       | Connected and ready                              |
+----------------------+----------+--------------------------------------------------+
```

## 🐛 Troubleshooting

### Still Getting Database Errors?
1. **Check Data Directory**: Ensure `javalin-api-mesh/data/` exists with database files
2. **Check Config Directory**: Ensure `javalin-api-mesh/generic-config/` exists with YAML files
3. **Check Logs**: Look for the working directory detection messages
4. **Manual Override**: Set working directory manually in IDE run configuration

### PostgreSQL Works but H2 Doesn't?
This confirms the working directory issue. PostgreSQL uses absolute connection strings (localhost:5432) while H2 uses relative file paths.

### Configuration Files Not Found?
Check that the IDE can find:
- `javalin-api-mesh/generic-config/*.yml` files
- `javalin-api-mesh/data/*.mv.db` files

### IDE-Specific Notes

#### IntelliJ IDEA
- Working directory is usually set to project root by default
- Auto-detection should work automatically
- Check "Run/Debug Configurations" if issues persist

#### Eclipse
- Working directory may need manual configuration
- Go to Run Configurations → Arguments → Working Directory
- Set to `${workspace_loc:javalin-api-mesh}/generic-api-service`

#### VS Code
- Working directory is typically the project root
- Auto-detection should work automatically
- Check `.vscode/launch.json` if using custom configurations

## 📝 Technical Details

### Files Modified
- `SystemBootstrapDemo.java`: Added `adjustWorkingDirectoryForIDE()` method
- `GenericApiConfig.java`: Enhanced directory configuration loading

### System Properties Used
- `user.dir`: Working directory (modified for IDE execution)
- `generic.config.directories`: Configuration directory paths

### Backward Compatibility
- Command line execution remains unchanged
- Existing scripts continue to work
- No impact on production deployments

## ✅ Success Criteria

The IDE setup is working correctly when:
1. ✅ All 4 databases show "OK" status
2. ✅ No "database unavailable" errors in logs  
3. ✅ Configuration files are found and loaded
4. ✅ Bootstrap demo completes successfully
5. ✅ API endpoints are accessible

## 🆘 Still Having Issues?

If problems persist:
1. Check that you've built the project: `mvn clean install`
2. Verify database files exist in `data/` directory
3. Verify config files exist in `generic-config/` directory
4. Try running from command line to confirm it works there
5. Check IDE console for the working directory detection logs
