#!/usr/bin/env bats

# Tests for run_full_batch.sh, run_posting.sh, run_interest_calc.sh
# Tests script structure, FTP tunnel checks, and job sequencing

setup() {
    export SCRIPT_DIR="$(cd "$(dirname "$BATS_TEST_FILENAME")/../scripts" && pwd)"
}

# === run_full_batch.sh tests ===

@test "run_full_batch.sh has bash shebang" {
    head -1 "$SCRIPT_DIR/run_full_batch.sh" | grep -q '#!/bin/bash'
}

@test "run_full_batch.sh checks FTP tunnel" {
    content=$(cat "$SCRIPT_DIR/run_full_batch.sh")
    [[ "$content" == *"2121"* ]]
    [[ "$content" == *"FTP Tunnel"* ]]
}

@test "run_full_batch.sh closes files first" {
    content=$(cat "$SCRIPT_DIR/run_full_batch.sh")
    [[ "$content" == *"CLOSEFIL.jcl"* ]]
}

@test "run_full_batch.sh opens files last" {
    content=$(cat "$SCRIPT_DIR/run_full_batch.sh")
    [[ "$content" == *"OPENFIL.jcl"* ]]
}

@test "run_full_batch.sh includes core processing job" {
    content=$(cat "$SCRIPT_DIR/run_full_batch.sh")
    [[ "$content" == *"POSTTRAN.jcl"* ]]
}

@test "run_full_batch.sh includes interest calculation" {
    content=$(cat "$SCRIPT_DIR/run_full_batch.sh")
    [[ "$content" == *"INTCALC.jcl"* ]]
}

@test "run_full_batch.sh refreshes account data" {
    content=$(cat "$SCRIPT_DIR/run_full_batch.sh")
    [[ "$content" == *"ACCTFILE.jcl"* ]]
}

@test "run_full_batch.sh refreshes card data" {
    content=$(cat "$SCRIPT_DIR/run_full_batch.sh")
    [[ "$content" == *"CARDFILE.jcl"* ]]
}

@test "run_full_batch.sh refreshes customer data" {
    content=$(cat "$SCRIPT_DIR/run_full_batch.sh")
    [[ "$content" == *"CUSTFILE.jcl"* ]]
}

@test "run_full_batch.sh refreshes cross-reference data" {
    content=$(cat "$SCRIPT_DIR/run_full_batch.sh")
    [[ "$content" == *"XREFFILE.jcl"* ]]
}

@test "run_full_batch.sh includes transaction backup" {
    content=$(cat "$SCRIPT_DIR/run_full_batch.sh")
    [[ "$content" == *"TRANBKP.jcl"* ]]
}

@test "run_full_batch.sh includes transaction index" {
    content=$(cat "$SCRIPT_DIR/run_full_batch.sh")
    [[ "$content" == *"TRANIDX.jcl"* ]]
}

@test "run_full_batch.sh includes combined transactions" {
    content=$(cat "$SCRIPT_DIR/run_full_batch.sh")
    [[ "$content" == *"COMBTRAN.jcl"* ]]
}

@test "run_full_batch.sh uses JES filetype" {
    content=$(cat "$SCRIPT_DIR/run_full_batch.sh")
    [[ "$content" == *"filetype=JES"* ]]
}

@test "run_full_batch.sh includes sleep for job ordering" {
    content=$(cat "$SCRIPT_DIR/run_full_batch.sh")
    [[ "$content" == *"sleep"* ]]
}

@test "run_full_batch.sh includes user security refresh" {
    content=$(cat "$SCRIPT_DIR/run_full_batch.sh")
    [[ "$content" == *"DUSRSECJ.jcl"* ]]
}

@test "run_full_batch.sh includes disclosure group" {
    content=$(cat "$SCRIPT_DIR/run_full_batch.sh")
    [[ "$content" == *"DISCGRP.jcl"* ]]
}

