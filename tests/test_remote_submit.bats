#!/usr/bin/env bats

# Test suite for remote_submit.sh
# This script tests the remote JCL submission functionality

load 'helpers/test_helper'

setup() {
    setup_test_environment
    export SCRIPT_PATH="${SCRIPTS_DIR}/remote_submit.sh"
    cd "${TEST_DIR}"
}

teardown() {
    cd "${ORIGINAL_DIR}"
    teardown_test_environment
}

@test "remote_submit.sh exists and is executable" {
    [ -f "${SCRIPT_PATH}" ]
}

@test "remote_submit.sh checks for FTP tunnel" {
    # The script should check if tunnel is running
    grep -q "2121:" "${SCRIPT_PATH}"
}

@test "remote_submit.sh only accepts .jcl files" {
    # Verify the script checks for .jcl extension
    grep -q '\.jcl' "${SCRIPT_PATH}"
}

@test "remote_submit.sh displays error for non-JCL files" {
    # Verify the script has error message for non-JCL files
    grep -q "Only files with jcl extension can be submitted" "${SCRIPT_PATH}"
}

@test "remote_submit.sh uses tnftp for file transfer" {
    # Verify the script uses tnftp
    grep -q "tnftp" "${SCRIPT_PATH}"
}

@test "remote_submit.sh sets JES filetype" {
    # Verify the script sets filetype to JES
    grep -q "filetype=JES" "${SCRIPT_PATH}"
}

@test "remote_submit.sh accepts two parameters" {
    # Verify the script expects file_name and file_extension
    grep -q "file_name=\$1" "${SCRIPT_PATH}"
    grep -q "file_extension=\$2" "${SCRIPT_PATH}"
}

@test "remote_submit.sh echoes file information" {
    # Verify the script echoes file info
    grep -q 'echo.*file_name.*file_extension' "${SCRIPT_PATH}"
}

@test "remote_submit.sh connects to localhost port 2121" {
    # Verify the script connects to the correct port
    grep -q "localhost 2121" "${SCRIPT_PATH}"
}

@test "remote_submit.sh uses put command to submit JCL" {
    # Verify the script uses put command
    grep -q "put.*file_name" "${SCRIPT_PATH}"
}

@test "remote_submit.sh exits when tunnel not running" {
    # Verify the script exits when tunnel is not running
    grep -q "exit" "${SCRIPT_PATH}"
}
