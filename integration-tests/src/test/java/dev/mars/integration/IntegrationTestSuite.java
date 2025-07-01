package dev.mars.integration;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Integration Test Suite for running all integration tests
 */
@Suite
@SuiteDisplayName("Modular Services Integration Test Suite")
@SelectClasses({
    ModularServicesIntegrationTest.class,
    ConfigurationIntegrationTest.class,
    PerformanceIntegrationTest.class
})
public class IntegrationTestSuite {
    // Test suite runner - no implementation needed
}
