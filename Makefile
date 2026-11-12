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
TARGET  ?=
MAVEN_ARGS ?=
INTEREST_PARM_DATE ?= $(shell sed -n "s/.*PARM='\([^']*\)'.*/\1/p" app/jcl/INTCALC.jcl)
TRANTYPE_DYNAMIC_PARMS ?= $(shell sed -n "s/.*PARMS('\([^']*\)').*/\1/p" app/app-transaction-type-db2/jcl/MNTTRDYN.jcl)
PY       = python3

.PHONY: up down build run record parity parity-naive chain-graph chain-graph-check reset baseline shell

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
	  target="$(TARGET)"; [ -n "$$target" ] || { if [ -d modern/$$chain ]; then target=java; else target=legacy; fi; }; \
	  if [ "$$target" = java ]; then $(COMPOSE) exec -T java mvn $(MAVEN_ARGS) -Dmaven.repo.local=/estate/work/maven -f modern/pom.xml -pl $$chain -am package -DskipTests > work/parity/build-$$chain.log 2>&1 || { cat work/parity/build-$$chain.log; exit 1; }; fi; \
	  cases="$(if $(CASE),$(CASE),$$(ls fixtures/$$chain))"; \
	  for case in $$cases; do \
	    if [ "$$target" = java ]; then \
	      parm=""; if [ "$$chain" = interest ]; then parm="$(INTEREST_PARM_DATE)"; fi; \
	      if [ "$$chain" = trantype-dynamic ]; then parm="$(TRANTYPE_DYNAMIC_PARMS)"; fi; \
	      $(COMPOSE) exec -T java java -jar modern/$$chain/target/$$chain-0.1.0.jar fixtures/$$chain/$$case/input work/parity/$$chain/$$case/actual $$parm > work/parity/run-$$chain-$$case.log 2>&1 || { cat work/parity/run-$$chain-$$case.log; fail=1; continue; }; \
	      label=java; \
	    else \
	      $(EXEC) $(PY) tools/parity/record.py $$chain $$case --out work/parity/$$chain/$$case/actual > /dev/null || { echo "$$chain/$$case: chain run failed"; fail=1; continue; }; label="legacy rerun"; \
	    fi; \
	    $(EXEC) $(PY) tools/parity/compare.py $$chain $$case --actual work/parity/$$chain/$$case/actual --report work/parity/report.md --label "$$label" || fail=1; \
	  done; \
	done; \
	echo "report: work/parity/report.md"; exit $$fail

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
