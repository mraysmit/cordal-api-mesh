package dev.mars.generic.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite that validates all aspects of configuration loading
 * and management API data consistency. This suite ensures that:
 * 
 * 1. Configurations are loaded from the exact paths defined in application.yaml
 * 2. Management APIs return the actual data that was loaded from configuration sources
 * 3. Configuration sources (YAML/database) work consistently
 * 4. Cross-validation between different data access methods is accurate
 */
@DisplayName("Comprehensive Configuration Test Suite")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ComprehensiveConfigurationTestSuite {
    private static final Logger logger = LoggerFactory.getLogger(ComprehensiveConfigurationTestSuite.class);

    @BeforeAll
    void setUpSuite() {
        logger.info("Starting Comprehensive Configuration Test Suite");
        logger.info("This suite validates configuration loading and management API consistency");
    }

    @AfterAll
    void tearDownSuite() {
        logger.info("Completed Comprehensive Configuration Test Suite");
    }

    @Nested
    @DisplayName("Configuration Path Verification Tests")
    class ConfigurationPathTests {
        
        private final ConfigurationPathVerificationTest pathTests = new ConfigurationPathVerificationTest();

        @Test
        @DisplayName("Should load configurations from default paths")
        void testDefaultPaths() {
            pathTests.setUp();
            try {
                pathTests.testConfigurationLoadedFromDefaultPaths();
                logger.info("✓ Default path configuration loading verified");
            } finally {
                pathTests.tearDown();
            }
        }

        @Test
        @DisplayName("Should load configurations from custom paths")
        void testCustomPaths() {
            pathTests.setUp();
            try {
                pathTests.testConfigurationLoadedFromCustomPaths();
                logger.info("✓ Custom path configuration loading verified");
            } finally {
                pathTests.tearDown();
            }
        }

        @Test
        @DisplayName("Should load configurations from test paths")
        void testTestPaths() {
            pathTests.setUp();
            try {
                pathTests.testConfigurationLoadedFromTestPaths();
                logger.info("✓ Test path configuration loading verified");
            } finally {
                pathTests.tearDown();
            }
        }

        @Test
        @DisplayName("Should verify loaded data matches source files")
        void testDataMatches() {
            pathTests.setUp();
            try {
                pathTests.testLoadedDataMatchesSourceFiles();
                logger.info("✓ Configuration data consistency with source files verified");
            } finally {
                pathTests.tearDown();
            }
        }

        @Test
        @DisplayName("Should maintain configuration consistency across different paths")
        void testConsistency() {
            pathTests.setUp();
            try {
                pathTests.testConfigurationConsistencyAcrossDifferentPaths();
                logger.info("✓ Configuration structure consistency verified");
            } finally {
                pathTests.tearDown();
            }
        }
    }

    @Nested
    @DisplayName("Management API Data Verification Tests")
    class ManagementApiTests {
        
        private final dev.mars.generic.management.ManagementApiDataVerificationTest apiTests = new dev.mars.generic.management.ManagementApiDataVerificationTest();

        @Test
        @DisplayName("Should return loaded database configurations via API")
        void testDatabaseApi() {
            apiTests.setUp();
            try {
                apiTests.testManagementApiReturnsLoadedDatabaseConfigurations();
                logger.info("✓ Management API database configuration data verified");
            } finally {
                apiTests.tearDown();
            }
        }

        @Test
        @DisplayName("Should return loaded query configurations via API")
        void testQueryApi() {
            apiTests.setUp();
            try {
                apiTests.testManagementApiReturnsLoadedQueryConfigurations();
                logger.info("✓ Management API query configuration data verified");
            } finally {
                apiTests.tearDown();
            }
        }

        @Test
        @DisplayName("Should return loaded endpoint configurations via API")
        void testEndpointApi() {
            apiTests.setUp();
            try {
                apiTests.testManagementApiReturnsLoadedEndpointConfigurations();
                logger.info("✓ Management API endpoint configuration data verified");
            } finally {
                apiTests.tearDown();
            }
        }

        @Test
        @DisplayName("Should return accurate configuration metadata")
        void testMetadataApi() {
            apiTests.setUp();
            try {
                apiTests.testManagementApiConfigurationMetadata();
                logger.info("✓ Management API configuration metadata verified");
            } finally {
                apiTests.tearDown();
            }
        }

        @Test
        @DisplayName("Should return correct configuration paths")
        void testPathsApi() {
            apiTests.setUp();
            try {
                apiTests.testManagementApiConfigurationPaths();
                logger.info("✓ Management API configuration paths verified");
            } finally {
                apiTests.tearDown();
            }
        }

        @Test
        @DisplayName("Should handle API errors gracefully")
        void testErrorHandling() {
            apiTests.setUp();
            try {
                apiTests.testManagementApiErrorHandling();
                logger.info("✓ Management API error handling verified");
            } finally {
                apiTests.tearDown();
            }
        }
    }

    @Nested
    @DisplayName("Configuration Source Integration Tests")
    class ConfigurationSourceTests {
        
        private final ConfigurationSourceIntegrationTest sourceTests = new ConfigurationSourceIntegrationTest();

        @Test
        @DisplayName("Should load configurations from YAML source")
        void testYamlSource() {
            sourceTests.setUp();
            try {
                sourceTests.testYamlConfigurationSourceLoading();
                logger.info("✓ YAML configuration source loading verified");
            } finally {
                sourceTests.tearDown();
            }
        }

        @Test
        @DisplayName("Should load configurations from database source")
        void testDatabaseSource() {
            sourceTests.setUp();
            try {
                sourceTests.testDatabaseConfigurationSourceLoading();
                logger.info("✓ Database configuration source loading verified");
            } finally {
                sourceTests.tearDown();
            }
        }

        @Test
        @DisplayName("Should maintain consistency across configuration sources")
        void testSourceConsistency() {
            sourceTests.setUp();
            try {
                sourceTests.testConfigurationSourceConsistency();
                logger.info("✓ Configuration source consistency verified");
            } finally {
                sourceTests.tearDown();
            }
        }

        @Test
        @DisplayName("Should handle configuration source switching")
        void testSourceSwitching() {
            sourceTests.setUp();
            try {
                sourceTests.testConfigurationSourceSwitching();
                logger.info("✓ Configuration source switching verified");
            } finally {
                sourceTests.tearDown();
            }
        }

        @Test
        @DisplayName("Should maintain data integrity across sources")
        void testDataIntegrity() {
            sourceTests.setUp();
            try {
                sourceTests.testConfigurationSourceDataIntegrity();
                logger.info("✓ Configuration source data integrity verified");
            } finally {
                sourceTests.tearDown();
            }
        }
    }

    @Nested
    @DisplayName("Configuration Consistency Tests")
    class ConfigurationConsistencyTests {
        
        private final ConfigurationConsistencyTest consistencyTests = new ConfigurationConsistencyTest();

        @Test
        @DisplayName("Should maintain consistency between ConfigurationLoader and Management APIs")
        void testLoaderApiConsistency() {
            consistencyTests.setUp();
            try {
                consistencyTests.testConfigurationLoaderAndManagementApiConsistency();
                logger.info("✓ ConfigurationLoader and Management API consistency verified");
            } finally {
                consistencyTests.tearDown();
            }
        }

        @Test
        @DisplayName("Should provide accurate configuration metadata")
        void testMetadataAccuracy() {
            consistencyTests.setUp();
            try {
                consistencyTests.testConfigurationMetadataAccuracy();
                logger.info("✓ Configuration metadata accuracy verified");
            } finally {
                consistencyTests.tearDown();
            }
        }

        @Test
        @DisplayName("Should maintain path consistency across different configurations")
        void testPathConsistency() {
            consistencyTests.setUp();
            try {
                consistencyTests.testConfigurationPathsConsistency();
                logger.info("✓ Configuration paths consistency verified");
            } finally {
                consistencyTests.tearDown();
            }
        }

        @Test
        @DisplayName("Should validate configuration data integrity")
        void testDataIntegrityValidation() {
            consistencyTests.setUp();
            try {
                consistencyTests.testConfigurationDataIntegrityValidation();
                logger.info("✓ Configuration data integrity validation verified");
            } finally {
                consistencyTests.tearDown();
            }
        }

        @Test
        @DisplayName("Should maintain timestamp consistency")
        void testTimestampConsistency() {
            consistencyTests.setUp();
            try {
                consistencyTests.testConfigurationTimestampConsistency();
                logger.info("✓ Configuration timestamp consistency verified");
            } finally {
                consistencyTests.tearDown();
            }
        }

        @Test
        @DisplayName("Should perform comprehensive cross-validation")
        void testCrossValidation() {
            consistencyTests.setUp();
            try {
                consistencyTests.testCrossValidationBetweenConfigurationAndManagementApis();
                logger.info("✓ Comprehensive cross-validation completed successfully");
            } finally {
                consistencyTests.tearDown();
            }
        }
    }

    @Test
    @DisplayName("Integration Test: Full Configuration Coverage Validation")
    void testFullConfigurationCoverage() {
        logger.info("Running full configuration coverage validation...");
        
        // This test ensures that all configuration aspects work together
        assertThatCode(() -> {
            // Test path verification
            ConfigurationPathVerificationTest pathTest = new ConfigurationPathVerificationTest();
            pathTest.setUp();
            pathTest.testConfigurationLoadedFromTestPaths();
            pathTest.tearDown();
            
            // Test management API data verification
            dev.mars.generic.management.ManagementApiDataVerificationTest apiTest = new dev.mars.generic.management.ManagementApiDataVerificationTest();
            apiTest.setUp();
            apiTest.testManagementApiReturnsLoadedDatabaseConfigurations();
            apiTest.tearDown();
            
            // Test configuration consistency
            ConfigurationConsistencyTest consistencyTest = new ConfigurationConsistencyTest();
            consistencyTest.setUp();
            consistencyTest.testConfigurationLoaderAndManagementApiConsistency();
            consistencyTest.tearDown();
            
        }).doesNotThrowAnyException();
        
        logger.info("✓ Full configuration coverage validation completed successfully");
        logger.info("All configuration loading and management API tests passed!");
    }
}
