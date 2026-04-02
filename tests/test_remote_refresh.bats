#!/usr/bin/env bats

# Tests for remote_refresh.sh
# Tests FTP tunnel check, file refresh sequence, and job ordering

setup() {
    export SCRIPT_DIR="$(cd "$(dirname "$BATS_TEST_FILENAME")/../scripts" && pwd)"
}

@test "remote_refresh.sh has bash shebang" {
    head -1 "$SCRIPT_DIR/remote_refresh.sh" | grep -q '#!/bin/bash'
}

@test "remote_refresh.sh checks FTP tunnel" {
    content=$(cat "$SCRIPT_DIR/remote_refresh.sh")
    [[ "$content" == *"2121"* ]]
    [[ "$content" == *"FTP Tunnel"* ]]
}

@test "remote_refresh.sh closes files first" {
    content=$(cat "$SCRIPT_DIR/remote_refresh.sh")
    [[ "$content" == *"CLOSEFIL.jcl"* ]]
}

@test "remote_refresh.sh opens files last" {
    content=$(cat "$SCRIPT_DIR/remote_refresh.sh")
    [[ "$content" == *"OPENFIL.jcl"* ]]
}

@test "remote_refresh.sh refreshes account master" {
    content=$(cat "$SCRIPT_DIR/remote_refresh.sh")
    [[ "$content" == *"ACCTFILE.jcl"* ]]
}

@test "remote_refresh.sh refreshes card master" {
    content=$(cat "$SCRIPT_DIR/remote_refresh.sh")
    [[ "$content" == *"CARDFILE.jcl"* ]]
}

@test "remote_refresh.sh refreshes cross-reference" {
    content=$(cat "$SCRIPT_DIR/remote_refresh.sh")
    [[ "$content" == *"XREFFILE.jcl"* ]]
}

@test "remote_refresh.sh refreshes customer master" {
    content=$(cat "$SCRIPT_DIR/remote_refresh.sh")
    [[ "$content" == *"CUSTFILE.jcl"* ]]
}

@test "remote_refresh.sh refreshes transaction master" {
    content=$(cat "$SCRIPT_DIR/remote_refresh.sh")
    [[ "$content" == *"TRANFILE.jcl"* ]]
}

@test "remote_refresh.sh refreshes disclosure group" {
    content=$(cat "$SCRIPT_DIR/remote_refresh.sh")
    [[ "$content" == *"DISCGRP.jcl"* ]]
}

@test "remote_refresh.sh refreshes transaction category balance" {
    content=$(cat "$SCRIPT_DIR/remote_refresh.sh")
    [[ "$content" == *"TCATBALF.jcl"* ]]
}

@test "remote_refresh.sh refreshes transaction category file" {
    content=$(cat "$SCRIPT_DIR/remote_refresh.sh")
    [[ "$content" == *"TRANCATG.jcl"* ]]
}

@test "remote_refresh.sh refreshes transaction type" {
    content=$(cat "$SCRIPT_DIR/remote_refresh.sh")
    [[ "$content" == *"TRANTYPE.jcl"* ]]
}

@test "remote_refresh.sh refreshes user security" {
    content=$(cat "$SCRIPT_DIR/remote_refresh.sh")
    [[ "$content" == *"DUSRSECJ.jcl"* ]]
}

@test "remote_refresh.sh uses JES filetype" {
    content=$(cat "$SCRIPT_DIR/remote_refresh.sh")
    [[ "$content" == *"filetype=JES"* ]]
}

@test "remote_refresh.sh connects to localhost 2121" {
    content=$(cat "$SCRIPT_DIR/remote_refresh.sh")
    [[ "$content" == *"localhost 2121"* ]]
}

@test "remote_refresh.sh includes data refresh complete message" {
    content=$(cat "$SCRIPT_DIR/remote_refresh.sh")
    [[ "$content" == *"Data Refresh Complete"* ]]
}
