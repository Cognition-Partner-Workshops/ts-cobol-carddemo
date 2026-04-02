#!/usr/bin/env bats

# Tests for git-addSrcVersionInfo.sh
# Tests file validation, extension detection, and comment syntax selection

setup() {
    # Create a temporary directory for test files
    export TEST_DIR="$(mktemp -d)"
    export SCRIPT_DIR="$(cd "$(dirname "$BATS_TEST_FILENAME")/../scripts" && pwd)"
    
    # Initialize a git repo in the temp dir for the script to work
    cd "$TEST_DIR"
    git init -q
    git config user.email "test@test.com"
    git config user.name "Test"
}

teardown() {
    rm -rf "$TEST_DIR"
}

@test "exits with error when no file_name parameter is provided" {
    run bash "$SCRIPT_DIR/git-addSrcVersionInfo.sh"
    [ "$status" -eq 99 ]
    [[ "$output" == *"file_name must be passed as input parameter"* ]]
}

@test "exits with error when file does not exist" {
    run bash "$SCRIPT_DIR/git-addSrcVersionInfo.sh" "$TEST_DIR/nonexistent.cbl"
    [ "$status" -eq 99 ]
    [[ "$output" == *"does NOT exist"* ]]
}

@test "recognizes .cbl extension as COBOL" {
    # Create a test COBOL file and commit it
    cd "$TEST_DIR"
    echo "       IDENTIFICATION DIVISION." > test.cbl
    git add test.cbl
    git commit -q -m "initial"
    
    run bash "$SCRIPT_DIR/git-addSrcVersionInfo.sh" "$TEST_DIR/test.cbl"
    [ "$status" -eq 0 ]
    # Check that COBOL comment syntax was used
    grep -q '      \*' "$TEST_DIR/test.cbl" || grep -q 'Ver: CardDemo' "$TEST_DIR/test.cbl"
}

@test "recognizes .cob extension as COBOL" {
    cd "$TEST_DIR"
    echo "       IDENTIFICATION DIVISION." > test.cob
    git add test.cob
    git commit -q -m "initial"
    
    run bash "$SCRIPT_DIR/git-addSrcVersionInfo.sh" "$TEST_DIR/test.cob"
    [ "$status" -eq 0 ]
}

@test "recognizes .cpy extension as COBOL copybook" {
    cd "$TEST_DIR"
    echo "       01 WS-FIELD PIC X(10)." > test.cpy
    git add test.cpy
    git commit -q -m "initial"
    
    run bash "$SCRIPT_DIR/git-addSrcVersionInfo.sh" "$TEST_DIR/test.cpy"
    [ "$status" -eq 0 ]
}

@test "recognizes .jcl extension as JCL" {
    cd "$TEST_DIR"
    echo "//JOBNAME JOB" > test.jcl
    git add test.jcl
    git commit -q -m "initial"
    
    run bash "$SCRIPT_DIR/git-addSrcVersionInfo.sh" "$TEST_DIR/test.jcl"
    [ "$status" -eq 0 ]
    grep -q 'Ver: CardDemo' "$TEST_DIR/test.jcl"
}

@test "recognizes .prc extension as JCL procedure" {
    cd "$TEST_DIR"
    echo "//PROCSTEP EXEC PGM=IEFBR14" > test.prc
    git add test.prc
    git commit -q -m "initial"
    
    run bash "$SCRIPT_DIR/git-addSrcVersionInfo.sh" "$TEST_DIR/test.prc"
    [ "$status" -eq 0 ]
}

@test "recognizes .proc extension as JCL procedure" {
    cd "$TEST_DIR"
    echo "//PROCSTEP EXEC PGM=IEFBR14" > test.proc
    git add test.proc
    git commit -q -m "initial"
    
    run bash "$SCRIPT_DIR/git-addSrcVersionInfo.sh" "$TEST_DIR/test.proc"
    [ "$status" -eq 0 ]
}

