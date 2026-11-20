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

.PHONY: test-runjcl
test-runjcl:
	PYTHONDONTWRITEBYTECODE=1 $(PY) -m unittest discover -s tools/runjcl/tests -v

parity: test-runjcl
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

# ------------------------------------------------- strangler-fig implementation switch
# Each tools/runjcl/impl/<chain>.json names a chain's migrated programs and its default
# implementation (legacy until cut over).  IMPL=<impl> switches CHAIN (or every chain that
# has an impl file) for this invocation of run / parity / record; CARDDEMO_IMPL=interest=java,...
# selects per chain explicitly.  See tools/runjcl/candidate.py.
IMPL ?=
IMPL_CHAINS = $(or $(CHAIN),$(basename $(notdir $(wildcard tools/runjcl/impl/*.json))))
CARDDEMO_IMPL ?= $(if $(IMPL),$(shell echo $(foreach c,$(IMPL_CHAINS),$(c)=$(IMPL)) | tr ' ' ','))
ifneq ($(CARDDEMO_IMPL),)
EXEC = $(COMPOSE) exec -T -e CARDDEMO_IMPL=$(CARDDEMO_IMPL) estate
endif

# ------------------------------------------------------------ java/interest (INTCALC)
#   make java-interest-build      package java/interest/target/interest.jar (Maven in Docker)
#   make parity-java-interest [CASE=..]  interest chain on the Java implementation -> work/parity/report-java-interest.md
MAVEN_IMAGE ?= maven:3.9-eclipse-temurin-21
MAVEN_HOME_DIR ?= $(HOME)/.m2

.PHONY: java-interest-build parity-java-interest parity-java

java-interest-build:
	@mkdir -p "$(MAVEN_HOME_DIR)"
	docker run --rm -u $(UID):$(GID) -e HOME=/var/maven -e MAVEN_CONFIG=/var/maven/.m2 \
	  -v "$(MAVEN_HOME_DIR)":/var/maven/.m2 -v "$(CURDIR)/java/interest":/src -w /src \
	  $(MAVEN_IMAGE) mvn -B -q -Duser.home=/var/maven package

parity-java: parity-java-interest

parity-java-interest: java-interest-build
	@rm -f work/parity/report-java-interest.md; mkdir -p work/parity; fail=0; \
	for case in $(if $(CASE),$(CASE),$$(ls fixtures/interest)); do \
	  $(COMPOSE) exec -T -e CARDDEMO_IMPL=interest=java estate $(PY) tools/parity/record.py interest $$case --out work/parity/interest/$$case/java > /dev/null || { echo "interest/$$case: java run failed"; fail=1; continue; }; \
	  $(COMPOSE) exec -T estate $(PY) tools/parity/compare.py interest $$case --actual work/parity/interest/$$case/java --report work/parity/report-java-interest.md --label "java (java/interest)" || fail=1; \
	done; \
	echo "report: work/parity/report-java-interest.md"; exit $$fail

# ------------------------------------------------------- java/posting (POSTTRAN + TRANREPT)
#   make java-posting-build       package java/posting/target/posting.jar (Maven in Docker)
#   make parity-java-posting [CASE=..]  posting chain on the Java implementation -> work/parity/report-java-posting.md
.PHONY: java-posting-build parity-java-posting

java-posting-build:
	@mkdir -p "$(MAVEN_HOME_DIR)"
	docker run --rm -u $(UID):$(GID) -e HOME=/var/maven -e MAVEN_CONFIG=/var/maven/.m2 \
	  -v "$(MAVEN_HOME_DIR)":/var/maven/.m2 -v "$(CURDIR)/java/posting":/src -w /src \
	  $(MAVEN_IMAGE) mvn -B -q -Duser.home=/var/maven package

parity-java: parity-java-posting

parity-java-posting: java-posting-build
	@rm -f work/parity/report-java-posting.md; mkdir -p work/parity; fail=0; \
	for case in $(if $(CASE),$(CASE),$$(ls fixtures/posting)); do \
	  $(COMPOSE) exec -T -e CARDDEMO_IMPL=posting=java estate $(PY) tools/parity/record.py posting $$case --out work/parity/posting/$$case/java > /dev/null || { echo "posting/$$case: java run failed"; fail=1; continue; }; \
	  $(COMPOSE) exec -T estate $(PY) tools/parity/compare.py posting $$case --actual work/parity/posting/$$case/java --report work/parity/report-java-posting.md --label "java (java/posting)" || fail=1; \
	done; \
	echo "report: work/parity/report-java-posting.md"; exit $$fail
