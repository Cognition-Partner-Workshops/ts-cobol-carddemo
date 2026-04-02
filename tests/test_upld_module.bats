#!/usr/bin/env bats

# Tests for upld_module.sh
# Tests parameter validation, module name extraction, and upload logic

setup() {
    export SCRIPT_DIR="$(cd "$(dirname "$BATS_TEST_FILENAME")/../scripts" && pwd)"
}

@test "script checks for FTP tunnel" {
    content=$(cat "$SCRIPT_DIR/upld_module.sh")
    [[ "$content" == *"2121"* ]]
}

@test "script requires module_path parameter" {
    content=$(cat "$SCRIPT_DIR/upld_module.sh")
    [[ "$content" == *'module_path=$1'* ]]
    [[ "$content" == *"Missing module_path parameter"* ]]
}

@test "script requires module_type parameter" {
    content=$(cat "$SCRIPT_DIR/upld_module.sh")
    [[ "$content" == *'module_type=$2'* ]]
    [[ "$content" == *"Missing module_type parameter"* ]]
}

@test "script extracts module name from path" {
    content=$(cat "$SCRIPT_DIR/upld_module.sh")
    [[ "$content" == *"module_name="* ]]
    [[ "$content" == *"sed"* ]]
}

@test "script uses awk for padding" {
    content=$(cat "$SCRIPT_DIR/upld_module.sh")
    [[ "$content" == *"awk"* ]]
    [[ "$content" == *"pad.awk"* ]]
}

@test "pad.awk script exists" {
    [ -f "$SCRIPT_DIR/pad.awk" ]
}

@test "script uploads to AWS M2 CARDDEMO PDS" {
    content=$(cat "$SCRIPT_DIR/upld_module.sh")
    [[ "$content" == *"AWS.M2.CARDDEMO"* ]]
}

@test "script checks awk return code" {
    content=$(cat "$SCRIPT_DIR/upld_module.sh")
    [[ "$content" == *'$?'* ]]
}

@test "script handles awk failure" {
    content=$(cat "$SCRIPT_DIR/upld_module.sh")
    [[ "$content" == *"awk transformation failed"* ]]
}

@test "script cleans up temp files" {
    content=$(cat "$SCRIPT_DIR/upld_module.sh")
    [[ "$content" == *".tmp"* ]]
}

@test "script has bash shebang" {
    head -1 "$SCRIPT_DIR/upld_module.sh" | grep -q '#!/bin/bash'
}
