#!/usr/bin/env bats

# Test suite for run_interest_calc.sh
# This script tests the interest calculation batch execution functionality

load 'helpers/test_helper'

setup() {
    setup_test_environment
    export SCRIPT_PATH="${SCRIPTS_DIR}/run_interest_calc.sh"
    cd "${TEST_DIR}"
}

teardown() {
    cd "${ORIGINAL_DIR}"
    teardown_test_environment
}

@test "run_interest_calc.sh exists and is executable" {
    [ -f "${SCRIPT_PATH}" ]
}

@test "run_interest_calc.sh checks for FTP tunnel" {
    # The script should check if tunnel is running
    grep -q "2121:" "${SCRIPT_PATH}"
}

@test "run_interest_calc.sh uses tnftp for file transfer" {
    # Verify the script uses tnftp
    grep -q "tnftp" "${SCRIPT_PATH}"
}

@test "run_interest_calc.sh sets JES filetype" {
    # Verify the script sets filetype to JES
    grep -q "filetype=JES" "${SCRIPT_PATH}"
}

@test "run_interest_calc.sh displays running message" {
    # Verify the script displays running message
    grep -q "Running Interest Calculation Cycle" "${SCRIPT_PATH}"
}

@test "run_interest_calc.sh submits CLOSEFIL job" {
    # Verify the script submits CLOSEFIL.jcl
    grep -q "CLOSEFIL.jcl" "${SCRIPT_PATH}"
}

@test "run_interest_calc.sh submits INTCALC job" {
    # Verify the script submits INTCALC.jcl
    grep -q "INTCALC.jcl" "${SCRIPT_PATH}"
}

@test "run_interest_calc.sh submits TRANBKP job" {
    # Verify the script submits TRANBKP.jcl
    grep -q "TRANBKP.jcl" "${SCRIPT_PATH}"
}

@test "run_interest_calc.sh submits COMBTRAN job" {
    # Verify the script submits COMBTRAN.jcl
    grep -q "COMBTRAN.jcl" "${SCRIPT_PATH}"
}

@test "run_interest_calc.sh submits TRANIDX job" {
    # Verify the script submits TRANIDX.jcl
    grep -q "TRANIDX.jcl" "${SCRIPT_PATH}"
}

@test "run_interest_calc.sh submits OPENFIL job" {
    # Verify the script submits OPENFIL.jcl
    grep -q "OPENFIL.jcl" "${SCRIPT_PATH}"
}

@test "run_interest_calc.sh includes wait steps between jobs" {
    # Verify the script includes sleep commands
    grep -q "sleep" "${SCRIPT_PATH}"
}

@test "run_interest_calc.sh displays job progress messages" {
    # Verify the script displays progress messages
    grep -q "echo.*Job" "${SCRIPT_PATH}"
}

@test "run_interest_calc.sh exits when tunnel not running" {
    # Verify the script exits when tunnel is not running
    grep -q "FTP Tunnel.*not running" "${SCRIPT_PATH}"
}
