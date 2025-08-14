#!/bin/bash
# Metrics Service Startup Script for Unix/Linux
# This script starts the Metrics Service using the executable JAR

set -e

# Set script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
SERVICE_DIR="$PROJECT_ROOT/cordal-metrics-service"

echo "================================================================================"
echo "                          Metrics Service Startup"
echo "================================================================================"
echo

# Find the most recent executable JAR (supports different profiles)
EXECUTABLE_JAR=""
for jar_pattern in "executable" "optimized" "dev" "thin"; do
    jar_file="$SERVICE_DIR/target/cordal-metrics-service-1.0-SNAPSHOT-$jar_pattern.jar"
    if [ -f "$jar_file" ]; then
        EXECUTABLE_JAR="$jar_file"
        JAR_TYPE="$jar_pattern"
        break
    fi
done

if [ -z "$EXECUTABLE_JAR" ]; then
    echo "[ERROR] No executable JAR found in: $SERVICE_DIR/target/"
    echo "Looked for patterns: *-executable.jar, *-optimized.jar, *-dev.jar, *-thin.jar"
    echo
    echo "Please build the project first:"
    echo "  cd \"$PROJECT_ROOT\""
    echo "  ./scripts/build-executable-jars.sh"
    echo
    exit 1
fi

echo "[INFO] Found JAR: $EXECUTABLE_JAR (type: $JAR_TYPE)"

# Check Java version
if ! command -v java &> /dev/null; then
    echo "[ERROR] Java is not installed or not in PATH"
    echo "Please install Java 21 or later"
    exit 1
fi

# Set JVM options for better performance
JVM_OPTS="${JVM_OPTS:--Xms256m -Xmx1g -XX:+UseG1GC -XX:+UseStringDeduplication}"

# Set application properties
APP_OPTS="${APP_OPTS:-}"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --help|-h)
            show_help
            exit 0
            ;;
        *)
            echo "[ERROR] Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

show_help() {
    echo "Usage: $(basename "$0") [OPTIONS]"
    echo
    echo "Options:"
    echo "  --help, -h                     Show this help message"
    echo
    echo "Environment Variables:"
    echo "  JVM_OPTS                       Additional JVM options (default: $JVM_OPTS)"
    echo "  APP_OPTS                       Additional application options"
    echo
    echo "Examples:"
    echo "  $(basename "$0")                          Start the service normally"
    echo
}

# Function to start the service
start_service() {
    echo "[INFO] Starting Metrics Service..."
    echo "[INFO] JAR: $EXECUTABLE_JAR"
    echo "[INFO] JVM Options: $JVM_OPTS"
    [ -n "$APP_OPTS" ] && echo "[INFO] App Options: $APP_OPTS"
    echo "[INFO] Working Directory: $SERVICE_DIR"
    echo

    # Change to service directory for proper relative path resolution
    cd "$SERVICE_DIR"

    # Start the service
    echo "[INFO] Executing: java $JVM_OPTS -jar \"$EXECUTABLE_JAR\" $APP_OPTS"
    echo
    
    # Use exec to replace the shell process with Java process for proper signal handling
    exec java $JVM_OPTS -jar "$EXECUTABLE_JAR" $APP_OPTS
}

# Set up signal handlers for graceful shutdown
trap 'echo; echo "[INFO] Received shutdown signal, stopping service..."; exit 0' SIGTERM SIGINT

# Start the service
start_service
