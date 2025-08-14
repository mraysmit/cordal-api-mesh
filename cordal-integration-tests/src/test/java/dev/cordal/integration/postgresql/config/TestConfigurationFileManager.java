package dev.cordal.integration.postgresql.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Manages temporary configuration files for dual PostgreSQL database integration testing
 * Creates, writes, and cleans up YAML configuration files during test execution
 */
public class TestConfigurationFileManager {
    private static final Logger logger = LoggerFactory.getLogger(TestConfigurationFileManager.class);
    
    private final Path tempConfigDirectory;
    private final String testName;
    
    public TestConfigurationFileManager(String testName) throws IOException {
        this.testName = testName;
        this.tempConfigDirectory = createTempConfigDirectory();
        logger.info("Created temporary configuration directory for test '{}': {}", testName, tempConfigDirectory);
    }
    
    /**
     * Create a temporary directory for configuration files
     */
    private Path createTempConfigDirectory() throws IOException {
        Path tempDir = Files.createTempDirectory("postgresql-integration-test-" + testName + "-");
        logger.debug("Created temporary directory: {}", tempDir);
        return tempDir;
    }
    
    /**
     * Write configuration files to the temporary directory
     * 
     * @param configurations Map of configuration type to YAML content
     * @return Map of configuration type to file path
     * @throws IOException if file writing fails
     */
    public Map<String, Path> writeConfigurationFiles(Map<String, String> configurations) throws IOException {
        logger.info("Writing {} configuration files to temporary directory", configurations.size());
        
        Map<String, Path> filePaths = new java.util.HashMap<>();
        
        for (Map.Entry<String, String> entry : configurations.entrySet()) {
            String configType = entry.getKey();
            String yamlContent = entry.getValue();
            
            Path configFile = writeConfigurationFile(configType, yamlContent);
            filePaths.put(configType, configFile);
        }
        
        logger.info("Successfully wrote all configuration files");
        return filePaths;
    }
    
    /**
     * Write a single configuration file
     * 
     * @param configType Type of configuration (databases, queries, endpoints)
     * @param yamlContent YAML content to write
     * @return Path to the written file
     * @throws IOException if file writing fails
     */
    public Path writeConfigurationFile(String configType, String yamlContent) throws IOException {
        String fileName = String.format("postgresql-%s-%s.yml", testName, configType);
        Path configFile = tempConfigDirectory.resolve(fileName);
        
        logger.debug("Writing {} configuration to file: {}", configType, configFile);
        Files.writeString(configFile, yamlContent);
        
        logger.debug("Successfully wrote {} configuration file ({} bytes)", 
                    configType, yamlContent.length());
        return configFile;
    }
    
    /**
     * Create an application configuration file that references the generated configurations
     * 
     * @param configFilePaths Map of configuration type to file path
     * @param serverPort Port for the test server
     * @return Path to the application configuration file
     * @throws IOException if file writing fails
     */
    public Path createApplicationConfiguration(Map<String, Path> configFilePaths, int serverPort) throws IOException {
        logger.info("Creating application configuration file for test server on port {}", serverPort);
        
        StringBuilder appConfig = new StringBuilder();
        appConfig.append("# Application Configuration for PostgreSQL Dual Database Integration Test\n");
        appConfig.append("# Generated for test: ").append(testName).append("\n\n");
        
        // Server configuration
        appConfig.append("server:\n");
        appConfig.append("  host: localhost\n");
        appConfig.append("  port: ").append(serverPort).append("\n");
        appConfig.append("  cors:\n");
        appConfig.append("    enabled: true\n");
        appConfig.append("  dev:\n");
        appConfig.append("    logging: true\n");
        appConfig.append("    requestLogging: true\n\n");
        
        // Database configuration (for API service internal database)
        appConfig.append("database:\n");
        appConfig.append("  url: \"jdbc:h2:mem:api-service-config-").append(testName).append(";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE\"\n");
        appConfig.append("  username: \"sa\"\n");
        appConfig.append("  password: \"\"\n");
        appConfig.append("  driver: \"org.h2.Driver\"\n");
        appConfig.append("  createIfMissing: true\n\n");
        
        // Configuration source and paths
        appConfig.append("config:\n");
        appConfig.append("  source: yaml\n");
        appConfig.append("  loadFromYaml: true\n");
        appConfig.append("  paths:\n");
        appConfig.append("    databases: \"").append(configFilePaths.get("databases").toString()).append("\"\n");
        appConfig.append("    queries: \"").append(configFilePaths.get("queries").toString()).append("\"\n");
        appConfig.append("    endpoints: \"").append(configFilePaths.get("endpoints").toString()).append("\"\n\n");
        
        // Validation configuration
        appConfig.append("validation:\n");
        appConfig.append("  runOnStartup: true\n");
        appConfig.append("  validateOnly: false\n\n");
        
        // Swagger configuration
        appConfig.append("swagger:\n");
        appConfig.append("  enabled: true\n");
        appConfig.append("  path: /swagger\n");
        appConfig.append("  title: \"PostgreSQL Dual Database Integration Test API\"\n");
        appConfig.append("  description: \"Generated API for testing dual PostgreSQL database integration\"\n");
        appConfig.append("  version: \"1.0.0-test\"\n\n");
        
        // Metrics configuration
        appConfig.append("metrics:\n");
        appConfig.append("  enabled: true\n");
        appConfig.append("  collectResponseTimes: true\n");
        appConfig.append("  collectMemoryUsage: false\n\n");
        
        // Logging configuration
        appConfig.append("logging:\n");
        appConfig.append("  level: INFO\n");
        appConfig.append("  requestLogging: true\n");
        
        String fileName = String.format("application-postgresql-%s.yml", testName);
        Path appConfigFile = tempConfigDirectory.resolve(fileName);
        
        Files.writeString(appConfigFile, appConfig.toString());
        
        logger.info("Created application configuration file: {}", appConfigFile);
        return appConfigFile;
    }
    
