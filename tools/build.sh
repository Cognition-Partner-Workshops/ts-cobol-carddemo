#!/bin/sh
# Compile the batch estate into ./loadlib with GnuCOBOL 3 (+ Open COBOL ESQL
# for the Db2 sub-application).  Runs inside the estate container.
set -eu
cd "$(dirname "$0")/.."
LOADLIB=loadlib
CPY=work/cpy
mkdir -p "$LOADLIB" "$CPY" work/esql

# The committed copybooks are tab indented; fixed-format COBOL needs real
# columns, so expand them into a scratch directory (app/ is never edited).
for cpy in app/cpy/*.cpy; do
    expand -t8 "$cpy" > "$CPY/$(basename "$cpy")"
done

COBC="cobc -m -std=ibm -fsign=EBCDIC -I $CPY"
failed=0

compile() {
    src=$1; name=$(basename "$src" .cbl)
    if $COBC -o "$LOADLIB/$name.so" "$src" > "work/build-$name.log" 2>&1; then
        echo "$name: compiled"
    else
        echo "$name: FAILED (see work/build-$name.log)"; failed=1
    fi
}

# Batch programs only: CO* members are CICS/BMS online programs and are not
# built (no CICS translator locally); CBEXPORT/CBIMPORT are listed in
# docs/BASELINE.md as known GnuCOBOL failures.
for src in app/cbl/CB*.cbl app/cbl/CSUTLDTC.cbl app/cbl/COBSWAIT.cbl; do
    case "$(basename "$src")" in
        CBEXPORT.cbl|CBIMPORT.cbl) echo "$(basename "$src" .cbl): skipped (known GnuCOBOL failure, docs/BASELINE.md)";;
        *) compile "$src";;
    esac
done

# Local stand-ins (PARM driver, LE abend stub, synthetic PL/I caller).
for src in tools/stubs/PARMDRV.cbl tools/stubs/CEE3ABD.cbl; do
    compile "$src"
done

# Db2 sub-application through Open COBOL ESQL.
if command -v ocesql > /dev/null 2>&1; then
    python3 tools/esql_prep.py
    cp tools/stubs/DSNRUN.cbl tools/stubs/DSNEXIT.cbl work/esql/
    for src in work/esql/*.cbl; do
        name=$(basename "$src" .cbl)
        if ocesql --inc=work/esql/inc "$src" "work/esql/$name.cob" > "work/build-$name.log" 2>&1 \
           && cobc -m -std=ibm -I work/esql/inc -o "$LOADLIB/$name.so" "work/esql/$name.cob" \
                -L/usr/local/lib -locesql >> "work/build-$name.log" 2>&1; then
            echo "$name: precompiled (ocesql) and compiled"
        else
            echo "$name: FAILED (see work/build-$name.log)"; failed=1
        fi
    done
else
    echo "ocesql not found: Db2 sub-application not built"
fi

exit $failed
