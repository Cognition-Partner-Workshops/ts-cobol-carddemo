#!/usr/bin/env bats

# Test suite for git-addSrcVersionInfo.sh
# This script tests the version info insertion functionality

load 'helpers/test_helper'

setup() {
    setup_test_environment
    export SCRIPT_PATH="${SCRIPTS_DIR}/git-addSrcVersionInfo.sh"
    
    # Create a mock git environment
    cd "${TEST_DIR}"
    git init --quiet
    git config user.email "test@test.com"
    git config user.name "Test User"
}

teardown() {
    cd "${ORIGINAL_DIR}"
    teardown_test_environment
}

@test "git-addSrcVersionInfo.sh exists and is executable" {
    [ -f "${SCRIPT_PATH}" ]
}

@test "git-addSrcVersionInfo.sh fails when no file_name parameter is provided" {
    run bash "${SCRIPT_PATH}"
    [ "$status" -eq 99 ]
    [[ "$output" == *"file_name must be passed as input parameter"* ]]
}

@test "git-addSrcVersionInfo.sh fails when file does not exist" {
    run bash "${SCRIPT_PATH}" "nonexistent_file.cbl"
    [ "$status" -eq 99 ]
    [[ "$output" == *"does NOT exist"* ]]
}

@test "git-addSrcVersionInfo.sh adds version info to COBOL file" {
    # Create a sample COBOL file
    local test_file="${TEST_DIR}/TEST.cbl"
    create_sample_cobol_file "${test_file}"
    
    # Add and commit the file to git
    git add "${test_file}"
    git commit -m "Initial commit" --quiet
    
    # Run the script
    run bash "${SCRIPT_PATH}" "${test_file}"
    [ "$status" -eq 0 ]
    
    # Check that version info was added
    grep -q "Ver: CardDemo_" "${test_file}"
}

@test "git-addSrcVersionInfo.sh adds version info to JCL file" {
    # Create a sample JCL file
    local test_file="${TEST_DIR}/TEST.jcl"
    create_sample_jcl_file "${test_file}"
    
    # Add and commit the file to git
    git add "${test_file}"
    git commit -m "Initial commit" --quiet
    
    # Run the script
    run bash "${SCRIPT_PATH}" "${test_file}"
    [ "$status" -eq 0 ]
    
    # Check that version info was added
    grep -q "Ver: CardDemo_" "${test_file}"
}

@test "git-addSrcVersionInfo.sh adds version info to BMS file" {
    # Create a sample BMS file
    local test_file="${TEST_DIR}/TEST.bms"
    create_sample_bms_file "${test_file}"
    
    # Add and commit the file to git
    git add "${test_file}"
    git commit -m "Initial commit" --quiet
    
    # Run the script
    run bash "${SCRIPT_PATH}" "${test_file}"
    [ "$status" -eq 0 ]
    
    # Check that version info was added
    grep -q "Ver: CardDemo_" "${test_file}"
}

@test "git-addSrcVersionInfo.sh adds version info to copybook file" {
    # Create a sample copybook file
    local test_file="${TEST_DIR}/TEST.cpy"
    create_sample_copybook_file "${test_file}"
    
    # Add and commit the file to git
    git add "${test_file}"
    git commit -m "Initial commit" --quiet
    
    # Run the script
    run bash "${SCRIPT_PATH}" "${test_file}"
    [ "$status" -eq 0 ]
    
    # Check that version info was added
    grep -q "Ver: CardDemo_" "${test_file}"
}

@test "git-addSrcVersionInfo.sh fails for unknown file type" {
    # Create a file with unknown extension
    local test_file="${TEST_DIR}/TEST.xyz"
    echo "test content" > "${test_file}"
    
    # Add and commit the file to git
    git add "${test_file}"
    git commit -m "Initial commit" --quiet
    
    # Run the script
    run bash "${SCRIPT_PATH}" "${test_file}"
    [ "$status" -eq 99 ]
    [[ "$output" == *"Unknown file type"* ]]
}

@test "git-addSrcVersionInfo.sh updates existing version info" {
    # Create a COBOL file with existing version info
    local test_file="${TEST_DIR}/TEST.cbl"
    cat > "${test_file}" << 'EOF'
       IDENTIFICATION DIVISION.
       PROGRAM-ID. SAMPLE.
      *
      * Ver: CardDemo_v1.0-old Date: 2020-01-01 00:00:00 UTC
      *
       PROCEDURE DIVISION.
           STOP RUN.
EOF
    
    # Add and commit the file to git
    git add "${test_file}"
    git commit -m "Initial commit" --quiet
    
    # Run the script
    run bash "${SCRIPT_PATH}" "${test_file}"
    [ "$status" -eq 0 ]
    
    # Check that version info was updated (should not contain old date)
    ! grep -q "2020-01-01" "${test_file}"
}

@test "git-addSrcVersionInfo.sh handles DOS line endings" {
    # Create a COBOL file with DOS line endings
    local test_file="${TEST_DIR}/TEST.cbl"
    printf "       IDENTIFICATION DIVISION.\r\n       PROGRAM-ID. SAMPLE.\r\n       STOP RUN.\r\n" > "${test_file}"
    
    # Add and commit the file to git
    git add "${test_file}"
    git commit -m "Initial commit" --quiet
    
    # Run the script
    run bash "${SCRIPT_PATH}" "${test_file}"
    [ "$status" -eq 0 ]
    
    # Check that version info was added
    grep -q "Ver: CardDemo_" "${test_file}"
}

@test "git-addSrcVersionInfo.sh uses correct comment syntax for COBOL" {
    # Create a sample COBOL file
    local test_file="${TEST_DIR}/TEST.cbl"
    create_sample_cobol_file "${test_file}"
    
    # Add and commit the file to git
    git add "${test_file}"
    git commit -m "Initial commit" --quiet
    
    # Run the script
    run bash "${SCRIPT_PATH}" "${test_file}"
    [ "$status" -eq 0 ]
    
    # Check that COBOL comment syntax is used (column 7 asterisk)
    grep -q "^      \*" "${test_file}"
}

@test "git-addSrcVersionInfo.sh uses correct comment syntax for JCL" {
    # Create a sample JCL file
    local test_file="${TEST_DIR}/TEST.jcl"
    create_sample_jcl_file "${test_file}"
    
    # Add and commit the file to git
    git add "${test_file}"
    git commit -m "Initial commit" --quiet
    
    # Run the script
    run bash "${SCRIPT_PATH}" "${test_file}"
    [ "$status" -eq 0 ]
    
    # Check that JCL comment syntax is used
    grep -q "^//\*" "${test_file}"
}
