#!/bin/bash
# Master Startup Script for All Services (Unix/Linux)
# This script starts both Generic API Service and Metrics Service

set -e

# Set script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "================================================================================"
echo "                        Javalin API Mesh - All Services"
echo "================================================================================"
echo

# Find executable JARs (supports different profiles)
GENERIC_API_JAR=""
METRICS_JAR=""

# Find Generic API Service JAR
for jar_pattern in "executable" "optimized" "dev" "thin"; do
    jar_file="$PROJECT_ROOT/generic-api-service/target/generic-api-service-1.0-SNAPSHOT-$jar_pattern.jar"
    if [ -f "$jar_file" ]; then
        GENERIC_API_JAR="$jar_file"
        GENERIC_API_TYPE="$jar_pattern"
        break
    fi
done

# Find Metrics Service JAR
for jar_pattern in "executable" "optimized" "dev" "thin"; do
    jar_file="$PROJECT_ROOT/metrics-service/target/metrics-service-1.0-SNAPSHOT-$jar_pattern.jar"
    if [ -f "$jar_file" ]; then
        METRICS_JAR="$jar_file"
        METRICS_TYPE="$jar_pattern"
        break
    fi
done

MISSING_JARS=false
if [ -z "$GENERIC_API_JAR" ]; then
    echo "[ERROR] Generic API Service JAR not found in: $PROJECT_ROOT/generic-api-service/target/"
    echo "Looked for patterns: *-executable.jar, *-optimized.jar, *-dev.jar, *-thin.jar"
    MISSING_JARS=true
fi
if [ -z "$METRICS_JAR" ]; then
    echo "[ERROR] Metrics Service JAR not found in: $PROJECT_ROOT/metrics-service/target/"
    echo "Looked for patterns: *-executable.jar, *-optimized.jar, *-dev.jar, *-thin.jar"
    MISSING_JARS=true
fi

if [ "$MISSING_JARS" = true ]; then
    echo
    echo "Please build the project first:"
    echo "  cd \"$PROJECT_ROOT\""
    echo "  ./scripts/build-executable-jars.sh"
    echo
    exit 1
fi

echo "[INFO] Found Generic API JAR: $GENERIC_API_JAR (type: $GENERIC_API_TYPE)"
echo "[INFO] Found Metrics JAR: $METRICS_JAR (type: $METRICS_TYPE)"

# Check Java version
if ! command -v java &> /dev/null; then
    echo "[ERROR] Java is not installed or not in PATH"
    echo "Please install Java 21 or later"
    exit 1
fi

# Parse command line arguments
START_MODE="both"
VALIDATE_ONLY=false
BACKGROUND=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --generic-api-only)
            START_MODE="generic-api"
            shift
            ;;
        --metrics-only)
            START_MODE="metrics"
            shift
            ;;
        --validate-only|--validate)
            VALIDATE_ONLY=true
            shift
            ;;
        --background|-b)
            BACKGROUND=true
            shift
            ;;
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
    echo "  --generic-api-only             Start only Generic API Service"
    echo "  --metrics-only                 Start only Metrics Service"
    echo "  --validate-only, --validate    Run configuration validation only and exit"
    echo "  --background, -b               Run services in background"
    echo "  --help, -h                     Show this help message"
    echo
    echo "Default behavior: Start both services in foreground"
    echo
    echo "Examples:"
    echo "  $(basename "$0")                          Start both services"
    echo "  $(basename "$0") --generic-api-only       Start only Generic API Service"
    echo "  $(basename "$0") --metrics-only           Start only Metrics Service"
    echo "  $(basename "$0") --validate-only          Validate configurations and exit"
    echo "  $(basename "$0") --background             Start both services in background"
    echo
}

# Function to start services
start_services() {
    echo "[INFO] Starting services in mode: $START_MODE"
    [ "$VALIDATE_ONLY" = true ] && echo "[INFO] Validation mode enabled"
    [ "$BACKGROUND" = true ] && echo "[INFO] Background mode enabled"
    echo

    case $START_MODE in
        generic-api)
            start_generic_api_only
            ;;
        metrics)
            start_metrics_only
            ;;
        both)
            start_both_services
            ;;
    esac
}

