#!/usr/bin/env bash
# Remove everything reset.sh created for the run and print the checks that prove it.
set -uo pipefail
RUN=${SDD_RUN:-sdd-2610}
ROOT=$(git rev-parse --show-toplevel)

kind delete cluster --name "$RUN"
docker ps -aq --filter "name=$RUN" | xargs -r docker rm -f
docker volume ls -q --filter "name=$RUN" | xargs -r docker volume rm
for st in before after; do
  docker network ls -q --filter "label=com.docker.compose.project=$RUN-$st" | xargs -r docker network rm >/dev/null
done
for img in carddemo-cobol-runner:before carddemo-cobol-runner:after carddemo-java:before carddemo-java:after carddemo-postgres:16 carddemo-nginx:1.27; do
  docker image rm "$img" >/dev/null 2>&1 || true
done
for wt in "$ROOT"/spec/runs/src-*; do
  [ -d "$wt" ] && git -C "$ROOT" worktree remove --force "$wt"
done
git -C "$ROOT" worktree prune

echo "== kind get clusters"; kind get clusters 2>&1
echo "== docker ps -a --filter name=$RUN"; docker ps -a --filter "name=$RUN"
echo "== docker volume ls --filter name=$RUN"; docker volume ls --filter "name=$RUN"
echo "== docker network ls --filter label=com.docker.compose.project=$RUN-{before,after}"
docker network ls --filter "label=com.docker.compose.project=$RUN-before"; docker network ls -q --filter "label=com.docker.compose.project=$RUN-after"
