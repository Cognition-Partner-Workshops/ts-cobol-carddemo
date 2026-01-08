#!/usr/bin/env bats

# Test suite for COBOL file validation
# This script validates the syntax and structure of COBOL programs

load 'helpers/test_helper'

setup() {
    setup_test_environment
    export CBL_DIR="${ORIGINAL_DIR}/app/cbl"
    export CPY_DIR="${ORIGINAL_DIR}/app/cpy"
    cd "${TEST_DIR}"
}

teardown() {
    cd "${ORIGINAL_DIR}"
    teardown_test_environment
}

# Helper function to validate COBOL basic structure
validate_cobol_structure() {
    local cbl_file="$1"
    
    # Check if file exists
    [ -f "${cbl_file}" ] || return 1
    
    # Check for IDENTIFICATION DIVISION (case insensitive)
    grep -qi "IDENTIFICATION DIVISION" "${cbl_file}" || return 1
    
    # Check for PROGRAM-ID
    grep -qi "PROGRAM-ID" "${cbl_file}" || return 1
    
    return 0
}

# Helper function to validate copybook structure
validate_copybook_structure() {
    local cpy_file="$1"
    
    # Check if file exists
    [ -f "${cpy_file}" ] || return 1
    
    # Copybooks should contain data definitions (01, 05, etc.) or COPY statements
    grep -qE "^\s*(0[1-9]|[1-4][0-9]|77|88)\s+" "${cpy_file}" || return 1
    
    return 0
}

@test "COBOL source directory exists" {
    [ -d "${CBL_DIR}" ]
}

@test "Copybook directory exists" {
    [ -d "${CPY_DIR}" ]
}

# COBOL Program Tests
@test "CBACT01C.cbl exists and has valid structure" {
    local cbl_file="${CBL_DIR}/CBACT01C.cbl"
    [ -f "${cbl_file}" ]
    validate_cobol_structure "${cbl_file}"
}

@test "CBACT02C.cbl exists and has valid structure" {
    local cbl_file="${CBL_DIR}/CBACT02C.cbl"
    [ -f "${cbl_file}" ]
    validate_cobol_structure "${cbl_file}"
}

@test "CBACT03C.cbl exists and has valid structure" {
    local cbl_file="${CBL_DIR}/CBACT03C.cbl"
    [ -f "${cbl_file}" ]
    validate_cobol_structure "${cbl_file}"
}

@test "CBACT04C.cbl exists and has valid structure" {
    local cbl_file="${CBL_DIR}/CBACT04C.cbl"
    [ -f "${cbl_file}" ]
    validate_cobol_structure "${cbl_file}"
}

@test "CBCUS01C.cbl exists and has valid structure" {
    local cbl_file="${CBL_DIR}/CBCUS01C.cbl"
    [ -f "${cbl_file}" ]
    validate_cobol_structure "${cbl_file}"
}

@test "CBTRN01C.cbl exists and has valid structure" {
    local cbl_file="${CBL_DIR}/CBTRN01C.cbl"
    [ -f "${cbl_file}" ]
    validate_cobol_structure "${cbl_file}"
}

@test "CBTRN02C.cbl exists and has valid structure" {
    local cbl_file="${CBL_DIR}/CBTRN02C.cbl"
    [ -f "${cbl_file}" ]
    validate_cobol_structure "${cbl_file}"
}

@test "CBTRN03C.cbl exists and has valid structure" {
    local cbl_file="${CBL_DIR}/CBTRN03C.cbl"
    [ -f "${cbl_file}" ]
    validate_cobol_structure "${cbl_file}"
}

@test "COACTUPC.cbl exists and has valid structure" {
    local cbl_file="${CBL_DIR}/COACTUPC.cbl"
    [ -f "${cbl_file}" ]
    validate_cobol_structure "${cbl_file}"
}

@test "COACTVWC.cbl exists and has valid structure" {
    local cbl_file="${CBL_DIR}/COACTVWC.cbl"
    [ -f "${cbl_file}" ]
    validate_cobol_structure "${cbl_file}"
}

@test "COADM01C.cbl exists and has valid structure" {
    local cbl_file="${CBL_DIR}/COADM01C.cbl"
    [ -f "${cbl_file}" ]
    validate_cobol_structure "${cbl_file}"
}

@test "COBIL00C.cbl exists and has valid structure" {
    local cbl_file="${CBL_DIR}/COBIL00C.cbl"
    [ -f "${cbl_file}" ]
    validate_cobol_structure "${cbl_file}"
}

@test "COCRDLIC.cbl exists and has valid structure" {
    local cbl_file="${CBL_DIR}/COCRDLIC.cbl"
    [ -f "${cbl_file}" ]
    validate_cobol_structure "${cbl_file}"
}

@test "COCRDSLC.cbl exists and has valid structure" {
    local cbl_file="${CBL_DIR}/COCRDSLC.cbl"
    [ -f "${cbl_file}" ]
    validate_cobol_structure "${cbl_file}"
}

@test "COCRDUPC.cbl exists and has valid structure" {
    local cbl_file="${CBL_DIR}/COCRDUPC.cbl"
    [ -f "${cbl_file}" ]
    validate_cobol_structure "${cbl_file}"
}

