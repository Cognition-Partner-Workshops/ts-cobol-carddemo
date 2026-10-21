#!/usr/bin/env bash
# Rebuild one state of the batch path on a local kind cluster and time each stage.
#   scripts/sdd/reset.sh before [git-ref]   -> namespace sdd-2610-before
#   scripts/sdd/reset.sh after  [git-ref]   -> namespace sdd-2610-after
# The pages are served at http://<state>.<run>.localtest.me:8080/ once the report job finishes.
set -euo pipefail

STATE=${1:?usage: reset.sh before|after [git-ref]}
REF=${2:-HEAD}
RUN=${SDD_RUN:-sdd-2610}
JOBS=${SDD_JOBS:-POSTTRAN,INTCALC}
NS="$RUN-$STATE"
HOST="$STATE.$RUN.localtest.me"
ROOT=$(git rev-parse --show-toplevel)
WORK="$ROOT/spec/runs"
SRC="$WORK/src-$STATE"
TIMES="$WORK/reset-$STATE.json"
EXPIRES=$(date -u -d '+3 days' +%Y-%m-%d)
LABEL="CardDemo batch path, $STATE"
export NS RUN STATE HOST JOBS EXPIRES LABEL
mkdir -p "$WORK"
echo "{" > "$TIMES"

stage() {
  local name=$1; shift
  local t0; t0=$(date +%s)
  echo "== $name"
  "$@"
  local secs=$(( $(date +%s) - t0 ))
  echo "  \"$name\": $secs," >> "$TIMES"
  echo "   $name took ${secs}s"
}

cluster() {
  if ! kind get clusters 2>/dev/null | grep -qx "$RUN"; then
    kind create cluster --name "$RUN" --config "$ROOT/deploy/kind/cluster.yaml" --wait 120s
    kubectl --context "kind-$RUN" apply -f \
      https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.11.2/deploy/static/provider/kind/deploy.yaml
  fi
  kubectl --context "kind-$RUN" -n ingress-nginx wait --for=condition=available deployment/ingress-nginx-controller --timeout=300s
}

images() {
  rm -rf "$SRC"
  git -C "$ROOT" worktree prune
  git -C "$ROOT" worktree add --detach "$SRC" "$REF" >/dev/null
  (cd "$SRC/java" && mvn -q -B package -DskipTests)
  docker build -q -f "$SRC/spec/runner/Dockerfile" -t "carddemo-cobol-runner:$STATE" "$SRC"
  docker build -q -t "carddemo-java:$STATE" "$SRC/java"
  kind load docker-image --name "$RUN" "carddemo-cobol-runner:$STATE" "carddemo-java:$STATE"
}

namespace() {
  kubectl --context "kind-$RUN" delete namespace "$NS" --ignore-not-found --wait=true
  envsubst < "$ROOT/deploy/kind/run.yaml" | kubectl --context "kind-$RUN" apply -f -
  kubectl --context "kind-$RUN" -n "$NS" rollout status deployment/postgres --timeout=180s
}

job() {
  envsubst < "$ROOT/deploy/kind/job-$1.yaml" | kubectl --context "kind-$RUN" apply -f -
  if ! kubectl --context "kind-$RUN" -n "$NS" wait --for=condition=complete "job/$1-run" "job/$1" --timeout=600s 2>/dev/null \
     && ! kubectl --context "kind-$RUN" -n "$NS" get job "$1-run" -o jsonpath='{.status.succeeded}' 2>/dev/null | grep -q 1 \
     && ! kubectl --context "kind-$RUN" -n "$NS" get job "$1" -o jsonpath='{.status.succeeded}' 2>/dev/null | grep -q 1; then
    kubectl --context "kind-$RUN" -n "$NS" logs "job/$1-run" 2>/dev/null || kubectl --context "kind-$RUN" -n "$NS" logs "job/$1"
    return 1
  fi
}

pages() {
  kubectl --context "kind-$RUN" -n "$NS" rollout status deployment/site --timeout=180s
  for _ in $(seq 30); do
    if curl -fsS -o /dev/null "http://$HOST:8080/"; then echo "   http://$HOST:8080/"; return 0; fi
    sleep 2
  done
  return 1
}

stage cluster cluster
stage images images
stage namespace namespace
stage cobol job cobol
stage java job java
stage report job report
stage pages pages
echo "  \"namespace\": \"$NS\"" >> "$TIMES"
echo "}" >> "$TIMES"
cat "$TIMES"
