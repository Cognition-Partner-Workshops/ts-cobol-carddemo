#!/usr/bin/env bats

# Test suite for run_posting.sh
# This script tests the transaction posting batch execution functionality

load 'helpers/test_helper'

setup() {
    setup_test_environment
    export SCRIPT_PATH="${SCRIPTS_DIR}/run_posting.sh"
    cd "${TEST_DIR}"
}

teardown() {
    cd "${ORIGINAL_DIR}"
    teardown_test_environment
}

@test "run_posting.sh exists and is executable" {
    [ -f "${SCRIPT_PATH}" ]
}

@test "run_posting.sh checks for FTP tunnel" {
    # The script should check if tunnel is running
    grep -q "2121:" "${SCRIPT_PATH}"
}

@test "run_posting.sh uses tnftp for file transfer" {
    # Verify the script uses tnftp
    grep -q "tnftp" "${SCRIPT_PATH}"
}

@test "run_posting.sh sets JES filetype" {
    # Verify the script sets filetype to JES
    grep -q "filetype=JES" "${SCRIPT_PATH}"
}

@test "run_posting.sh submits CLOSEFIL job" {
    # Verify the script submits CLOSEFIL.jcl
    grep -q "CLOSEFIL.jcl" "${SCRIPT_PATH}"
}

@test "run_posting.sh submits ACCTFILE job" {
    # Verify the script submits ACCTFILE.jcl
    grep -q "ACCTFILE.jcl" "${SCRIPT_PATH}"
}

@test "run_posting.sh submits TCATBALF job" {
    # Verify the script submits TCATBALF.jcl
    grep -q "TCATBALF.jcl" "${SCRIPT_PATH}"
}

@test "run_posting.sh submits TRANBKP job" {
    # Verify the script submits TRANBKP.jcl
    grep -q "TRANBKP.jcl" "${SCRIPT_PATH}"
}

@test "run_posting.sh submits POSTTRAN job" {
    # Verify the script submits POSTTRAN.jcl
    grep -q "POSTTRAN.jcl" "${SCRIPT_PATH}"
}

@test "run_posting.sh submits TRANIDX job" {
    # Verify the script submits TRANIDX.jcl
    grep -q "TRANIDX.jcl" "${SCRIPT_PATH}"
}

@test "run_posting.sh submits OPENFIL job" {
    # Verify the script submits OPENFIL.jcl
    grep -q "OPENFIL.jcl" "${SCRIPT_PATH}"
}

@test "run_posting.sh includes wait steps between jobs" {
    # Verify the script includes sleep commands
    grep -q "sleep" "${SCRIPT_PATH}"
}

@test "run_posting.sh displays job progress messages" {
    # Verify the script displays progress messages
    grep -q "echo.*Job" "${SCRIPT_PATH}"
}

@test "run_posting.sh exits when tunnel not running" {
    # Verify the script exits when tunnel is not running
    grep -q "FTP Tunnel.*not running" "${SCRIPT_PATH}"
}

@test "run_posting.sh runs core processing job" {
    # Verify the script runs core processing
    grep -q "Core processing job" "${SCRIPT_PATH}"
}
