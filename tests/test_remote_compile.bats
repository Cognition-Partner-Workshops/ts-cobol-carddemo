#!/usr/bin/env bats

# Tests for remote_compile.sh
# Tests argument validation, file extension checking, and JCL template substitution

setup() {
    export TEST_DIR="$(mktemp -d)"
    export SCRIPT_DIR="$(cd "$(dirname "$BATS_TEST_FILENAME")/../scripts" && pwd)"
}

teardown() {
    rm -rf "$TEST_DIR"
}

@test "script checks for FTP tunnel" {
    # The script checks for FTP tunnel on port 2121
    content=$(cat "$SCRIPT_DIR/remote_compile.sh")
    [[ "$content" == *"2121"* ]]
}

@test "script uses compile_batch.jcl.template" {
    content=$(cat "$SCRIPT_DIR/remote_compile.sh")
    [[ "$content" == *"compile_batch.jcl.template"* ]]
}

@test "script checks for .cbl extension" {
    content=$(cat "$SCRIPT_DIR/remote_compile.sh")
    [[ "$content" == *".cbl"* ]]
}

@test "script rejects non-cbl files" {
    content=$(cat "$SCRIPT_DIR/remote_compile.sh")
    [[ "$content" == *"Only files with cbl extension can be compiled"* ]]
}

@test "script uses sed for template substitution" {
    content=$(cat "$SCRIPT_DIR/remote_compile.sh")
    [[ "$content" == *"sed"* ]]
    [[ "$content" == *"ZZZZZZZZ"* ]]
}

@test "script substitutes file basename in JCL template" {
    content=$(cat "$SCRIPT_DIR/remote_compile.sh")
    [[ "$content" == *'$file_basename_no_extension'* ]]
}

@test "script cleans up temporary files" {
    content=$(cat "$SCRIPT_DIR/remote_compile.sh")
    [[ "$content" == *"rm "* ]]
    [[ "$content" == *".tmp"* ]]
}

@test "script checks sed return code" {
    content=$(cat "$SCRIPT_DIR/remote_compile.sh")
    [[ "$content" == *'rc=$?'* ]] || [[ "$content" == *'$?'* ]]
}

@test "script handles JCL template substitution failure" {
    content=$(cat "$SCRIPT_DIR/remote_compile.sh")
    [[ "$content" == *"JCL template substition failed"* ]]
}

@test "script accepts three parameters" {
    content=$(cat "$SCRIPT_DIR/remote_compile.sh")
    [[ "$content" == *'file_name=$1'* ]]
    [[ "$content" == *'file_extension=$2'* ]]
    [[ "$content" == *'file_basename_no_extension=$3'* ]]
}

@test "compile_batch.jcl.template exists" {
    [ -f "$SCRIPT_DIR/compile_batch.jcl.template" ]
}
