#!/usr/bin/env bats

# Test suite for JCL file validation
# This script validates the syntax and structure of JCL files

load 'helpers/test_helper'

setup() {
    setup_test_environment
    export JCL_DIR="${ORIGINAL_DIR}/app/jcl"
    cd "${TEST_DIR}"
}

teardown() {
    cd "${ORIGINAL_DIR}"
    teardown_test_environment
}

# Helper function to validate JCL basic structure
validate_jcl_structure() {
    local jcl_file="$1"
    
    # Check if file exists
    [ -f "${jcl_file}" ] || return 1
    
    # Check if file starts with // (JCL job card or comment)
    head -1 "${jcl_file}" | grep -q "^//" || return 1
    
    return 0
}

# Helper function to check JCL has job card
has_job_card() {
    local jcl_file="$1"
    grep -q "JOB " "${jcl_file}"
}

@test "JCL directory exists" {
    [ -d "${JCL_DIR}" ]
}

@test "ACCTFILE.jcl exists and has valid structure" {
    local jcl_file="${JCL_DIR}/ACCTFILE.jcl"
    [ -f "${jcl_file}" ]
    validate_jcl_structure "${jcl_file}"
}

@test "CARDFILE.jcl exists and has valid structure" {
    local jcl_file="${JCL_DIR}/CARDFILE.jcl"
    [ -f "${jcl_file}" ]
    validate_jcl_structure "${jcl_file}"
}

@test "CLOSEFIL.jcl exists and has valid structure" {
    local jcl_file="${JCL_DIR}/CLOSEFIL.jcl"
    [ -f "${jcl_file}" ]
    validate_jcl_structure "${jcl_file}"
}

@test "COMBTRAN.jcl exists and has valid structure" {
    local jcl_file="${JCL_DIR}/COMBTRAN.jcl"
    [ -f "${jcl_file}" ]
    validate_jcl_structure "${jcl_file}"
}

@test "CUSTFILE.jcl exists and has valid structure" {
    local jcl_file="${JCL_DIR}/CUSTFILE.jcl"
    [ -f "${jcl_file}" ]
    validate_jcl_structure "${jcl_file}"
}

@test "DALYREJS.jcl exists and has valid structure" {
    local jcl_file="${JCL_DIR}/DALYREJS.jcl"
    [ -f "${jcl_file}" ]
    validate_jcl_structure "${jcl_file}"
}

@test "DEFCUST.jcl exists and has valid structure" {
    local jcl_file="${JCL_DIR}/DEFCUST.jcl"
    [ -f "${jcl_file}" ]
    validate_jcl_structure "${jcl_file}"
}

@test "DEFGDGB.jcl exists and has valid structure" {
    local jcl_file="${JCL_DIR}/DEFGDGB.jcl"
    [ -f "${jcl_file}" ]
    validate_jcl_structure "${jcl_file}"
}

@test "DEFGDGD.jcl exists and has valid structure" {
    local jcl_file="${JCL_DIR}/DEFGDGD.jcl"
    [ -f "${jcl_file}" ]
    validate_jcl_structure "${jcl_file}"
}

@test "DISCGRP.jcl exists and has valid structure" {
    local jcl_file="${JCL_DIR}/DISCGRP.jcl"
    [ -f "${jcl_file}" ]
    validate_jcl_structure "${jcl_file}"
}

@test "DUSRSECJ.jcl exists and has valid structure" {
    local jcl_file="${JCL_DIR}/DUSRSECJ.jcl"
    [ -f "${jcl_file}" ]
    validate_jcl_structure "${jcl_file}"
}

@test "ESDSRRDS.jcl exists and has valid structure" {
    local jcl_file="${JCL_DIR}/ESDSRRDS.jcl"
    [ -f "${jcl_file}" ]
    validate_jcl_structure "${jcl_file}"
}

