#!/usr/bin/env bash
# Run the Java/Spring Batch candidate for one fixture case and write its outputs in the
# layout tools/parity/record.py produces, for tools/parity/compare.py --actual <out>.
#
#   modern/run-candidate.sh <chain> <case> <out>
#
# Convention for every converted chain: Maven module modern/<chain>, Spring Boot jar
# modern/<chain>/target/<chain>.jar taking --case=<fixture dir> --out=<dir>.
set -euo pipefail
chain=$1 case=$2 out=$3
root=$(cd "$(dirname "$0")/.." && pwd)
jar="$root/modern/$chain/target/$chain.jar"
if [ -n "${JAVA_JAR_DIR:-}" ]; then jar="$JAVA_JAR_DIR/$chain.jar"; fi
[ -f "$jar" ] || { echo "no candidate jar $jar (run make modern-build)" >&2; exit 2; }
exec java -jar "$jar" --estate.root="$root" --case="$root/fixtures/$chain/$case" --out="$out"
