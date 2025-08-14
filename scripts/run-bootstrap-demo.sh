#!/bin/bash

echo "========================================"
echo "System Bootstrap Demonstration"
echo "========================================"
echo
echo "This script will:"
echo "1. Build the project"
echo "2. Run the bootstrap demonstration"
echo "3. Show system startup and API testing"
echo
read -p "Press Enter to continue..."

echo
echo "Building project..."
mvn clean install -q
if [ $? -ne 0 ]; then
    echo "ERROR: Build failed!"
    exit 1
fi

echo
echo "Starting bootstrap demonstration..."
echo
cd cordal-api-service
mvn exec:java -Dexec.mainClass="dev.mars.bootstrap.SystemBootstrapDemo" -q

echo
echo "Bootstrap demonstration completed."
read -p "Press Enter to exit..."