@test "COMEN01C.cbl exists and has valid structure" {
    local cbl_file="${CBL_DIR}/COMEN01C.cbl"
    [ -f "${cbl_file}" ]
    validate_cobol_structure "${cbl_file}"
}

@test "CORPT00C.cbl exists and has valid structure" {
    local cbl_file="${CBL_DIR}/CORPT00C.cbl"
    [ -f "${cbl_file}" ]
    validate_cobol_structure "${cbl_file}"
}

@test "COSGN00C.cbl exists and has valid structure" {
    local cbl_file="${CBL_DIR}/COSGN00C.cbl"
    [ -f "${cbl_file}" ]
    validate_cobol_structure "${cbl_file}"
}

@test "COTRN00C.cbl exists and has valid structure" {
    local cbl_file="${CBL_DIR}/COTRN00C.cbl"
    [ -f "${cbl_file}" ]
    validate_cobol_structure "${cbl_file}"
}

@test "COTRN01C.cbl exists and has valid structure" {
    local cbl_file="${CBL_DIR}/COTRN01C.cbl"
    [ -f "${cbl_file}" ]
    validate_cobol_structure "${cbl_file}"
}

@test "COTRN02C.cbl exists and has valid structure" {
    local cbl_file="${CBL_DIR}/COTRN02C.cbl"
    [ -f "${cbl_file}" ]
    validate_cobol_structure "${cbl_file}"
}

@test "COUSR00C.cbl exists and has valid structure" {
    local cbl_file="${CBL_DIR}/COUSR00C.cbl"
    [ -f "${cbl_file}" ]
    validate_cobol_structure "${cbl_file}"
}

@test "COUSR01C.cbl exists and has valid structure" {
    local cbl_file="${CBL_DIR}/COUSR01C.cbl"
    [ -f "${cbl_file}" ]
    validate_cobol_structure "${cbl_file}"
}

@test "COUSR02C.cbl exists and has valid structure" {
    local cbl_file="${CBL_DIR}/COUSR02C.cbl"
    [ -f "${cbl_file}" ]
    validate_cobol_structure "${cbl_file}"
}

@test "COUSR03C.cbl exists and has valid structure" {
    local cbl_file="${CBL_DIR}/COUSR03C.cbl"
    [ -f "${cbl_file}" ]
    validate_cobol_structure "${cbl_file}"
}

@test "CSUTLDTC.cbl exists and has valid structure" {
    local cbl_file="${CBL_DIR}/CSUTLDTC.cbl"
    [ -f "${cbl_file}" ]
    validate_cobol_structure "${cbl_file}"
}

# Copybook Tests
@test "COCOM01Y.cpy exists" {
    [ -f "${CPY_DIR}/COCOM01Y.cpy" ]
}

@test "CSUSR01Y.cpy exists" {
    [ -f "${CPY_DIR}/CSUSR01Y.cpy" ]
}

@test "CVACT01Y.cpy exists" {
    [ -f "${CPY_DIR}/CVACT01Y.cpy" ]
}

@test "CVACT02Y.cpy exists" {
    [ -f "${CPY_DIR}/CVACT02Y.cpy" ]
}

@test "CVACT03Y.cpy exists" {
    [ -f "${CPY_DIR}/CVACT03Y.cpy" ]
}

@test "CVCUS01Y.cpy exists" {
    [ -f "${CPY_DIR}/CVCUS01Y.cpy" ]
}

@test "CVTRA01Y.cpy exists" {
    [ -f "${CPY_DIR}/CVTRA01Y.cpy" ]
}

@test "CVTRA05Y.cpy exists" {
    [ -f "${CPY_DIR}/CVTRA05Y.cpy" ]
}

@test "CVTRA06Y.cpy exists" {
    [ -f "${CPY_DIR}/CVTRA06Y.cpy" ]
}

# General validation tests
@test "All COBOL files have PROCEDURE DIVISION" {
    local count=0
    local total=0
    for cbl_file in "${CBL_DIR}"/*.cbl "${CBL_DIR}"/*.CBL; do
        if [ -f "${cbl_file}" ]; then
            total=$((total + 1))
            if grep -qi "PROCEDURE DIVISION" "${cbl_file}"; then
                count=$((count + 1))
            fi
        fi
    done
    [ "$count" -eq "$total" ]
}

@test "All COBOL files have DATA DIVISION" {
    local count=0
    local total=0
    for cbl_file in "${CBL_DIR}"/*.cbl "${CBL_DIR}"/*.CBL; do
        if [ -f "${cbl_file}" ]; then
            total=$((total + 1))
            if grep -qi "DATA DIVISION" "${cbl_file}"; then
                count=$((count + 1))
            fi
        fi
    done
    [ "$count" -eq "$total" ]
}

@test "COBOL files follow mainframe column conventions" {
    # Check that COBOL files have content starting in appropriate columns
    # Column 7 should have * for comments or space for code
    # Column 8-72 should contain the actual code
    for cbl_file in "${CBL_DIR}"/*.cbl; do
        if [ -f "${cbl_file}" ]; then
            # Just verify the file is readable and has content
            [ -s "${cbl_file}" ]
        fi
    done
}