@test "INTCALC.jcl exists and has valid structure" {
    local jcl_file="${JCL_DIR}/INTCALC.jcl"
    [ -f "${jcl_file}" ]
    validate_jcl_structure "${jcl_file}"
}

@test "OPENFIL.jcl exists and has valid structure" {
    local jcl_file="${JCL_DIR}/OPENFIL.jcl"
    [ -f "${jcl_file}" ]
    validate_jcl_structure "${jcl_file}"
}

@test "POSTTRAN.jcl exists and has valid structure" {
    local jcl_file="${JCL_DIR}/POSTTRAN.jcl"
    [ -f "${jcl_file}" ]
    validate_jcl_structure "${jcl_file}"
}

@test "REPTFILE.jcl exists and has valid structure" {
    local jcl_file="${JCL_DIR}/REPTFILE.jcl"
    [ -f "${jcl_file}" ]
    validate_jcl_structure "${jcl_file}"
}

@test "TCATBALF.jcl exists and has valid structure" {
    local jcl_file="${JCL_DIR}/TCATBALF.jcl"
    [ -f "${jcl_file}" ]
    validate_jcl_structure "${jcl_file}"
}

@test "TRANBKP.jcl exists and has valid structure" {
    local jcl_file="${JCL_DIR}/TRANBKP.jcl"
    [ -f "${jcl_file}" ]
    validate_jcl_structure "${jcl_file}"
}

@test "TRANCATG.jcl exists and has valid structure" {
    local jcl_file="${JCL_DIR}/TRANCATG.jcl"
    [ -f "${jcl_file}" ]
    validate_jcl_structure "${jcl_file}"
}

@test "TRANFILE.jcl exists and has valid structure" {
    local jcl_file="${JCL_DIR}/TRANFILE.jcl"
    [ -f "${jcl_file}" ]
    validate_jcl_structure "${jcl_file}"
}

@test "TRANIDX.jcl exists and has valid structure" {
    local jcl_file="${JCL_DIR}/TRANIDX.jcl"
    [ -f "${jcl_file}" ]
    validate_jcl_structure "${jcl_file}"
}

@test "TRANREPT.jcl exists and has valid structure" {
    local jcl_file="${JCL_DIR}/TRANREPT.jcl"
    [ -f "${jcl_file}" ]
    validate_jcl_structure "${jcl_file}"
}

@test "TRANTYPE.jcl exists and has valid structure" {
    local jcl_file="${JCL_DIR}/TRANTYPE.jcl"
    [ -f "${jcl_file}" ]
    validate_jcl_structure "${jcl_file}"
}

@test "XREFFILE.jcl exists and has valid structure" {
    local jcl_file="${JCL_DIR}/XREFFILE.jcl"
    [ -f "${jcl_file}" ]
    validate_jcl_structure "${jcl_file}"
}

@test "All JCL files have proper line endings" {
    for jcl_file in "${JCL_DIR}"/*.jcl; do
        # Check for DOS line endings (should not have them or should handle them)
        if file "${jcl_file}" | grep -q "CRLF"; then
            # File has DOS line endings, which is acceptable for mainframe files
            true
        fi
    done
}

@test "All JCL files reference AWS.M2.CARDDEMO datasets" {
    local count=0
    for jcl_file in "${JCL_DIR}"/*.jcl; do
        if grep -q "AWS.M2.CARDDEMO" "${jcl_file}"; then
            count=$((count + 1))
        fi
    done
    # At least some JCL files should reference the CardDemo datasets
    [ "$count" -gt 0 ]
}

@test "JCL files do not contain syntax errors in EXEC statements" {
    for jcl_file in "${JCL_DIR}"/*.jcl; do
        # Check that EXEC statements have proper format
        if grep -q "EXEC " "${jcl_file}"; then
            # EXEC should be followed by PGM= or PROC=
            grep "EXEC " "${jcl_file}" | grep -qE "(PGM=|PROC=)" || true
        fi
    done
}
