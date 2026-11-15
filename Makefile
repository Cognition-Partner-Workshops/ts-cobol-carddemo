# CardDemo legacy estate: build and run the batch chains locally.
# Everything executes inside the `estate` Docker Compose service (GnuCOBOL 3,
# Open COBOL ESQL) against the `db` service (PostgreSQL 15).
#
#   make up                     start the containers (builds the image once)
#   make build                  compile app/cbl + Db2 sub-application into ./loadlib
#   make run CHAIN=posting      run one chain on its first fixture case (or CASE=...)
#   make record [CHAIN=..] [CASE=..]   (re)write golden outputs under fixtures/*/*/expected
#   make parity [CHAIN=..] [CASE=..]   rerun chains and compare field by field -> work/parity/report.md
#   make parity-naive CASE=i02-tie-half-cent   half-even Python reference vs golden (fails on the tie)
#   make parity-java [CASE=..]  run the Java interest job (java/interest) and compare -> work/parity-java/report.md
#   make chain-graph / chain-graph-check       regenerate / verify docs/chain-graph.md
#   make rules / rules-check   regenerate / verify the interest rulebook outputs (board, register, expectations)
#   make board-e2e              Playwright suite for the rules review board (needs: pip install -r e2e/requirements.txt)
#   make design / design-check  regenerate / verify the interest design outputs (modernization/design)
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

.PHONY: up down build run record parity parity-naive parity-java chain-graph chain-graph-check reset baseline shell rules rules-check board-e2e design design-check

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
	for chain in $$chains; do \
	  cases="$(if $(CASE),$(CASE),$$(ls fixtures/$$chain))"; \
	  for case in $$cases; do \
	    $(EXEC) $(PY) tools/parity/record.py $$chain $$case --out work/parity/$$chain/$$case/actual > /dev/null || { echo "$$chain/$$case: chain run failed"; fail=1; continue; }; \
	    $(EXEC) $(PY) tools/parity/compare.py $$chain $$case --actual work/parity/$$chain/$$case/actual --report work/parity/report.md || fail=1; \
	  done; \
	done; \
	echo "report: work/parity/report.md"; exit $$fail

parity-naive:
	@test -n "$(CASE)" || { echo "usage: make parity-naive CASE=<interest case>"; exit 2; }
	@rm -f work/parity/report-naive.md; mkdir -p work/parity; \
	$(EXEC) $(PY) tools/parity/naive_ref.py interest $(CASE) --out work/parity/interest/$(CASE)/naive && \
	$(EXEC) $(PY) tools/parity/compare.py interest $(CASE) --actual work/parity/interest/$(CASE)/naive --report work/parity/report-naive.md --label "naive half-even reference"; \
	rc=$$?; echo "report: work/parity/report-naive.md"; exit $$rc

# ------------------------------------------- Java interest job (java/interest)
# Parity entry point of the converted interest stream (FI-6).  Needs JDK 21 and
# Maven on the host for the build, Docker for the run, python3 (3.10+) for the
# comparison.  The job runs in a JRE container that shares the network of the
# `db` service, where it keeps the Spring Batch job repository (schema batch).
# Output: work/parity-java/<case>/actual, report: work/parity-java/report.md.
INTEREST_JAR     = java/interest/target/interest.jar
INTEREST_JRE    ?= eclipse-temurin:21-jre
# PARM of INTCALC.STEP15 (app/jcl/INTCALC.jcl:22) and the clock the harness freezes (tools/runjcl/chains.json)
INTEREST_PARM   ?= 2022071800
INTEREST_CLOCK  ?= 2022-07-05 10:00:00.00

parity-java:
	$(COMPOSE) up -d --wait db
	mvn -q -f java/interest/pom.xml -DskipTests package
	@rm -f work/parity-java/report.md; mkdir -p work/parity-java; fail=0; \
	db=$$($(COMPOSE) ps -q db); \
	cases="$(if $(CASE),$(CASE),$$(ls fixtures/interest))"; \
	for case in $$cases; do \
	  rm -rf work/parity-java/$$case; \
	  docker run --rm --network container:$$db --user $(UID):$(GID) -v "$(CURDIR)":/estate -w /estate $(INTEREST_JRE) \
	    java -jar $(INTEREST_JAR) --interest.clock.frozen="$(INTEREST_CLOCK)" --logging.level.root=WARN \
	      parmDate=$(INTEREST_PARM) runId=$$(date +%s)-$$$$-$$case format=fixture \
	      inputDir=fixtures/interest/$$case/input outputDir=work/parity-java/$$case/actual \
	    || { echo "interest/$$case: java job ended with exit code $$?"; fail=1; }; \
	  $(PY) tools/parity/compare.py interest $$case --actual work/parity-java/$$case/actual --label java --report work/parity-java/report.md || fail=1; \
	done; \
	echo "report: work/parity-java/report.md"; exit $$fail

# ------------------------------------------------------------ discovery graph
chain-graph:
	$(PY) tools/chaingraph/gen_chain_graph.py --out docs/chain-graph.md

chain-graph-check:
	$(PY) tools/chaingraph/gen_chain_graph.py --check docs/chain-graph.md

# ------------------------------------------------- business rules (host python)
RULES_TOOLS = modernization/rules/interest/tools

rules:
	$(PY) $(RULES_TOOLS)/evaluate.py
	$(PY) $(RULES_TOOLS)/build_register.py
	$(PY) $(RULES_TOOLS)/build_board.py

rules-check:
	$(PY) $(RULES_TOOLS)/build_board.py --check
	$(PY) $(RULES_TOOLS)/build_register.py --check
	$(PY) $(RULES_TOOLS)/evaluate.py --check

board-e2e:
	$(PY) -m pytest e2e -v

baseline:
	$(EXEC) tools/build.sh

reset:
	rm -rf datasets loadlib work
	$(COMPOSE) down -v

# ------------------------------------------- target design (host python)
DESIGN_TOOLS = modernization/design/tools

design:
	$(PY) $(DESIGN_TOOLS)/build_design.py

design-check:
	$(PY) $(DESIGN_TOOLS)/build_design.py --check
