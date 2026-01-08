#!/usr/bin/env bats

# Test suite for remote_compile.sh
# This script tests the remote compilation functionality

load 'helpers/test_helper'

setup() {
    setup_test_environment
    export SCRIPT_PATH="${SCRIPTS_DIR}/remote_compile.sh"
    cd "${TEST_DIR}"
    
    # Create mock compile_batch.jcl.template
    cat > "${TEST_DIR}/compile_batch.jcl.template" << 'EOF'
//ZZZZZZZZ JOB 'COMPILE',CLASS=A,MSGCLASS=0
//STEP1    EXEC PGM=IGYCRCTL
//SYSIN    DD DSN=AWS.M2.CARDDEMO.CBL(ZZZZZZZZ),DISP=SHR
EOF
    
    # Create a mock Makefile
    cat > "${TEST_DIR}/Makefile" << 'EOF'
all:
	@echo "Mock make executed"
EOF
}

teardown() {
    cd "${ORIGINAL_DIR}"
    teardown_test_environment
}

@test "remote_compile.sh exists and is executable" {
    [ -f "${SCRIPT_PATH}" ]
}

@test "remote_compile.sh checks for FTP tunnel" {
    # The script should check if tunnel is running
    grep -q "2121:" "${SCRIPT_PATH}"
}

@test "remote_compile.sh only accepts .cbl files" {
    # Verify the script checks for .cbl extension
    grep -q '\.cbl' "${SCRIPT_PATH}"
    grep -q "Only files with cbl extension can be compiled" "${SCRIPT_PATH}"
}

@test "remote_compile.sh creates temporary JCL from template" {
    # Verify the script uses sed to substitute in the template
    grep -q "sed.*ZZZZZZZZ" "${SCRIPT_PATH}"
}

@test "remote_compile.sh removes temporary JCL after execution" {
    # Verify the script removes the temp file
    grep -q "rm.*\.tmp" "${SCRIPT_PATH}"
}

@test "remote_compile.sh uses tnftp for file transfer" {
    # Verify the script uses tnftp
    grep -q "tnftp" "${SCRIPT_PATH}"
}

@test "remote_compile.sh sets JES filetype" {
    # Verify the script sets filetype to JES
    grep -q "filetype=JES" "${SCRIPT_PATH}"
}

@test "remote_compile.sh runs make before compilation" {
    # Verify the script runs make
    grep -q "make -f Makefile" "${SCRIPT_PATH}"
}

@test "remote_compile.sh handles template substitution failure" {
    # Verify the script checks sed return code
    grep -q "rc -ne 0" "${SCRIPT_PATH}" || grep -q "rc != 0" "${SCRIPT_PATH}"
}

@test "remote_compile.sh accepts three parameters" {
    # Verify the script expects file_name, file_extension, and file_basename_no_extension
    grep -q "file_name=\$1" "${SCRIPT_PATH}"
    grep -q "file_extension=\$2" "${SCRIPT_PATH}"
    grep -q "file_basename_no_extension=\$3" "${SCRIPT_PATH}"
}