    /**
     * Get the path to the temporary configuration directory
     * 
     * @return Path to the temporary directory
     */
    public Path getConfigurationDirectory() {
        return tempConfigDirectory;
    }
    
    /**
     * List all configuration files in the temporary directory
     * 
     * @return Map of file name to file path
     * @throws IOException if directory listing fails
     */
    public Map<String, Path> listConfigurationFiles() throws IOException {
        Map<String, Path> files = new java.util.HashMap<>();
        
        try (var stream = Files.list(tempConfigDirectory)) {
            stream.filter(Files::isRegularFile)
                  .filter(path -> path.toString().endsWith(".yml"))
                  .forEach(path -> files.put(path.getFileName().toString(), path));
        }
        
        logger.debug("Found {} configuration files in temporary directory", files.size());
        return files;
    }
    
    /**
     * Validate that all required configuration files exist
     * 
     * @param configFilePaths Map of configuration type to file path
     * @return true if all files exist and are readable, false otherwise
     */
    public boolean validateConfigurationFiles(Map<String, Path> configFilePaths) {
        logger.debug("Validating configuration files");
        
        for (Map.Entry<String, Path> entry : configFilePaths.entrySet()) {
            String configType = entry.getKey();
            Path filePath = entry.getValue();
            
            if (!Files.exists(filePath)) {
                logger.error("Configuration file does not exist: {} ({})", configType, filePath);
                return false;
            }
            
            if (!Files.isReadable(filePath)) {
                logger.error("Configuration file is not readable: {} ({})", configType, filePath);
                return false;
            }
            
            try {
                long fileSize = Files.size(filePath);
                if (fileSize == 0) {
                    logger.error("Configuration file is empty: {} ({})", configType, filePath);
                    return false;
                }
                logger.debug("Configuration file {} is valid ({} bytes)", configType, fileSize);
            } catch (IOException e) {
                logger.error("Failed to check configuration file size: {} ({})", configType, filePath, e);
                return false;
            }
        }
        
        logger.info("All configuration files are valid");
        return true;
    }
    
