#!/bin/bash

# Dual PostgreSQL Integration Test Runner
# This script runs the comprehensive dual PostgreSQL database integration test

echo "================================================================================"
echo "                    Dual PostgreSQL Integration Test"
echo "================================================================================"
echo
echo "This test demonstrates the CORDAL framework's capability to:"
echo "  - Connect to multiple similar PostgreSQL databases"
echo "  - Create standardized configurations dynamically"
echo "  - Generate REST API endpoints for both databases"
echo "  - Validate data consistency across databases"
echo "  - Test the complete end-to-end flow as a REST API consumer"
echo
echo "The test is completely self-contained using TestContainers with no external"
echo "dependencies. It will automatically:"
echo "  1. Start two PostgreSQL containers"
echo "  2. Create identical schemas and populate with 100 test records each"
echo "  3. Generate YAML configurations dynamically"
echo "  4. Start the Generic API Service"
echo "  5. Test all REST endpoints comprehensively"
echo "  6. Validate data consistency and performance"
echo "  7. Clean up all resources"
echo
echo "Prerequisites:"
echo "  - Java 21 or higher"
echo "  - Maven 3.8 or higher"
echo "  - Docker (for TestContainers)"
echo
echo "================================================================================"

# Check if Docker is running
if ! docker info >/dev/null 2>&1; then
    echo "ERROR: Docker is not running or not accessible"
    echo "Please start Docker and try again"
    echo
    exit 1
fi

# Check if we're in the right directory
if [ ! -f "pom.xml" ]; then
    echo "ERROR: This script must be run from the integration-tests directory"
    echo "Current directory: $(pwd)"
    echo
    exit 1
fi

echo "Starting test execution..."
echo

# Run the integration test
mvn test -Dtest=DualPostgreSQLIntegrationTest -q

if [ $? -eq 0 ]; then
    echo
    echo "================================================================================"
    echo "                           TEST COMPLETED SUCCESSFULLY!"
    echo "================================================================================"
    echo
    echo "The dual PostgreSQL integration test has completed successfully."
    echo
    echo "Key achievements:"
    echo "  ✓ Two PostgreSQL containers started and configured"
    echo "  ✓ Identical schemas created with 100 test records each"
    echo "  ✓ YAML configurations generated dynamically"
    echo "  ✓ Generic API Service started with dual database config"
    echo "  ✓ All REST endpoints tested and validated"
    echo "  ✓ Data consistency verified across both databases"
    echo "  ✓ Performance monitoring completed"
    echo "  ✓ All resources cleaned up properly"
    echo
    echo "This demonstrates the framework's powerful capability to standardize"
    echo "access to multiple similar databases through unified REST APIs with"
    echo "zero code required - just configuration!"
    echo
else
    echo
    echo "================================================================================"
    echo "                              TEST FAILED"
    echo "================================================================================"
    echo
    echo "The integration test failed. Please check the output above for details."
    echo
    echo "Common troubleshooting steps:"
    echo "  1. Ensure Docker is running and accessible"
    echo "  2. Check that port 19080 is available"
    echo "  3. Verify sufficient memory is available for containers"
    echo "  4. Check the logs for specific error messages"
    echo
    echo "For detailed logging, run:"
    echo "  mvn test -Dtest=DualPostgreSQLIntegrationTest"
    echo
fi

echo "Press Enter to exit..."
read
