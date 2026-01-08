#!/bin/bash
#
# Test Runner Script for CardDemo Application
# This script runs all bats tests in the tests directory
#
# Usage: ./run_tests.sh [options]
#   Options:
#     -v, --verbose    Run tests in verbose mode
#     -t, --tap        Output in TAP format
#     -h, --help       Show this help message
#     <test_file>      Run specific test file only
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "${SCRIPT_DIR}")"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Default options
VERBOSE=""
TAP_FORMAT=""
SPECIFIC_TEST=""

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -v|--verbose)
            VERBOSE="--verbose-run"
            shift
            ;;
        -t|--tap)
            TAP_FORMAT="--tap"
            shift
            ;;
        -h|--help)
            echo "Usage: $0 [options] [test_file]"
            echo ""
            echo "Options:"
            echo "  -v, --verbose    Run tests in verbose mode"
            echo "  -t, --tap        Output in TAP format"
            echo "  -h, --help       Show this help message"
            echo "  <test_file>      Run specific test file only"
            exit 0
            ;;
        *)
            SPECIFIC_TEST="$1"
            shift
            ;;
    esac
done

# Check if bats is installed
check_bats() {
    if command -v bats &> /dev/null; then
        echo -e "${GREEN}bats is installed${NC}"
        return 0
    else
        echo -e "${YELLOW}bats is not installed. Installing...${NC}"
        
        # Try to install bats
        if command -v apt-get &> /dev/null; then
            sudo apt-get update && sudo apt-get install -y bats
        elif command -v brew &> /dev/null; then
            brew install bats-core
        elif command -v npm &> /dev/null; then
            npm install -g bats
        else
            echo -e "${RED}Could not install bats. Please install it manually.${NC}"
            echo "Visit: https://github.com/bats-core/bats-core"
            return 1
        fi
    fi
}

# Run tests
run_tests() {
    local test_files=()
    local exit_code=0
    
    cd "${SCRIPT_DIR}"
    
    if [[ -n "${SPECIFIC_TEST}" ]]; then
        if [[ -f "${SPECIFIC_TEST}" ]]; then
            test_files=("${SPECIFIC_TEST}")
        elif [[ -f "${SCRIPT_DIR}/${SPECIFIC_TEST}" ]]; then
            test_files=("${SCRIPT_DIR}/${SPECIFIC_TEST}")
        else
            echo -e "${RED}Test file not found: ${SPECIFIC_TEST}${NC}"
            return 1
        fi
    else
        # Find all .bats files
        while IFS= read -r -d '' file; do
            test_files+=("$file")
        done < <(find "${SCRIPT_DIR}" -name "*.bats" -type f -print0 | sort -z)
    fi
    
    if [[ ${#test_files[@]} -eq 0 ]]; then
        echo -e "${YELLOW}No test files found${NC}"
        return 0
    fi
    
    echo ""
    echo "========================================"
    echo "Running CardDemo Test Suite"
    echo "========================================"
    echo ""
    echo "Found ${#test_files[@]} test file(s)"
    echo ""
    
    local passed=0
    local failed=0
    
    for test_file in "${test_files[@]}"; do
        local test_name=$(basename "${test_file}")
        echo -e "${YELLOW}Running: ${test_name}${NC}"
        
        if bats ${VERBOSE} ${TAP_FORMAT} "${test_file}"; then
            ((passed++))
            echo -e "${GREEN}PASSED: ${test_name}${NC}"
        else
            ((failed++))
            echo -e "${RED}FAILED: ${test_name}${NC}"
            exit_code=1
        fi
        echo ""
    done
    
    echo "========================================"
    echo "Test Summary"
    echo "========================================"
    echo -e "Passed: ${GREEN}${passed}${NC}"
    echo -e "Failed: ${RED}${failed}${NC}"
    echo "Total:  $((passed + failed))"
    echo ""
    
    return ${exit_code}
}

# Main execution
main() {
    echo ""
    echo "CardDemo Test Suite"
    echo "==================="
    echo ""
    
    # Check for bats
    if ! check_bats; then
        exit 1
    fi
    
    # Run tests
    if run_tests; then
        echo -e "${GREEN}All tests passed!${NC}"
        exit 0
    else
        echo -e "${RED}Some tests failed!${NC}"
        exit 1
    fi
}

main