    /**
     * Clean up all temporary configuration files and directory
     * 
     * @throws IOException if cleanup fails
     */
    public void cleanup() throws IOException {
        if (Files.exists(tempConfigDirectory)) {
            logger.info("Cleaning up temporary configuration directory: {}", tempConfigDirectory);
            
            // Delete all files in the directory
            try (var stream = Files.list(tempConfigDirectory)) {
                stream.forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                        logger.debug("Deleted configuration file: {}", path);
                    } catch (IOException e) {
                        logger.warn("Failed to delete configuration file: {}", path, e);
                    }
                });
            }
            
            // Delete the directory itself
            Files.deleteIfExists(tempConfigDirectory);
            logger.info("Temporary configuration directory cleaned up successfully");
        }
    }
    
    /**
     * Copy generated configuration files to test classpath directory
     * This allows the application to find them using the standard resource loading mechanism
     *
     * @param configFilePaths Map of configuration type to file path
     * @throws IOException if copying fails
     */
    public void copyConfigurationsToTestClasspath(Map<String, Path> configFilePaths) throws IOException {
        logger.info("Copying generated configurations to test classpath directory");

        // Get the test classpath config directory (target/test-classes/config)
        Path testClasspathConfig = Paths.get("target/test-classes/config");

        // Ensure the directory exists
        if (!Files.exists(testClasspathConfig)) {
            Files.createDirectories(testClasspathConfig);
        }

        // Copy each configuration file
        for (Map.Entry<String, Path> entry : configFilePaths.entrySet()) {
            String configType = entry.getKey();
            Path sourceFile = entry.getValue();

            // Determine target filename
            String targetFileName = switch (configType) {
                case "databases" -> "databases.yml";
                case "queries" -> "queries.yml";
                case "endpoints" -> "api-endpoints.yml";
                default -> configType + ".yml";
            };

            Path targetFile = testClasspathConfig.resolve(targetFileName);

            // Copy the file
            Files.copy(sourceFile, targetFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            logger.info("Copied {} configuration to classpath: {}", configType, targetFile);
        }

        logger.info("All configuration files copied to test classpath");
    }

    /**
     * Create a custom application configuration file in test classpath
     *
     * @param serverPort Port for the test server
     * @return Path to the created application configuration file
     * @throws IOException if file creation fails
     */
    public Path createTestClasspathApplicationConfiguration(int serverPort) throws IOException {
        logger.info("Creating application configuration in test classpath for port {}", serverPort);

        Path testClasspath = Paths.get("target/test-classes");
        String configFileName = "application-postgresql-" + testName + ".yml";
        Path appConfigFile = testClasspath.resolve(configFileName);

        StringBuilder appConfig = new StringBuilder();
        appConfig.append("# Application Configuration for PostgreSQL Dual Database Integration Test\n");
        appConfig.append("# Generated for test: ").append(testName).append("\n\n");

        // Server configuration
        appConfig.append("server:\n");
        appConfig.append("  host: localhost\n");
        appConfig.append("  port: ").append(serverPort).append("\n");
        appConfig.append("  cors:\n");
        appConfig.append("    enabled: true\n\n");

        // Database configuration (for API service internal database)
        appConfig.append("database:\n");
        appConfig.append("  url: \"jdbc:h2:mem:api-service-config-").append(testName).append(";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE\"\n");
        appConfig.append("  username: \"sa\"\n");
        appConfig.append("  password: \"\"\n");
        appConfig.append("  driver: \"org.h2.Driver\"\n");
        appConfig.append("  createIfMissing: true\n\n");

        // Configuration source and paths (using test classpath directory)
        appConfig.append("config:\n");
        appConfig.append("  source: yaml\n");
        appConfig.append("  loadFromYaml: true\n");
        appConfig.append("  paths:\n");
        appConfig.append("    databases: config/databases.yml\n");
        appConfig.append("    queries: config/queries.yml\n");
        appConfig.append("    endpoints: config/api-endpoints.yml\n");
        appConfig.append("  # Use test classpath directory instead of default directories\n");
        appConfig.append("  directories:\n");
        appConfig.append("    - \"target/test-classes\"\n");
        appConfig.append("  patterns:\n");
        appConfig.append("    databases: [\"config/databases.yml\"]\n");
        appConfig.append("    queries: [\"config/queries.yml\"]\n");
        appConfig.append("    endpoints: [\"config/api-endpoints.yml\"]\n\n");

        // Validation configuration
        appConfig.append("validation:\n");
        appConfig.append("  runOnStartup: true\n");
        appConfig.append("  validateOnly: false\n\n");

        // Swagger configuration
        appConfig.append("swagger:\n");
        appConfig.append("  enabled: true\n");
        appConfig.append("  path: /swagger\n\n");

        // Metrics configuration
        appConfig.append("metrics:\n");
        appConfig.append("  enabled: true\n");
        appConfig.append("  collectResponseTimes: true\n");
        appConfig.append("  collectMemoryUsage: false\n\n");

        // Logging configuration
        appConfig.append("logging:\n");
        appConfig.append("  level: INFO\n");
        appConfig.append("  requestLogging: true\n");

        Files.writeString(appConfigFile, appConfig.toString());

        logger.info("Created application configuration file: {}", appConfigFile);
        return appConfigFile;
    }

    /**
     * Get a system property string for the application configuration file
     *
     * @param appConfigFile Path to the application configuration file
     * @return System property string
     */
    public static String getApplicationConfigSystemProperty(Path appConfigFile) {
        return "generic.config.file=" + appConfigFile.toString();
    }

    /**
     * Rewrite the application-generic-api.yml with our test configuration
     *
     * @param serverPort Port for the test server
     * @throws IOException if file update fails
     */
    public void updateExistingApplicationConfiguration(int serverPort) throws IOException {
        logger.info("Rewriting application-generic-api.yml for port {} with test configuration", serverPort);

        // Update both source and compiled versions
        Path sourceConfigFile = Paths.get("src/test/resources/application-generic-api.yml");
        Path compiledConfigFile = Paths.get("target/test-classes/application-generic-api.yml");

        // Create a complete new configuration that points to our test classpath
        StringBuilder appConfig = new StringBuilder();
        appConfig.append("# Application Configuration for PostgreSQL Dual Database Integration Test\n");
        appConfig.append("# Rewritten for test: ").append(testName).append("\n\n");

        // Server configuration
        appConfig.append("server:\n");
        appConfig.append("  host: localhost\n");
        appConfig.append("  port: ").append(serverPort).append("\n");
        appConfig.append("  cors:\n");
        appConfig.append("    enabled: true\n\n");

        // Database configuration (for API service internal database)
        appConfig.append("database:\n");
        appConfig.append("  url: \"jdbc:h2:mem:api-service-config-").append(testName).append(";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE\"\n");
        appConfig.append("  username: \"sa\"\n");
        appConfig.append("  password: \"\"\n");
        appConfig.append("  driver: \"org.h2.Driver\"\n");
        appConfig.append("  createIfMissing: true\n\n");

        // Configuration source and paths (using test classpath directory)
        appConfig.append("config:\n");
        appConfig.append("  source: yaml\n");
        appConfig.append("  loadFromYaml: true\n");
        appConfig.append("  # Use test classpath config directory instead of default directories\n");
        appConfig.append("  directories:\n");
        appConfig.append("    - \"target/test-classes/config\"\n");
        appConfig.append("  patterns:\n");
        appConfig.append("    databases: [\"databases.yml\"]\n");
        appConfig.append("    queries: [\"queries.yml\"]\n");
        appConfig.append("    endpoints: [\"api-endpoints.yml\"]\n\n");

        // Validation configuration
        appConfig.append("validation:\n");
        appConfig.append("  runOnStartup: true\n");
        appConfig.append("  validateOnly: false\n\n");

        // Swagger configuration
        appConfig.append("swagger:\n");
        appConfig.append("  enabled: true\n");
        appConfig.append("  path: /swagger\n\n");

        // Metrics configuration
        appConfig.append("metrics:\n");
        appConfig.append("  enabled: true\n");
        appConfig.append("  collectResponseTimes: true\n");
        appConfig.append("  collectMemoryUsage: false\n\n");

        // Logging configuration
        appConfig.append("logging:\n");
        appConfig.append("  level: INFO\n");
        appConfig.append("  requestLogging: true\n");

        // Write the new configuration to both locations
        String configContent = appConfig.toString();
        Files.writeString(sourceConfigFile, configContent);
        Files.writeString(compiledConfigFile, configContent);

        logger.info("Rewrote application-generic-api.yml (both source and compiled) with port {} and test configuration", serverPort);
    }

    /**
     * Temporarily disable default configuration files by renaming the generic-config directory
     * This prevents the application from loading default databases that conflict with our test databases
     *
     * @return Path to the renamed directory (for restoration later)
     * @throws IOException if directory operations fail
     */
    public Path temporarilyDisableDefaultConfigurations() throws IOException {
        logger.info("Temporarily disabling default configuration files to prevent conflicts");

        // Try both possible locations for the generic-config directory
        Path[] possibleDirs = {
            Paths.get("generic-config"),
            Paths.get("../generic-config")
        };

        for (Path genericConfigDir : possibleDirs) {
            if (Files.exists(genericConfigDir)) {
                Path backupDir = genericConfigDir.getParent().resolve("generic-config-backup-" + testName);

                // Move the directory to a backup location
                Files.move(genericConfigDir, backupDir);
                logger.info("Moved generic-config directory from {} to: {}", genericConfigDir, backupDir);
                return backupDir;
            }
        }

        logger.info("No generic-config directory found in expected locations, no action needed");
        return null;
    }

    /**
     * Restore default configuration files by moving the backup directory back
     *
     * @param backupDir Path to the backup directory (returned by temporarilyDisableDefaultConfigurations)
     * @throws IOException if directory operations fail
     */
    public void restoreDefaultConfigurations(Path backupDir) throws IOException {
        if (backupDir != null && Files.exists(backupDir)) {
            logger.info("Restoring default configuration files from: {}", backupDir);

            // Determine the original directory path from the backup path
            Path originalDir = backupDir.getParent().resolve("generic-config");

            // Remove the current directory if it exists
            if (Files.exists(originalDir)) {
                // Delete the directory and its contents
                Files.walk(originalDir)
                     .sorted(java.util.Comparator.reverseOrder())
                     .map(Path::toFile)
                     .forEach(java.io.File::delete);
            }

            // Move the backup back
            Files.move(backupDir, originalDir);
            logger.info("Restored generic-config directory to: {}", originalDir);
        } else {
            logger.info("No backup directory to restore");
        }
    }

    /**
     * Get the resource name for an application configuration file
     *
     * @param appConfigFile Path to the application configuration file
     * @return Resource name (filename only)
     */
    public static String getApplicationConfigResourceName(Path appConfigFile) {
        return appConfigFile.getFileName().toString();
    }
}
