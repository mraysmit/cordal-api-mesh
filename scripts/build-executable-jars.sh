#!/bin/bash
# Build Script for Executable JARs (Unix/Linux)
# This script builds the project and creates executable fat JARs

set -e

# Set script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "================================================================================"
echo "                        Building Executable JARs"
echo "================================================================================"
echo

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "[ERROR] Maven is not installed or not in PATH"
    echo "Please install Apache Maven 3.6+ and add it to your PATH"
    exit 1
fi

# Parse command line arguments
SKIP_TESTS=true
CLEAN_BUILD=true
PROFILE=""
ANALYZE_JARS=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --run-tests)
            SKIP_TESTS=false
            shift
            ;;
        --no-clean)
            CLEAN_BUILD=false
            shift
            ;;
        --profile)
            PROFILE="-P$2"
            shift 2
            ;;
        --fat-jar)
            PROFILE="-Pfat-jar"
            shift
            ;;
        --thin-jar)
            PROFILE="-Pthin-jar"
            shift
            ;;
        --optimized-jar)
            PROFILE="-Poptimized-jar"
            shift
            ;;
        --dev)
            PROFILE="-Pdev"
            shift
            ;;
        --analyze)
            ANALYZE_JARS=true
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
    echo "  --run-tests                    Run tests during build (default: skip tests)"
    echo "  --no-clean                     Skip clean phase (default: run clean)"
    echo "  --profile PROFILE              Activate Maven profile"
    echo "  --fat-jar                      Build fat JARs with all dependencies (default)"
    echo "  --thin-jar                     Build thin JARs with minimal dependencies"
    echo "  --optimized-jar                Build optimized JARs (fat + minimized)"
    echo "  --dev                          Build development JARs (fast build, skip tests)"
    echo "  --analyze                      Run JAR analysis after build"
    echo "  --help, -h                     Show this help message"
    echo
    echo "Distribution Profiles:"
    echo "  fat-jar (default)              Self-contained JARs with all dependencies (~20MB)"
    echo "  thin-jar                       Minimal JARs requiring external classpath (~1MB)"
    echo "  optimized-jar                  Fat JARs with unused classes removed (~15MB)"
    echo "  dev                            Fast development builds, skip tests"
    echo
    echo "Examples:"
    echo "  $(basename "$0")                          Build fat JARs (default)"
    echo "  $(basename "$0") --thin-jar               Build thin JARs"
    echo "  $(basename "$0") --optimized-jar --analyze Build optimized JARs and analyze"
    echo "  $(basename "$0") --dev                    Fast development build"
    echo "  $(basename "$0") --run-tests              Build with tests"
    echo
}

# Function to start build
start_build() {
    echo "[INFO] Build Configuration:"
    echo "[INFO]   Project Root: $PROJECT_ROOT"
    echo "[INFO]   Clean Build: $CLEAN_BUILD"
    echo "[INFO]   Skip Tests: $SKIP_TESTS"
    [ -n "$PROFILE" ] && echo "[INFO]   Maven Profile: $PROFILE"
    echo

    # Change to project root directory
    cd "$PROJECT_ROOT"

    # Build Maven command
    MVN_CMD="mvn"
    [ "$CLEAN_BUILD" = true ] && MVN_CMD="$MVN_CMD clean"
    MVN_CMD="$MVN_CMD package"
    [ "$SKIP_TESTS" = true ] && MVN_CMD="$MVN_CMD -DskipTests"
    [ -n "$PROFILE" ] && MVN_CMD="$MVN_CMD $PROFILE"

    echo "[INFO] Executing: $MVN_CMD"
    echo

    # Execute Maven build
    $MVN_CMD

    echo
    echo "================================================================================"
    echo "                            Build Completed Successfully"
    echo "================================================================================"
    echo

    # Determine JAR classifier based on profile
    JAR_CLASSIFIER="executable"
    if [[ "$PROFILE" == *"thin-jar"* ]]; then
        JAR_CLASSIFIER="thin"
    elif [[ "$PROFILE" == *"optimized-jar"* ]]; then
        JAR_CLASSIFIER="optimized"
    elif [[ "$PROFILE" == *"dev"* ]]; then
        JAR_CLASSIFIER="dev"
    fi

    # Check if JARs were created
    GENERIC_API_JAR="$PROJECT_ROOT/generic-api-service/target/generic-api-service-1.0-SNAPSHOT-$JAR_CLASSIFIER.jar"
    METRICS_JAR="$PROJECT_ROOT/metrics-service/target/metrics-service-1.0-SNAPSHOT-$JAR_CLASSIFIER.jar"

    echo "[INFO] Checking created artifacts ($JAR_CLASSIFIER profile):"
    if [ -f "$GENERIC_API_JAR" ]; then
        echo "[OK] Generic API Service JAR: $GENERIC_API_JAR"
        echo "     Size: $(du -h "$GENERIC_API_JAR" | cut -f1)"
    else
        echo "[ERROR] Generic API Service JAR not found!"
    fi

    if [ -f "$METRICS_JAR" ]; then
        echo "[OK] Metrics Service JAR: $METRICS_JAR"
        echo "     Size: $(du -h "$METRICS_JAR" | cut -f1)"
    else
        echo "[ERROR] Metrics Service JAR not found!"
    fi

    # Run JAR analysis if requested
    if [ "$ANALYZE_JARS" = true ]; then
        echo
        echo "================================================================================"
        echo "                            JAR Analysis"
        echo "================================================================================"
        if [ -f "$SCRIPT_DIR/analyze-jars.sh" ]; then
            "$SCRIPT_DIR/analyze-jars.sh" --profile "$JAR_CLASSIFIER"
        else
            echo "[WARNING] JAR analysis script not found: $SCRIPT_DIR/analyze-jars.sh"
        fi
    fi

    echo
    echo "[INFO] You can now start the services using:"
    echo "[INFO]   $SCRIPT_DIR/start-all-services.sh"
    echo "[INFO]   $SCRIPT_DIR/start-generic-api-service.sh"
    echo "[INFO]   $SCRIPT_DIR/start-metrics-service.sh"
    echo
    echo "[INFO] For JAR analysis, run:"
    echo "[INFO]   $SCRIPT_DIR/analyze-jars.sh --profile $JAR_CLASSIFIER"
    echo
}

# Start the build
start_build