@test "run_full_batch.sh includes transaction type" {
    content=$(cat "$SCRIPT_DIR/run_full_batch.sh")
    [[ "$content" == *"TRANTYPE.jcl"* ]]
}

# === run_posting.sh tests ===

@test "run_posting.sh has bash shebang" {
    head -1 "$SCRIPT_DIR/run_posting.sh" | grep -q '#!/bin/bash'
}

@test "run_posting.sh checks FTP tunnel" {
    content=$(cat "$SCRIPT_DIR/run_posting.sh")
    [[ "$content" == *"2121"* ]]
    [[ "$content" == *"FTP Tunnel"* ]]
}

@test "run_posting.sh closes files first" {
    content=$(cat "$SCRIPT_DIR/run_posting.sh")
    [[ "$content" == *"CLOSEFIL.jcl"* ]]
}

@test "run_posting.sh opens files last" {
    content=$(cat "$SCRIPT_DIR/run_posting.sh")
    [[ "$content" == *"OPENFIL.jcl"* ]]
}

@test "run_posting.sh includes core posting job" {
    content=$(cat "$SCRIPT_DIR/run_posting.sh")
    [[ "$content" == *"POSTTRAN.jcl"* ]]
}

@test "run_posting.sh refreshes account data" {
    content=$(cat "$SCRIPT_DIR/run_posting.sh")
    [[ "$content" == *"ACCTFILE.jcl"* ]]
}

@test "run_posting.sh includes transaction index" {
    content=$(cat "$SCRIPT_DIR/run_posting.sh")
    [[ "$content" == *"TRANIDX.jcl"* ]]
}

@test "run_posting.sh uses JES filetype" {
    content=$(cat "$SCRIPT_DIR/run_posting.sh")
    [[ "$content" == *"filetype=JES"* ]]
}

# === run_interest_calc.sh tests ===

@test "run_interest_calc.sh has bash shebang" {
    head -1 "$SCRIPT_DIR/run_interest_calc.sh" | grep -q '#!/bin/bash'
}

@test "run_interest_calc.sh checks FTP tunnel" {
    content=$(cat "$SCRIPT_DIR/run_interest_calc.sh")
    [[ "$content" == *"2121"* ]]
    [[ "$content" == *"FTP Tunnel"* ]]
}

@test "run_interest_calc.sh closes files first" {
    content=$(cat "$SCRIPT_DIR/run_interest_calc.sh")
    [[ "$content" == *"CLOSEFIL.jcl"* ]]
}

@test "run_interest_calc.sh opens files last" {
    content=$(cat "$SCRIPT_DIR/run_interest_calc.sh")
    [[ "$content" == *"OPENFIL.jcl"* ]]
}

@test "run_interest_calc.sh includes interest calculation" {
    content=$(cat "$SCRIPT_DIR/run_interest_calc.sh")
    [[ "$content" == *"INTCALC.jcl"* ]]
}

@test "run_interest_calc.sh includes transaction backup" {
    content=$(cat "$SCRIPT_DIR/run_interest_calc.sh")
    [[ "$content" == *"TRANBKP.jcl"* ]]
}

@test "run_interest_calc.sh includes combined transactions" {
    content=$(cat "$SCRIPT_DIR/run_interest_calc.sh")
    [[ "$content" == *"COMBTRAN.jcl"* ]]
}

@test "run_interest_calc.sh includes transaction index" {
    content=$(cat "$SCRIPT_DIR/run_interest_calc.sh")
    [[ "$content" == *"TRANIDX.jcl"* ]]
}

@test "run_interest_calc.sh prints running message" {
    content=$(cat "$SCRIPT_DIR/run_interest_calc.sh")
    [[ "$content" == *"Running Interest Calculation"* ]]
}

@test "run_interest_calc.sh uses JES filetype" {
    content=$(cat "$SCRIPT_DIR/run_interest_calc.sh")
    [[ "$content" == *"filetype=JES"* ]]
}
