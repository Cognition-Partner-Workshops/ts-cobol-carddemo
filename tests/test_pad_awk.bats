#!/usr/bin/env bats

# Test suite for pad.awk
# This script tests the AWK padding functionality

load 'helpers/test_helper'

setup() {
    setup_test_environment
    export AWK_SCRIPT="${SCRIPTS_DIR}/pad.awk"
    cd "${TEST_DIR}"
}

teardown() {
    cd "${ORIGINAL_DIR}"
    teardown_test_environment
}

@test "pad.awk exists" {
    [ -f "${AWK_SCRIPT}" ]
}

@test "pad.awk pads short lines to 80 characters" {
    # Create a test file with short lines
    echo "SHORT LINE" > "${TEST_DIR}/input.txt"
    
    # Run pad.awk
    run awk -f "${AWK_SCRIPT}" "${TEST_DIR}/input.txt"
    [ "$status" -eq 0 ]
    
    # Check that output is exactly 80 characters (plus newline)
    output_length=$(echo "$output" | head -1 | wc -c)
    [ "$output_length" -eq 81 ]  # 80 chars + newline
}

@test "pad.awk handles empty lines" {
    # Create a test file with empty line
    echo "" > "${TEST_DIR}/input.txt"
    
    # Run pad.awk
    run awk -f "${AWK_SCRIPT}" "${TEST_DIR}/input.txt"
    [ "$status" -eq 0 ]
    
    # Check that output is exactly 80 characters (plus newline)
    output_length=$(echo "$output" | head -1 | wc -c)
    [ "$output_length" -eq 81 ]  # 80 spaces + newline
}

@test "pad.awk handles lines exactly 80 characters" {
    # Create a test file with exactly 80 character line
    printf '%.0sX' {1..80} > "${TEST_DIR}/input.txt"
    echo "" >> "${TEST_DIR}/input.txt"
    
    # Run pad.awk
    run awk -f "${AWK_SCRIPT}" "${TEST_DIR}/input.txt"
    [ "$status" -eq 0 ]
    
    # Check that output is exactly 80 characters (plus newline)
    output_length=$(echo "$output" | head -1 | wc -c)
    [ "$output_length" -eq 81 ]  # 80 chars + newline
}

@test "pad.awk removes carriage returns" {
    # Create a test file with DOS line endings
    printf "LINE WITH CR\r\n" > "${TEST_DIR}/input.txt"
    
    # Run pad.awk
    run awk -f "${AWK_SCRIPT}" "${TEST_DIR}/input.txt"
    [ "$status" -eq 0 ]
    
    # Check that output does not contain carriage return
    [[ "$output" != *$'\r'* ]]
}

@test "pad.awk handles multiple lines" {
    # Create a test file with multiple lines
    cat > "${TEST_DIR}/input.txt" << 'EOF'
LINE ONE
LINE TWO
LINE THREE
EOF
    
    # Run pad.awk
    run awk -f "${AWK_SCRIPT}" "${TEST_DIR}/input.txt"
    [ "$status" -eq 0 ]
    
    # Check that we have 3 lines of output
    line_count=$(echo "$output" | wc -l)
    [ "$line_count" -eq 3 ]
}

@test "pad.awk handles COBOL source code" {
    # Create a test file with COBOL code
    cat > "${TEST_DIR}/input.txt" << 'EOF'
       IDENTIFICATION DIVISION.
       PROGRAM-ID. SAMPLE.
       PROCEDURE DIVISION.
           DISPLAY "HELLO".
           STOP RUN.
EOF
    
    # Run pad.awk
    run awk -f "${AWK_SCRIPT}" "${TEST_DIR}/input.txt"
    [ "$status" -eq 0 ]
    
    # Check that each line is padded to 80 characters
    while IFS= read -r line; do
        line_length=${#line}
        [ "$line_length" -eq 80 ]
    done <<< "$output"
}

@test "pad.awk handles JCL source code" {
    # Create a test file with JCL code
    cat > "${TEST_DIR}/input.txt" << 'EOF'
//SAMPLE   JOB 'SAMPLE',CLASS=A
//STEP1    EXEC PGM=IEFBR14
EOF
    
    # Run pad.awk
    run awk -f "${AWK_SCRIPT}" "${TEST_DIR}/input.txt"
    [ "$status" -eq 0 ]
    
    # Check that each line is padded to 80 characters
    while IFS= read -r line; do
        line_length=${#line}
        [ "$line_length" -eq 80 ]
    done <<< "$output"
}

@test "pad.awk preserves leading spaces" {
    # Create a test file with leading spaces
    echo "       COBOL CODE" > "${TEST_DIR}/input.txt"
    
    # Run pad.awk
    run awk -f "${AWK_SCRIPT}" "${TEST_DIR}/input.txt"
    [ "$status" -eq 0 ]
    
    # Check that leading spaces are preserved
    [[ "$output" == "       COBOL CODE"* ]]
}

@test "pad.awk handles special characters" {
    # Create a test file with special characters
    echo "LINE WITH SPECIAL: @#\$%^&*()" > "${TEST_DIR}/input.txt"
    
    # Run pad.awk
    run awk -f "${AWK_SCRIPT}" "${TEST_DIR}/input.txt"
    [ "$status" -eq 0 ]
    
    # Check that output is exactly 80 characters
    output_length=$(echo "$output" | head -1 | wc -c)
    [ "$output_length" -eq 81 ]  # 80 chars + newline
}
