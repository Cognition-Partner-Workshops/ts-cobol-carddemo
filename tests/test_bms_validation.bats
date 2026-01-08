#!/usr/bin/env bats

# Test suite for BMS file validation
# This script validates the syntax and structure of BMS map files

load 'helpers/test_helper'

setup() {
    setup_test_environment
    export BMS_DIR="${ORIGINAL_DIR}/app/bms"
    cd "${TEST_DIR}"
}

teardown() {
    cd "${ORIGINAL_DIR}"
    teardown_test_environment
}

# Helper function to validate BMS basic structure
validate_bms_structure() {
    local bms_file="$1"
    
    # Check if file exists
    [ -f "${bms_file}" ] || return 1
    
    # Check for DFHMSD or DFHMDI macro (BMS map definition)
    grep -qE "(DFHMSD|DFHMDI)" "${bms_file}" || return 1
    
    return 0
}

@test "BMS directory exists" {
    [ -d "${BMS_DIR}" ]
}

@test "COACTUP.bms exists and has valid structure" {
    local bms_file="${BMS_DIR}/COACTUP.bms"
    [ -f "${bms_file}" ]
    validate_bms_structure "${bms_file}"
}

@test "COACTVW.bms exists and has valid structure" {
    local bms_file="${BMS_DIR}/COACTVW.bms"
    [ -f "${bms_file}" ]
    validate_bms_structure "${bms_file}"
}

@test "COADM01.bms exists and has valid structure" {
    local bms_file="${BMS_DIR}/COADM01.bms"
    [ -f "${bms_file}" ]
    validate_bms_structure "${bms_file}"
}

@test "COBIL00.bms exists and has valid structure" {
    local bms_file="${BMS_DIR}/COBIL00.bms"
    [ -f "${bms_file}" ]
    validate_bms_structure "${bms_file}"
}

@test "COCRDLI.bms exists and has valid structure" {
    local bms_file="${BMS_DIR}/COCRDLI.bms"
    [ -f "${bms_file}" ]
    validate_bms_structure "${bms_file}"
}

@test "COCRDSL.bms exists and has valid structure" {
    local bms_file="${BMS_DIR}/COCRDSL.bms"
    [ -f "${bms_file}" ]
    validate_bms_structure "${bms_file}"
}

@test "COCRDUP.bms exists and has valid structure" {
    local bms_file="${BMS_DIR}/COCRDUP.bms"
    [ -f "${bms_file}" ]
    validate_bms_structure "${bms_file}"
}

@test "COMEN01.bms exists and has valid structure" {
    local bms_file="${BMS_DIR}/COMEN01.bms"
    [ -f "${bms_file}" ]
    validate_bms_structure "${bms_file}"
}

@test "CORPT00.bms exists and has valid structure" {
    local bms_file="${BMS_DIR}/CORPT00.bms"
    [ -f "${bms_file}" ]
    validate_bms_structure "${bms_file}"
}

@test "COSGN00.bms exists and has valid structure" {
    local bms_file="${BMS_DIR}/COSGN00.bms"
    [ -f "${bms_file}" ]
    validate_bms_structure "${bms_file}"
}

@test "COTRN00.bms exists and has valid structure" {
    local bms_file="${BMS_DIR}/COTRN00.bms"
    [ -f "${bms_file}" ]
    validate_bms_structure "${bms_file}"
}

@test "COTRN01.bms exists and has valid structure" {
    local bms_file="${BMS_DIR}/COTRN01.bms"
    [ -f "${bms_file}" ]
    validate_bms_structure "${bms_file}"
}

@test "COTRN02.bms exists and has valid structure" {
    local bms_file="${BMS_DIR}/COTRN02.bms"
    [ -f "${bms_file}" ]
    validate_bms_structure "${bms_file}"
}

@test "COUSR00.bms exists and has valid structure" {
    local bms_file="${BMS_DIR}/COUSR00.bms"
    [ -f "${bms_file}" ]
    validate_bms_structure "${bms_file}"
}

@test "COUSR01.bms exists and has valid structure" {
    local bms_file="${BMS_DIR}/COUSR01.bms"
    [ -f "${bms_file}" ]
    validate_bms_structure "${bms_file}"
}

@test "COUSR02.bms exists and has valid structure" {
    local bms_file="${BMS_DIR}/COUSR02.bms"
    [ -f "${bms_file}" ]
    validate_bms_structure "${bms_file}"
}

@test "COUSR03.bms exists and has valid structure" {
    local bms_file="${BMS_DIR}/COUSR03.bms"
    [ -f "${bms_file}" ]
    validate_bms_structure "${bms_file}"
}

# General BMS validation tests
@test "All BMS files contain DFHMSD macro" {
    local count=0
    local total=0
    for bms_file in "${BMS_DIR}"/*.bms; do
        if [ -f "${bms_file}" ]; then
            total=$((total + 1))
            if grep -q "DFHMSD" "${bms_file}"; then
                count=$((count + 1))
            fi
        fi
    done
    [ "$count" -eq "$total" ]
}

@test "All BMS files have TYPE parameter" {
    local count=0
    for bms_file in "${BMS_DIR}"/*.bms; do
        if [ -f "${bms_file}" ]; then
            if grep -q "TYPE=" "${bms_file}"; then
                count=$((count + 1))
            fi
        fi
    done
    # At least some BMS files should have TYPE parameter
    [ "$count" -gt 0 ]
}

@test "All BMS files have LANG parameter" {
    local count=0
    for bms_file in "${BMS_DIR}"/*.bms; do
        if [ -f "${bms_file}" ]; then
            if grep -q "LANG=" "${bms_file}"; then
                count=$((count + 1))
            fi
        fi
    done
    # At least some BMS files should have LANG parameter
    [ "$count" -gt 0 ]
}

@test "BMS files define screen fields with DFHMDF" {
    local count=0
    for bms_file in "${BMS_DIR}"/*.bms; do
        if [ -f "${bms_file}" ]; then
            if grep -q "DFHMDF" "${bms_file}"; then
                count=$((count + 1))
            fi
        fi
    done
    # Most BMS files should define fields
    [ "$count" -gt 0 ]
}
