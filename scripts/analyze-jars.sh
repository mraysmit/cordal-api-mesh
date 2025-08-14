#!/bin/bash
# JAR Analysis Tool for CORDAL
# This script analyzes built JARs to show dependencies, sizes, and contents

set -e

# Set script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "================================================================================"
echo "                        JAR Analysis Tool"
echo "================================================================================"
echo

# Function to display help
show_help() {
    echo "Usage: $0 [OPTIONS]"
    echo
    echo "Options:"
    echo "  -h, --help              Show this help message"
    echo "  -s, --sizes             Show JAR sizes only"
    echo "  -d, --dependencies      Show dependency analysis"
    echo "  -c, --contents          Show JAR contents"
    echo "  -a, --all               Show all analysis (default)"
    echo "  -m, --module MODULE     Analyze specific module (cordal-api-service, cordal-metrics-service)"
    echo "  -p, --profile PROFILE   Analyze specific profile JARs (executable, thin, optimized, dev)"
    echo
    echo "Examples:"
    echo "  $0                      # Analyze all JARs"
    echo "  $0 --sizes              # Show sizes only"
    echo "  $0 --module cordal-api-service --profile executable"
    echo "  $0 --dependencies       # Show dependency breakdown"
}

# Default options
SHOW_SIZES=false
SHOW_DEPENDENCIES=false
SHOW_CONTENTS=false
SHOW_ALL=true
TARGET_MODULE=""
TARGET_PROFILE=""

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -s|--sizes)
            SHOW_SIZES=true
            SHOW_ALL=false
            shift
            ;;
        -d|--dependencies)
            SHOW_DEPENDENCIES=true
            SHOW_ALL=false
            shift
            ;;
        -c|--contents)
            SHOW_CONTENTS=true
            SHOW_ALL=false
            shift
            ;;
        -a|--all)
            SHOW_ALL=true
            shift
            ;;
        -m|--module)
            TARGET_MODULE="$2"
            shift 2
            ;;
        -p|--profile)
            TARGET_PROFILE="$2"
            shift 2
            ;;
        *)
            echo "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# Set what to show if --all is selected
if [ "$SHOW_ALL" = true ]; then
    SHOW_SIZES=true
    SHOW_DEPENDENCIES=true
    SHOW_CONTENTS=true
fi

# Function to format file size
format_size() {
    local size=$1
    if [ $size -gt 1048576 ]; then
        echo "$(( size / 1048576 )) MB"
    elif [ $size -gt 1024 ]; then
        echo "$(( size / 1024 )) KB"
    else
        echo "$size bytes"
    fi
}

# Function to find JARs
find_jars() {
    local module=$1
    local profile=$2
    
    if [ -n "$module" ] && [ -n "$profile" ]; then
        find "$PROJECT_ROOT/$module/target" -name "*-$profile.jar" 2>/dev/null || true
    elif [ -n "$module" ]; then
        find "$PROJECT_ROOT/$module/target" -name "*.jar" 2>/dev/null || true
    elif [ -n "$profile" ]; then
        find "$PROJECT_ROOT" -path "*/target/*-$profile.jar" 2>/dev/null || true
    else
        find "$PROJECT_ROOT" -path "*/target/*.jar" -not -path "*/cordal-integration-tests/*" 2>/dev/null || true
    fi
}

# Function to analyze JAR size
analyze_jar_size() {
    local jar_file=$1
    local jar_name=$(basename "$jar_file")
    local jar_size=$(stat -f%z "$jar_file" 2>/dev/null || stat -c%s "$jar_file" 2>/dev/null || echo "0")
    local formatted_size=$(format_size $jar_size)
    
    echo "  $jar_name: $formatted_size"
}

# Function to analyze JAR dependencies
analyze_jar_dependencies() {
    local jar_file=$1
    local jar_name=$(basename "$jar_file")
    
    echo "  Dependencies in $jar_name:"
    
    # Extract and count unique packages
    local temp_dir=$(mktemp -d)
    cd "$temp_dir"
    jar -tf "$jar_file" | grep "\.class$" | sed 's|/[^/]*\.class$||' | sort | uniq -c | sort -nr | head -10 | while read count package; do
        echo "    $package: $count classes"
    done
    cd - > /dev/null
    rm -rf "$temp_dir"
}

# Function to analyze JAR contents
analyze_jar_contents() {
    local jar_file=$1
    local jar_name=$(basename "$jar_file")
    
    echo "  Contents of $jar_name:"
    echo "    Total entries: $(jar -tf "$jar_file" | wc -l)"
    echo "    Class files: $(jar -tf "$jar_file" | grep "\.class$" | wc -l)"
    echo "    Resource files: $(jar -tf "$jar_file" | grep -v "\.class$" | wc -l)"
    
    echo "    Top-level packages:"
    jar -tf "$jar_file" | grep "\.class$" | cut -d'/' -f1 | sort | uniq -c | sort -nr | head -5 | while read count package; do
        echo "      $package: $count classes"
    done
}

# Main analysis
echo "Analyzing JARs in: $PROJECT_ROOT"
echo

# Find all relevant JARs
JARS=$(find_jars "$TARGET_MODULE" "$TARGET_PROFILE")

if [ -z "$JARS" ]; then
    echo "No JARs found matching criteria."
    echo "Make sure to build the project first:"
    echo "  mvn clean package"
    exit 1
fi

# Show sizes
if [ "$SHOW_SIZES" = true ]; then
    echo "JAR Sizes:"
    echo "$JARS" | while read jar_file; do
        if [ -f "$jar_file" ]; then
            analyze_jar_size "$jar_file"
        fi
    done
    echo
fi

# Show dependencies
if [ "$SHOW_DEPENDENCIES" = true ]; then
    echo "Dependency Analysis:"
    echo "$JARS" | while read jar_file; do
        if [ -f "$jar_file" ]; then
            analyze_jar_dependencies "$jar_file"
            echo
        fi
    done
fi

# Show contents
if [ "$SHOW_CONTENTS" = true ]; then
    echo "Content Analysis:"
    echo "$JARS" | while read jar_file; do
        if [ -f "$jar_file" ]; then
            analyze_jar_contents "$jar_file"
            echo
        fi
    done
fi

echo "================================================================================"
echo "Analysis complete!"
echo "================================================================================"
