#!/bin/bash
# Test helper functions for bats tests

# Setup function to create temporary directories and mock environment
setup_test_environment() {
    export TEST_DIR="$(mktemp -d)"
    # Get the project root directory (parent of tests directory)
    export ORIGINAL_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
    export SCRIPTS_DIR="${ORIGINAL_DIR}/scripts"
    
    # Create mock directories
    mkdir -p "${TEST_DIR}/jcl"
    mkdir -p "${TEST_DIR}/cbl"
    mkdir -p "${TEST_DIR}/cpy"
    mkdir -p "${TEST_DIR}/bms"
    
    # Create mock JCL files
    touch "${TEST_DIR}/jcl/CLOSEFIL.jcl"
    touch "${TEST_DIR}/jcl/ACCTFILE.jcl"
    touch "${TEST_DIR}/jcl/CARDFILE.jcl"
    touch "${TEST_DIR}/jcl/XREFFILE.jcl"
    touch "${TEST_DIR}/jcl/CUSTFILE.jcl"
    touch "${TEST_DIR}/jcl/TRANBKP.jcl"
    touch "${TEST_DIR}/jcl/DISCGRP.jcl"
    touch "${TEST_DIR}/jcl/TCATBALF.jcl"
    touch "${TEST_DIR}/jcl/TRANTYPE.jcl"
    touch "${TEST_DIR}/jcl/DUSRSECJ.jcl"
    touch "${TEST_DIR}/jcl/POSTTRAN.jcl"
    touch "${TEST_DIR}/jcl/INTCALC.jcl"
    touch "${TEST_DIR}/jcl/COMBTRAN.jcl"
    touch "${TEST_DIR}/jcl/TRANIDX.jcl"
    touch "${TEST_DIR}/jcl/OPENFIL.jcl"
    touch "${TEST_DIR}/jcl/TRANFILE.jcl"
    touch "${TEST_DIR}/jcl/TRANCATG.jcl"
}

# Teardown function to clean up temporary directories
teardown_test_environment() {
    if [[ -d "${TEST_DIR}" ]]; then
        rm -rf "${TEST_DIR}"
    fi
}

# Mock tnftp command for testing
mock_tnftp() {
    echo "Mock tnftp called with args: $@"
    return 0
}

# Mock ps command to simulate tunnel running
mock_ps_tunnel_running() {
    echo "  PID TTY      STAT   TIME COMMAND"
    echo "12345 pts/0    S      0:00 ssh -L 2121:mainframe:21 user@gateway"
}

# Mock ps command to simulate tunnel not running
mock_ps_tunnel_not_running() {
    echo "  PID TTY      STAT   TIME COMMAND"
    echo "12345 pts/0    S      0:00 bash"
}

# Create a sample COBOL file for testing
create_sample_cobol_file() {
    local file_path="$1"
    cat > "${file_path}" << 'EOF'
       IDENTIFICATION DIVISION.
       PROGRAM-ID. SAMPLE.
       PROCEDURE DIVISION.
           DISPLAY "HELLO WORLD".
           STOP RUN.
EOF
}

# Create a sample JCL file for testing
create_sample_jcl_file() {
    local file_path="$1"
    cat > "${file_path}" << 'EOF'
//SAMPLE   JOB 'SAMPLE JOB',CLASS=A,MSGCLASS=0
//STEP1    EXEC PGM=IEFBR14
EOF
}

# Create a sample BMS file for testing
create_sample_bms_file() {
    local file_path="$1"
    cat > "${file_path}" << 'EOF'
SAMPLE   DFHMSD TYPE=&SYSPARM,MODE=INOUT,LANG=COBOL,STORAGE=AUTO
EOF
}

# Create a sample copybook file for testing
create_sample_copybook_file() {
    local file_path="$1"
    cat > "${file_path}" << 'EOF'
       01  SAMPLE-RECORD.
           05  SAMPLE-FIELD    PIC X(10).
EOF
}

# Assert file exists
assert_file_exists() {
    local file_path="$1"
    if [[ ! -f "${file_path}" ]]; then
        echo "FAIL: File ${file_path} does not exist"
        return 1
    fi
    return 0
}

# Assert file contains string
assert_file_contains() {
    local file_path="$1"
    local search_string="$2"
    if ! grep -q "${search_string}" "${file_path}"; then
        echo "FAIL: File ${file_path} does not contain '${search_string}'"
        return 1
    fi
    return 0
}

# Assert command succeeds
assert_success() {
    local exit_code="$1"
    if [[ "${exit_code}" -ne 0 ]]; then
        echo "FAIL: Command failed with exit code ${exit_code}"
        return 1
    fi
    return 0
}

# Assert command fails
assert_failure() {
    local exit_code="$1"
    if [[ "${exit_code}" -eq 0 ]]; then
        echo "FAIL: Command succeeded but was expected to fail"
        return 1
    fi
    return 0
}

# Assert output contains string
assert_output_contains() {
    local output="$1"
    local search_string="$2"
    if [[ "${output}" != *"${search_string}"* ]]; then
        echo "FAIL: Output does not contain '${search_string}'"
        echo "Output was: ${output}"
        return 1
    fi
    return 0
}

# Assert output equals string
assert_output_equals() {
    local output="$1"
    local expected="$2"
    if [[ "${output}" != "${expected}" ]]; then
        echo "FAIL: Output does not equal expected"
        echo "Expected: ${expected}"
        echo "Got: ${output}"
        return 1
    fi
    return 0
}