@test "recognizes .bms extension as BMS map" {
    cd "$TEST_DIR"
    echo "MAPSET DFHMSD TYPE=MAP" > test.bms
    git add test.bms
    git commit -q -m "initial"
    
    run bash "$SCRIPT_DIR/git-addSrcVersionInfo.sh" "$TEST_DIR/test.bms"
    [ "$status" -eq 0 ]
    grep -q 'Ver: CardDemo' "$TEST_DIR/test.bms"
}

@test "recognizes .py extension as Python" {
    cd "$TEST_DIR"
    echo "print('hello')" > test.py
    git add test.py
    git commit -q -m "initial"
    
    run bash "$SCRIPT_DIR/git-addSrcVersionInfo.sh" "$TEST_DIR/test.py"
    [ "$status" -eq 0 ]
    grep -q '##' "$TEST_DIR/test.py"
}

@test "exits with error for unknown file extension" {
    cd "$TEST_DIR"
    echo "unknown content" > test.xyz
    git add test.xyz
    git commit -q -m "initial"
    
    run bash "$SCRIPT_DIR/git-addSrcVersionInfo.sh" "$TEST_DIR/test.xyz"
    [ "$status" -eq 99 ]
    [[ "$output" == *"Unknown file type"* ]]
}

@test "exits with error for .txt extension" {
    cd "$TEST_DIR"
    echo "text content" > test.txt
    git add test.txt
    git commit -q -m "initial"
    
    run bash "$SCRIPT_DIR/git-addSrcVersionInfo.sh" "$TEST_DIR/test.txt"
    [ "$status" -eq 99 ]
}

@test "adds version info to file without existing version" {
    cd "$TEST_DIR"
    echo "       IDENTIFICATION DIVISION." > test.cbl
    git add test.cbl
    git commit -q -m "initial"
    
    bash "$SCRIPT_DIR/git-addSrcVersionInfo.sh" "$TEST_DIR/test.cbl"
    
    grep -q 'Ver: CardDemo_' "$TEST_DIR/test.cbl"
    grep -q 'Date:' "$TEST_DIR/test.cbl"
}

@test "updates existing version info in file" {
    cd "$TEST_DIR"
    cat > test.cbl << 'EOF'
       IDENTIFICATION DIVISION.
      * Ver: CardDemo_v2.0-0-gabcdef-1 Date: 2024-01-01 00:00:00 UTC
      *
EOF
    git add test.cbl
    git commit -q -m "initial"
    
    bash "$SCRIPT_DIR/git-addSrcVersionInfo.sh" "$TEST_DIR/test.cbl"
    
    # Version should be updated (different date)
    grep -q 'Ver: CardDemo_' "$TEST_DIR/test.cbl"
}

@test "handles case-insensitive file extensions" {
    cd "$TEST_DIR"
    echo "       IDENTIFICATION DIVISION." > test.CBL
    git add test.CBL
    git commit -q -m "initial"
    
    run bash "$SCRIPT_DIR/git-addSrcVersionInfo.sh" "$TEST_DIR/test.CBL"
    [ "$status" -eq 0 ]
}

@test "version info contains app name CardDemo" {
    cd "$TEST_DIR"
    echo "print('hello')" > test.py
    git add test.py
    git commit -q -m "initial"
    
    bash "$SCRIPT_DIR/git-addSrcVersionInfo.sh" "$TEST_DIR/test.py"
    
    grep -q 'CardDemo' "$TEST_DIR/test.py"
}

@test "version info contains date stamp" {
    cd "$TEST_DIR"
    echo "print('hello')" > test.py
    git add test.py
    git commit -q -m "initial"
    
    bash "$SCRIPT_DIR/git-addSrcVersionInfo.sh" "$TEST_DIR/test.py"
    
    grep -q 'Date:' "$TEST_DIR/test.py"
}

@test "script exits with 0 on success" {
    cd "$TEST_DIR"
    echo "print('hello')" > test.py
    git add test.py
    git commit -q -m "initial"
    
    run bash "$SCRIPT_DIR/git-addSrcVersionInfo.sh" "$TEST_DIR/test.py"
    [ "$status" -eq 0 ]
}