start_generic_api_only() {
    echo "[INFO] Starting Generic API Service only..."
    if [ "$VALIDATE_ONLY" = true ]; then
        "$SCRIPT_DIR/start-generic-api-service.sh" --validate-only
    else
        if [ "$BACKGROUND" = true ]; then
            nohup "$SCRIPT_DIR/start-generic-api-service.sh" > "$PROJECT_ROOT/logs/generic-api-service.log" 2>&1 &
            echo "[INFO] Generic API Service started in background (PID: $!)"
            echo "[INFO] Log file: $PROJECT_ROOT/logs/generic-api-service.log"
        else
            "$SCRIPT_DIR/start-generic-api-service.sh"
        fi
    fi
}

start_metrics_only() {
    echo "[INFO] Starting Metrics Service only..."
    if [ "$BACKGROUND" = true ]; then
        nohup "$SCRIPT_DIR/start-metrics-service.sh" > "$PROJECT_ROOT/logs/metrics-service.log" 2>&1 &
        echo "[INFO] Metrics Service started in background (PID: $!)"
        echo "[INFO] Log file: $PROJECT_ROOT/logs/metrics-service.log"
    else
        "$SCRIPT_DIR/start-metrics-service.sh"
    fi
}

start_both_services() {
    echo "[INFO] Starting both services..."
    
    if [ "$VALIDATE_ONLY" = true ]; then
        echo "[INFO] Validating Generic API Service configuration..."
        "$SCRIPT_DIR/start-generic-api-service.sh" --validate-only
        return
    fi
    
    if [ "$BACKGROUND" = true ]; then
        # Create logs directory if it doesn't exist
        mkdir -p "$PROJECT_ROOT/logs"
        
        echo "[INFO] Starting Generic API Service in background..."
        nohup "$SCRIPT_DIR/start-generic-api-service.sh" > "$PROJECT_ROOT/logs/generic-api-service.log" 2>&1 &
        GENERIC_API_PID=$!
        
        echo "[INFO] Waiting 5 seconds before starting Metrics Service..."
        sleep 5
        
        echo "[INFO] Starting Metrics Service in background..."
        nohup "$SCRIPT_DIR/start-metrics-service.sh" > "$PROJECT_ROOT/logs/metrics-service.log" 2>&1 &
        METRICS_PID=$!
        
        echo
        echo "[INFO] Both services started in background:"
        echo "[INFO] Generic API Service: PID $GENERIC_API_PID, Log: $PROJECT_ROOT/logs/generic-api-service.log"
        echo "[INFO] Metrics Service: PID $METRICS_PID, Log: $PROJECT_ROOT/logs/metrics-service.log"
        echo "[INFO] Generic API Service: http://localhost:8080"
        echo "[INFO] Metrics Service: http://localhost:8081"
        echo
        echo "To stop the services:"
        echo "  kill $GENERIC_API_PID $METRICS_PID"
        
    else
        echo "[INFO] Starting services in foreground (use Ctrl+C to stop)..."
        echo "[INFO] Note: This will start Generic API Service first, then Metrics Service"
        echo
        
        # Start Generic API Service in background
        "$SCRIPT_DIR/start-generic-api-service.sh" &
        GENERIC_API_PID=$!
        
        # Wait a moment
        sleep 5
        
        # Start Metrics Service in foreground (this will block)
        "$SCRIPT_DIR/start-metrics-service.sh" &
        METRICS_PID=$!
        
        # Set up signal handlers
        trap 'echo; echo "[INFO] Stopping services..."; kill $GENERIC_API_PID $METRICS_PID 2>/dev/null; exit 0' SIGTERM SIGINT
        
        # Wait for both processes
        wait $GENERIC_API_PID $METRICS_PID
    fi
}

# Start the services
start_services
