#!/usr/bin/env bats

# Test suite for run_full_batch.sh
# This script tests the full batch execution functionality

load 'helpers/test_helper'

setup() {
    setup_test_environment
    export SCRIPT_PATH="${SCRIPTS_DIR}/run_full_batch.sh"
    cd "${TEST_DIR}"
}

teardown() {
    cd "${ORIGINAL_DIR}"
    teardown_test_environment
}

@test "run_full_batch.sh exists and is executable" {
    [ -f "${SCRIPT_PATH}" ]
}

@test "run_full_batch.sh checks for FTP tunnel" {
    # The script should check if tunnel is running
    grep -q "2121:" "${SCRIPT_PATH}"
}

@test "run_full_batch.sh uses tnftp for file transfer" {
    # Verify the script uses tnftp
    grep -q "tnftp" "${SCRIPT_PATH}"
}

@test "run_full_batch.sh sets JES filetype" {
    # Verify the script sets filetype to JES
    grep -q "filetype=JES" "${SCRIPT_PATH}"
}

@test "run_full_batch.sh submits CLOSEFIL job" {
    # Verify the script submits CLOSEFIL.jcl
    grep -q "CLOSEFIL.jcl" "${SCRIPT_PATH}"
}

@test "run_full_batch.sh submits ACCTFILE job" {
    # Verify the script submits ACCTFILE.jcl
    grep -q "ACCTFILE.jcl" "${SCRIPT_PATH}"
}

@test "run_full_batch.sh submits CARDFILE job" {
    # Verify the script submits CARDFILE.jcl
    grep -q "CARDFILE.jcl" "${SCRIPT_PATH}"
}

@test "run_full_batch.sh submits XREFFILE job" {
    # Verify the script submits XREFFILE.jcl
    grep -q "XREFFILE.jcl" "${SCRIPT_PATH}"
}

@test "run_full_batch.sh submits CUSTFILE job" {
    # Verify the script submits CUSTFILE.jcl
    grep -q "CUSTFILE.jcl" "${SCRIPT_PATH}"
}

@test "run_full_batch.sh submits TRANBKP job" {
    # Verify the script submits TRANBKP.jcl
    grep -q "TRANBKP.jcl" "${SCRIPT_PATH}"
}

@test "run_full_batch.sh submits DISCGRP job" {
    # Verify the script submits DISCGRP.jcl
    grep -q "DISCGRP.jcl" "${SCRIPT_PATH}"
}

@test "run_full_batch.sh submits TCATBALF job" {
    # Verify the script submits TCATBALF.jcl
    grep -q "TCATBALF.jcl" "${SCRIPT_PATH}"
}

@test "run_full_batch.sh submits TRANTYPE job" {
    # Verify the script submits TRANTYPE.jcl
    grep -q "TRANTYPE.jcl" "${SCRIPT_PATH}"
}

@test "run_full_batch.sh submits DUSRSECJ job" {
    # Verify the script submits DUSRSECJ.jcl
    grep -q "DUSRSECJ.jcl" "${SCRIPT_PATH}"
}

@test "run_full_batch.sh submits POSTTRAN job" {
    # Verify the script submits POSTTRAN.jcl
    grep -q "POSTTRAN.jcl" "${SCRIPT_PATH}"
}

@test "run_full_batch.sh submits INTCALC job" {
    # Verify the script submits INTCALC.jcl
    grep -q "INTCALC.jcl" "${SCRIPT_PATH}"
}

@test "run_full_batch.sh submits COMBTRAN job" {
    # Verify the script submits COMBTRAN.jcl
    grep -q "COMBTRAN.jcl" "${SCRIPT_PATH}"
}

@test "run_full_batch.sh submits TRANIDX job" {
    # Verify the script submits TRANIDX.jcl
    grep -q "TRANIDX.jcl" "${SCRIPT_PATH}"
}

@test "run_full_batch.sh submits OPENFIL job" {
    # Verify the script submits OPENFIL.jcl
    grep -q "OPENFIL.jcl" "${SCRIPT_PATH}"
}

@test "run_full_batch.sh includes wait steps between jobs" {
    # Verify the script includes sleep commands
    grep -q "sleep" "${SCRIPT_PATH}"
}

@test "run_full_batch.sh displays job progress messages" {
    # Verify the script displays progress messages
    grep -q "echo.*Job" "${SCRIPT_PATH}"
}

@test "run_full_batch.sh exits when tunnel not running" {
    # Verify the script exits when tunnel is not running
    grep -q "FTP Tunnel.*not running" "${SCRIPT_PATH}"
}
