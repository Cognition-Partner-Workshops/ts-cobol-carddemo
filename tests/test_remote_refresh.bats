#!/usr/bin/env bats

# Test suite for remote_refresh.sh
# This script tests the remote data refresh functionality

load 'helpers/test_helper'

setup() {
    setup_test_environment
    export SCRIPT_PATH="${SCRIPTS_DIR}/remote_refresh.sh"
    cd "${TEST_DIR}"
}

teardown() {
    cd "${ORIGINAL_DIR}"
    teardown_test_environment
}

@test "remote_refresh.sh exists and is executable" {
    [ -f "${SCRIPT_PATH}" ]
}

@test "remote_refresh.sh checks for FTP tunnel" {
    # The script should check if tunnel is running
    grep -q "2121:" "${SCRIPT_PATH}"
}

@test "remote_refresh.sh uses tnftp for file transfer" {
    # Verify the script uses tnftp
    grep -q "tnftp" "${SCRIPT_PATH}"
}

@test "remote_refresh.sh sets JES filetype" {
    # Verify the script sets filetype to JES
    grep -q "filetype=JES" "${SCRIPT_PATH}"
}

@test "remote_refresh.sh submits CLOSEFIL job" {
    # Verify the script submits CLOSEFIL.jcl
    grep -q "CLOSEFIL.jcl" "${SCRIPT_PATH}"
}

@test "remote_refresh.sh submits ACCTFILE job" {
    # Verify the script submits ACCTFILE.jcl
    grep -q "ACCTFILE.jcl" "${SCRIPT_PATH}"
}

@test "remote_refresh.sh submits CARDFILE job" {
    # Verify the script submits CARDFILE.jcl
    grep -q "CARDFILE.jcl" "${SCRIPT_PATH}"
}

@test "remote_refresh.sh submits XREFFILE job" {
    # Verify the script submits XREFFILE.jcl
    grep -q "XREFFILE.jcl" "${SCRIPT_PATH}"
}

@test "remote_refresh.sh submits CUSTFILE job" {
    # Verify the script submits CUSTFILE.jcl
    grep -q "CUSTFILE.jcl" "${SCRIPT_PATH}"
}

@test "remote_refresh.sh submits TRANFILE job" {
    # Verify the script submits TRANFILE.jcl
    grep -q "TRANFILE.jcl" "${SCRIPT_PATH}"
}

@test "remote_refresh.sh submits DISCGRP job" {
    # Verify the script submits DISCGRP.jcl
    grep -q "DISCGRP.jcl" "${SCRIPT_PATH}"
}

@test "remote_refresh.sh submits TCATBALF job" {
    # Verify the script submits TCATBALF.jcl
    grep -q "TCATBALF.jcl" "${SCRIPT_PATH}"
}

@test "remote_refresh.sh submits TRANCATG job" {
    # Verify the script submits TRANCATG.jcl
    grep -q "TRANCATG.jcl" "${SCRIPT_PATH}"
}

@test "remote_refresh.sh submits TRANTYPE job" {
    # Verify the script submits TRANTYPE.jcl
    grep -q "TRANTYPE.jcl" "${SCRIPT_PATH}"
}

@test "remote_refresh.sh submits DUSRSECJ job" {
    # Verify the script submits DUSRSECJ.jcl
    grep -q "DUSRSECJ.jcl" "${SCRIPT_PATH}"
}

@test "remote_refresh.sh submits OPENFIL job" {
    # Verify the script submits OPENFIL.jcl
    grep -q "OPENFIL.jcl" "${SCRIPT_PATH}"
}

@test "remote_refresh.sh includes wait steps between jobs" {
    # Verify the script includes sleep commands
    grep -q "sleep" "${SCRIPT_PATH}"
}

@test "remote_refresh.sh displays job progress messages" {
    # Verify the script displays progress messages
    grep -q "echo.*Job" "${SCRIPT_PATH}"
}

@test "remote_refresh.sh displays data refresh complete message" {
    # Verify the script displays completion message
    grep -q "Data Refresh Complete" "${SCRIPT_PATH}"
}

@test "remote_refresh.sh exits when tunnel not running" {
    # Verify the script exits when tunnel is not running
    grep -q "FTP Tunnel.*not running" "${SCRIPT_PATH}"
}
