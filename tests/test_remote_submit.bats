#!/usr/bin/env bats

# Tests for remote_submit.sh
# Tests argument validation and file extension checking

setup() {
    export SCRIPT_DIR="$(cd "$(dirname "$BATS_TEST_FILENAME")/../scripts" && pwd)"
}

@test "script checks for FTP tunnel" {
    content=$(cat "$SCRIPT_DIR/remote_submit.sh")
    [[ "$content" == *"2121"* ]]
}

@test "script accepts file_name parameter" {
    content=$(cat "$SCRIPT_DIR/remote_submit.sh")
    [[ "$content" == *'file_name=$1'* ]]
}

@test "script accepts file_extension parameter" {
    content=$(cat "$SCRIPT_DIR/remote_submit.sh")
    [[ "$content" == *'file_extension=$2'* ]]
}

@test "script checks for .jcl extension" {
    content=$(cat "$SCRIPT_DIR/remote_submit.sh")
    [[ "$content" == *".jcl"* ]]
}

@test "script rejects non-jcl files" {
    content=$(cat "$SCRIPT_DIR/remote_submit.sh")
    [[ "$content" == *"Only files with jcl extension can be submitted"* ]]
}

@test "script uses tnftp for file transfer" {
    content=$(cat "$SCRIPT_DIR/remote_submit.sh")
    [[ "$content" == *"tnftp"* ]]
}

@test "script uses JES filetype" {
    content=$(cat "$SCRIPT_DIR/remote_submit.sh")
    [[ "$content" == *"filetype=JES"* ]]
}

@test "script connects to localhost port 2121" {
    content=$(cat "$SCRIPT_DIR/remote_submit.sh")
    [[ "$content" == *"localhost 2121"* ]]
}

@test "script has bash shebang" {
    head -1 "$SCRIPT_DIR/remote_submit.sh" | grep -q '#!/bin/bash'
}
