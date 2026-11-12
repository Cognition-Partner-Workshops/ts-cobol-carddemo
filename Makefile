# CardDemo legacy estate: build and run the batch chains locally.
# Everything executes inside the `estate` Docker Compose service (GnuCOBOL 3,
# Open COBOL ESQL) against the `db` service (PostgreSQL 15).
#
#   make up                     start the containers (builds the image once)
#   make build                  compile app/cbl + Db2 sub-application into ./loadlib
#   make run CHAIN=posting      run one chain on its first fixture case (or CASE=...)
#   make record [CHAIN=..] [CASE=..]   (re)write golden outputs under fixtures/*/*/expected
#   make parity [CHAIN=..] [CASE=..]   rerun chains and compare field by field -> work/parity/report.md
#        [TARGET=legacy|java]        default both: the COBOL rerun and, for chains converted under
#                                    modern/<chain>, the Java/Spring Batch candidate
#   make modern-build               build and unit-test the Java 21 modules under modern/
#   make parity-naive CASE=i02-tie-half-cent   half-even Python reference vs golden (fails on the tie)
#   make chain-graph / chain-graph-check       regenerate / verify docs/chain-graph.md
#   make reset                  drop datasets, loadlib, work/ and the database volume

# run the estate container as the invoking user so bind-mounted loadlib/, work/,
# datasets/ stay writable on any host (CI runners are not uid 1000)
export UID := $(shell id -u)
export GID := $(shell id -g)

COMPOSE ?= docker compose
EXEC     = $(COMPOSE) exec -T estate
CHAINS  ?= posting interest trantype-update trantype-dynamic
CHAIN   ?=
CASE    ?=
PY       = python3
TARGET  ?= legacy java
MVN_ARGS ?=
MVN      = mvn -B -q -Dmaven.repo.local=/estate/work/m2 $(MVN_ARGS)

.PHONY: up down build run record parity parity-naive modern-build chain-graph chain-graph-check reset baseline shell

up:
	$(COMPOSE) up -d --build --wait

down:
	$(COMPOSE) down

build: up
	$(EXEC) tools/build.sh

shell:
	$(COMPOSE) exec estate bash

# ---------------------------------------------------------------- chains
define for_each_case
	@set -e; \
	chains="$(if $(CHAIN),$(CHAIN),$(CHAINS))"; \
	for chain in $$chains; do \
	  cases="$(if $(CASE),$(CASE),$$(ls fixtures/$$chain))"; \
	  for case in $$cases; do $(1); done; \
	done
endef

run:
	@test -n "$(CHAIN)" || { echo "usage: make run CHAIN=<posting|interest|trantype-update> [CASE=<case>]"; exit 2; }
	@case="$(if $(CASE),$(CASE),$$(ls fixtures/$(CHAIN) | head -1))"; \
	$(EXEC) $(PY) tools/parity/record.py $(CHAIN) $$case --out work/run/$(CHAIN)/$$case -v

record:
	$(call for_each_case,$(EXEC) $(PY) tools/parity/record.py $$chain $$case --out fixtures/$$chain/$$case/expected)

parity:
	@rm -f work/parity/report.md; mkdir -p work/parity; fail=0; \
	chains="$(if $(CHAIN),$(CHAIN),$(CHAINS))"; \
	java=""; case " $(TARGET) " in *" java "*) for chain in $$chains; do [ -f modern/$$chain/pom.xml ] && java="$$java $$chain"; done;; esac; \
	if [ -n "$$java" ]; then $(MAKE) --no-print-directory modern-build || exit 1; fi; \
	for chain in $$chains; do \
	  case " $(TARGET) " in *" java "*) case " $$java " in *" $$chain "*) ;; *) \
	    echo "$$chain [java]: no candidate, modern/$$chain/pom.xml missing"; \
	    case " $(TARGET) " in *" legacy "*) ;; *) fail=1; continue;; esac;; esac;; esac; \
	  cases="$(if $(CASE),$(CASE),$$(ls fixtures/$$chain))"; \
	  for case in $$cases; do \
	    case " $(TARGET) " in *" legacy "*) \
	    $(EXEC) $(PY) tools/parity/record.py $$chain $$case --out work/parity/$$chain/$$case/actual > /dev/null || { echo "$$chain/$$case: chain run failed"; fail=1; continue; }; \
	    $(EXEC) $(PY) tools/parity/compare.py $$chain $$case --actual work/parity/$$chain/$$case/actual --report work/parity/report.md || fail=1;; \
	    esac; \
	    case " $$java " in *" $$chain "*) \
	    $(EXEC) modern/run-candidate.sh $$chain $$case work/parity/$$chain/$$case/java > work/parity/$$chain-$$case-java.log 2>&1 || { echo "$$chain/$$case [java]: candidate run failed, see work/parity/$$chain-$$case-java.log"; fail=1; continue; }; \
	    $(EXEC) $(PY) tools/parity/compare.py $$chain $$case --actual work/parity/$$chain/$$case/java --report work/parity/report.md --label "java modern/$$chain" || fail=1;; \
	    esac; \
	  done; \
	done; \
	echo "report: work/parity/report.md"; exit $$fail

modern-build: up
	$(EXEC) $(MVN) -f modern/pom.xml verify

parity-naive:
	@test -n "$(CASE)" || { echo "usage: make parity-naive CASE=<interest case>"; exit 2; }
	@rm -f work/parity/report-naive.md; mkdir -p work/parity; \
	$(EXEC) $(PY) tools/parity/naive_ref.py interest $(CASE) --out work/parity/interest/$(CASE)/naive && \
	$(EXEC) $(PY) tools/parity/compare.py interest $(CASE) --actual work/parity/interest/$(CASE)/naive --report work/parity/report-naive.md --label "naive half-even reference"; \
	rc=$$?; echo "report: work/parity/report-naive.md"; exit $$rc

# ------------------------------------------------------------ discovery graph
chain-graph:
	$(PY) tools/chaingraph/gen_chain_graph.py --out docs/chain-graph.md

chain-graph-check:
	$(PY) tools/chaingraph/gen_chain_graph.py --check docs/chain-graph.md

baseline:
	$(EXEC) tools/build.sh

reset:
	rm -rf datasets loadlib work
	$(COMPOSE) down -v
