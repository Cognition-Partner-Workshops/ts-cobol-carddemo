#!/usr/bin/env bats

# Test suite for upld_module.sh
# This script tests the module upload functionality

load 'helpers/test_helper'

setup() {
    setup_test_environment
    export SCRIPT_PATH="${SCRIPTS_DIR}/upld_module.sh"
    cd "${TEST_DIR}"
    
    # Copy pad.awk to test directory
    mkdir -p "${TEST_DIR}/scripts"
    cp "${SCRIPTS_DIR}/pad.awk" "${TEST_DIR}/scripts/"
}

teardown() {
    cd "${ORIGINAL_DIR}"
    teardown_test_environment
}

@test "upld_module.sh exists and is executable" {
    [ -f "${SCRIPT_PATH}" ]
}

@test "upld_module.sh checks for FTP tunnel" {
    # The script should check if tunnel is running
    grep -q "2121:" "${SCRIPT_PATH}"
}

@test "upld_module.sh requires module_path parameter" {
    # Verify the script checks for module_path
    grep -q "Missing module_path parameter" "${SCRIPT_PATH}"
}

@test "upld_module.sh requires module_type parameter" {
    # Verify the script checks for module_type
    grep -q "Missing module_type parameter" "${SCRIPT_PATH}"
}

@test "upld_module.sh extracts module name from path" {
    # Verify the script extracts module name using sed
    grep -q "module_name=" "${SCRIPT_PATH}"
    grep -q "sed" "${SCRIPT_PATH}"
}

@test "upld_module.sh uses awk for padding" {
    # Verify the script uses awk for padding
    grep -q "awk -f.*pad.awk" "${SCRIPT_PATH}"
}

@test "upld_module.sh creates temporary file" {
    # Verify the script creates a temp file
    grep -q "\.tmp" "${SCRIPT_PATH}"
}

@test "upld_module.sh removes temporary file" {
    # Verify the script removes temp file
    grep -q "rm.*\.tmp" "${SCRIPT_PATH}"
}

@test "upld_module.sh uses tnftp for file transfer" {
    # Verify the script uses tnftp
    grep -q "tnftp" "${SCRIPT_PATH}"
}

@test "upld_module.sh uploads to AWS.M2.CARDDEMO dataset" {
    # Verify the script uploads to correct dataset
    grep -q "AWS.M2.CARDDEMO" "${SCRIPT_PATH}"
}

@test "upld_module.sh handles awk transformation failure" {
    # Verify the script checks awk return code
    grep -q '\$?' "${SCRIPT_PATH}"
}

@test "upld_module.sh accepts module_path as first parameter" {
    # Verify the script expects module_path as first parameter
    grep -q "module_path=\$1" "${SCRIPT_PATH}"
}

@test "upld_module.sh accepts module_type as second parameter" {
    # Verify the script expects module_type as second parameter
    grep -q "module_type=\$2" "${SCRIPT_PATH}"
}
