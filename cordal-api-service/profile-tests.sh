#!/bin/bash
# Test Profiling Script for Linux/Mac
# This script runs tests with profiling enabled and generates performance reports

set -e

echo "================================================================================"
echo "                           TEST PROFILING UTILITY"
echo "================================================================================"
echo

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven is not available in PATH"
    echo "Please install Maven or add it to your PATH"
    exit 1
fi

# Default settings
PROFILE="test-profiling"
CLEAN=false
SHOW_REPORTS=true

# Function to show help
show_help() {
    cat << EOF
Usage: $0 [OPTIONS]

Options:
  --profile PROFILE    Use specific Maven profile (default: test-profiling)
                      Available profiles: test-profiling, test-benchmark
  --clean             Clean before running tests
  --no-reports        Don't open reports after completion
  --help              Show this help message

Examples:
  $0                          # Run with default profiling
  $0 --profile test-benchmark # Run benchmark profiling
  $0 --clean                  # Clean and run profiling

EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --profile)
            PROFILE="$2"
            shift 2
            ;;
        --clean)
            CLEAN=true
            shift
            ;;
        --no-reports)
            SHOW_REPORTS=false
            shift
            ;;
        --help)
            show_help
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

echo "Profile: $PROFILE"
echo "Clean build: $CLEAN"
echo "Show reports: $SHOW_REPORTS"
echo

# Clean if requested
if [ "$CLEAN" = true ]; then
    echo "Cleaning project..."
    mvn clean
    echo
fi

# Run tests with profiling
echo "Running tests with profiling enabled..."
echo "Command: mvn test -P$PROFILE"
echo

mvn test -P$PROFILE
TEST_RESULT=$?

echo
echo "================================================================================"
echo "                           PROFILING RESULTS"
echo "================================================================================"

# Determine report directory
REPORT_DIR="target/test-profiling"
case $PROFILE in
    test-benchmark)
        REPORT_DIR="target/test-benchmark"
        ;;
    test-profiling)
        REPORT_DIR="target/test-profiling-detailed"
        ;;
esac

if [ -d "$REPORT_DIR" ]; then
    echo
    echo "Profiling reports generated in: $REPORT_DIR"
    echo
    
    # Show summary if available
    if [ -f "$REPORT_DIR/performance-summary.txt" ]; then
        echo "PERFORMANCE SUMMARY:"
        echo "----------------------------------------"
        cat "$REPORT_DIR/performance-summary.txt"
        echo
    fi
    
    # List available reports
    echo "AVAILABLE REPORTS:"
    echo "----------------------------------------"
    ls -1 "$REPORT_DIR"/*.txt "$REPORT_DIR"/*.csv 2>/dev/null || echo "No report files found"
    echo
    
    # Open reports if requested and on macOS
    if [ "$SHOW_REPORTS" = true ]; then
        if [[ "$OSTYPE" == "darwin"* ]]; then
            echo "Opening reports directory..."
            open "$REPORT_DIR"
            
            # Open summary report if available
            if [ -f "$REPORT_DIR/performance-summary.txt" ]; then
                open -a TextEdit "$REPORT_DIR/performance-summary.txt"
            fi
        elif command -v xdg-open &> /dev/null; then
            echo "Opening reports directory..."
            xdg-open "$REPORT_DIR"
        else
            echo "Reports directory: $REPORT_DIR"
            echo "Use your file manager to open the reports directory"
        fi
    fi
    
else
    echo "WARNING: No profiling reports found in $REPORT_DIR"
    echo "This might indicate that profiling was not enabled or no tests were run."
fi

# Show test results
echo
if [ $TEST_RESULT -eq 0 ]; then
    echo "✓ All tests passed successfully"
else
    echo "✗ Some tests failed (exit code: $TEST_RESULT)"
fi

echo
echo "================================================================================"
echo "                           NEXT STEPS"
echo "================================================================================"
echo
echo "1. Review the performance summary above"
echo "2. Check detailed reports in: $REPORT_DIR"
echo "3. Focus optimization on the slowest tests identified"
echo "4. Consider the optimization recommendations provided"
echo
echo "Key reports:"
echo "  - performance-summary.txt     : Overall performance statistics"
echo "  - slowest-tests-report.txt    : Top slowest tests with details"
echo "  - optimization-recommendations.txt : Specific optimization suggestions"
echo "  - detailed-performance-report.csv  : Complete test timing data"
echo

exit $TEST_RESULT
