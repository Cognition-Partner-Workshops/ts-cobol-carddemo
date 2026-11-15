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
# Parity entry point of the converted interest stream (FI-6) and its gate (INT-T09).  Needs JDK 21
# and Maven on the host for the build, Docker for the run, python3 (3.10+) for the comparison.
# The job runs in a JRE container that shares the network of the `db` service, with the profile
# `database`: loadExtractsStep -> calcInterestStep -> exportDatasetsStep through schema carddemo
# (db/postgres/02-interest.sql; applied here when the volume of the db service is older than it).
# Per case: the job, then the database assertions RC1, RC2 and RC5 on the run's row of interest_run
# (the reconciliation report of the data move kit, <out>/<case>/recon.md), then the comparator.
# A case fails when the job does not end with exit code 0, when a reconciliation figure is
# DIFFERENT, when a line of RC1, RC2 or RC5 is not EQUAL (NOT AVAILABLE is not equal) or when the
# comparator finds a difference; it is never skipped.
# Output: work/parity-java/<case>/actual, report: work/parity-java/report.md.
#   INTEREST_ARGS="--interest.policy.rounding=HALF_EVEN" PARITY_JAVA_OUT=work/parity-java-half-even
#       the negative control: another policy value, written somewhere else.  Must fail on
#       i02-tie-half-cent only, as `make parity-naive` does.
INTEREST_JAR     = java/interest/target/interest.jar
INTEREST_JRE    ?= eclipse-temurin:21-jre
# PARM of INTCALC.STEP15 (app/jcl/INTCALC.jcl:22) and the clock the harness freezes (tools/runjcl/chains.json)
INTEREST_PARM   ?= 2022071800
INTEREST_CLOCK  ?= 2022-07-05 10:00:00.00
INTEREST_ARGS   ?=
PARITY_JAVA_OUT ?= work/parity-java

parity-java:
	$(COMPOSE) up -d --wait db
	@$(COMPOSE) exec -T db psql -U carddemo -d carddemo -Atc "select to_regclass('carddemo.interest_run')" | grep -q interest_run \
	  || $(COMPOSE) exec -T db psql -q -v ON_ERROR_STOP=1 -U carddemo -d carddemo -f /docker-entrypoint-initdb.d/02-interest.sql
	mvn -q -f java/interest/pom.xml -DskipTests package
	@out=$(PARITY_JAVA_OUT); rm -f $$out/report.md; mkdir -p $$out; fail=0; total=0; passed=0; \
	db=$$($(COMPOSE) ps -q db); \
	jre="docker run --rm --network container:$$db --user $(UID):$(GID) -v $(CURDIR):/estate -w /estate $(INTEREST_JRE)"; \
	cases="$(if $(CASE),$(CASE),$$(ls fixtures/interest))"; \
	for case in $$cases; do \
	  total=$$((total + 1)); ok=1; rm -rf $$out/$$case $$out/$$case.load-rejects.txt; \
	  $$jre java -jar $(INTEREST_JAR) --spring.profiles.active=database \
	      --interest.clock.frozen="$(INTEREST_CLOCK)" --logging.level.root=WARN $(INTEREST_ARGS) \
	      parmDate=$(INTEREST_PARM) runId=$$(date +%s)-$$$$-$$case format=fixture \
	      inputDir=fixtures/interest/$$case/input outputDir=$$out/$$case/actual \
	    || { echo "interest/$$case: java job ended with exit code $$?"; ok=0; }; \
	  if [ $$ok = 1 ]; then \
	    $$jre java -cp $(INTEREST_JAR) $(RECON_MAIN) recon title=$$case inputDir=fixtures/interest/$$case/input \
	        outputDir=$$out/$$case/actual format=fixture report=$$out/$$case/recon.md \
	      && for rc in RC1 RC2 RC5; do \
	           grep -q "^| $$rc .*| \*\*EQUAL\*\* |" $$out/$$case/recon.md && ! grep "^| $$rc " $$out/$$case/recon.md | grep -qv "| \*\*EQUAL\*\* |" || exit 1; \
	         done \
	      || { echo "interest/$$case: database assertions RC1, RC2, RC5 do not hold, see $$out/$$case/recon.md"; ok=0; }; \
	  fi; \
	  $(PY) tools/parity/compare.py interest $$case --actual $$out/$$case/actual --label java --report $$out/report.md || ok=0; \
	  if [ $$ok = 1 ]; then passed=$$((passed + 1)); else fail=1; fi; \
	done; \
	echo "parity-java: $$passed of $$total cases PASS"; echo "report: $$out/report.md"; exit $$fail

# ------------------------------------------- Data move kit of the interest stream (INT-T10)
#   make recon-java CASE=<case> [LOAD=1] [RUN=<interest_run.run_id>] [OUT=<output dir>] [FORMAT=fixture|host]
#       reconciliation report RC1-RC6 with the profile of TL4 -> work/recon-java/<case>.md
#       Reads the unloaded files of fixtures/interest/<case>/input, interest_run and the tables (read-only)
#       and the exported files of OUT (default work/parity-java/<case>/actual).  LOAD=1 first loads the
#       four tables from the case's input (trial load, TL3); RUN names another run than the newest.
#       Exit 1 when a figure is DIFFERENT.  NOT AVAILABLE is never counted as equal.
#   make sidebyside-java CASE=<case> [LEGACY=<dir>] [TARGET=<dir>] [TS=format|exact]
#       two output directories field by field via tools/parity/compare.py, every difference with its
#       class (defect, decision id, gap id) -> work/sidebyside-java/<case>/report.md
#       LEGACY defaults to fixtures/interest/<case>/expected, TARGET to work/parity-java/<case>/actual;
#       with both given, CASE is only the name of the comparison.  TS is the timestamp rule of SS3.
#       Exit 1 when a difference is a defect.
.PHONY: recon-java sidebyside-java
RECON_MAIN  = -Dloader.main=com.carddemo.interest.recon.DataMoveKit org.springframework.boot.loader.launch.PropertiesLauncher
RECON_JAVA  = docker run --rm --user $(UID):$(GID) -v "$(CURDIR)":/estate -w /estate

recon-java:
	@test -n "$(CASE)" || { echo "usage: make recon-java CASE=<case> [LOAD=1] [RUN=<run_id>] [OUT=<dir>] [FORMAT=fixture|host]"; exit 2; }
	$(COMPOSE) up -d --wait db
	mvn -q -f java/interest/pom.xml -DskipTests package
	@db=$$($(COMPOSE) ps -q db); \
	if [ -n "$(LOAD)" ]; then \
	  $(RECON_JAVA) --network container:$$db $(INTEREST_JRE) java -cp $(INTEREST_JAR) $(RECON_MAIN) trial-load \
	    inputDir=fixtures/interest/$(CASE)/input rejects=work/recon-java/$(CASE).load-rejects.txt \
	    format=$(or $(FORMAT),fixture) parmDate=$(INTEREST_PARM) || exit 1; \
	fi; \
	$(RECON_JAVA) --network container:$$db $(INTEREST_JRE) java -cp $(INTEREST_JAR) $(RECON_MAIN) recon \
	  title=$(CASE) inputDir=fixtures/interest/$(CASE)/input outputDir=$(or $(OUT),work/parity-java/$(CASE)/actual) \
	  format=$(or $(FORMAT),fixture) $(if $(RUN),run=$(RUN)) report=work/recon-java/$(CASE).md

sidebyside-java:
	@test -n "$(CASE)" || { echo "usage: make sidebyside-java CASE=<case> [LEGACY=<dir>] [TARGET=<dir>] [TS=format|exact]"; exit 2; }
	mvn -q -f java/interest/pom.xml -DskipTests package
	@work=work/sidebyside-java/$(CASE); legacy=$(or $(LEGACY),fixtures/interest/$(CASE)/expected); \
	target=$(or $(TARGET),work/parity-java/$(CASE)/actual); ts=$(or $(TS),format); \
	$(RECON_JAVA) $(INTEREST_JRE) java -cp $(INTEREST_JAR) $(RECON_MAIN) sidebyside-prepare \
	  legacy=$$legacy target=$$target work=$$work name=$(CASE) timestamps=$$ts || exit 2; \
	PYTHONDONTWRITEBYTECODE=1 $(PY) -c "import pathlib, sys; sys.path.insert(0, '.'); from tools.parity import compare; compare.FIXTURES = pathlib.Path(sys.argv[1]); del sys.argv[1]; sys.exit(compare.main())" \
	  $$work/legacy interest $(CASE) --actual $$work/target --label "side by side, timestamps $$ts" --report $$work/compare.md; \
	$(RECON_JAVA) $(INTEREST_JRE) java -cp $(INTEREST_JAR) $(RECON_MAIN) sidebyside-report \
	  compare=$$work/compare.md prepared=$$work/prepared.txt report=$$work/report.md name=$(CASE) \
	  legacy=$$legacy target=$$target timestamps=$$ts

# ------------------------------------------- Data move kit of the interest stream (INT-T10)
#   make recon-java CASE=<case> [LOAD=1] [RUN=<interest_run.run_id>] [OUT=<output dir>] [FORMAT=fixture|host]
#       reconciliation report RC1-RC6 with the profile of TL4 -> work/recon-java/<case>.md
#       Reads the unloaded files of fixtures/interest/<case>/input, interest_run and the tables (read-only)
#       and the exported files of OUT (default work/parity-java/<case>/actual).  LOAD=1 first loads the
#       four tables from the case's input (trial load, TL3); RUN names another run than the newest.
#       Exit 1 when a figure is DIFFERENT.  NOT AVAILABLE is never counted as equal.
#   make sidebyside-java CASE=<case> [LEGACY=<dir>] [TARGET=<dir>] [TS=format|exact]
#       two output directories field by field via tools/parity/compare.py, every difference with its
#       class (defect, decision id, gap id) -> work/sidebyside-java/<case>/report.md
#       LEGACY defaults to fixtures/interest/<case>/expected, TARGET to work/parity-java/<case>/actual;
#       with both given, CASE is only the name of the comparison.  TS is the timestamp rule of SS3.
#       Exit 1 when a difference is a defect.
.PHONY: recon-java sidebyside-java
RECON_MAIN  = -Dloader.main=com.carddemo.interest.recon.DataMoveKit org.springframework.boot.loader.launch.PropertiesLauncher
RECON_JAVA  = docker run --rm --user $(UID):$(GID) -v "$(CURDIR)":/estate -w /estate

recon-java:
	@test -n "$(CASE)" || { echo "usage: make recon-java CASE=<case> [LOAD=1] [RUN=<run_id>] [OUT=<dir>] [FORMAT=fixture|host]"; exit 2; }
	$(COMPOSE) up -d --wait db
	mvn -q -f java/interest/pom.xml -DskipTests package
	@db=$$($(COMPOSE) ps -q db); \
	if [ -n "$(LOAD)" ]; then \
	  $(RECON_JAVA) --network container:$$db $(INTEREST_JRE) java -cp $(INTEREST_JAR) $(RECON_MAIN) trial-load \
	    inputDir=fixtures/interest/$(CASE)/input rejects=work/recon-java/$(CASE).load-rejects.txt \
	    format=$(or $(FORMAT),fixture) parmDate=$(INTEREST_PARM) || exit 1; \
	fi; \
	$(RECON_JAVA) --network container:$$db $(INTEREST_JRE) java -cp $(INTEREST_JAR) $(RECON_MAIN) recon \
	  title=$(CASE) inputDir=fixtures/interest/$(CASE)/input outputDir=$(or $(OUT),work/parity-java/$(CASE)/actual) \
	  format=$(or $(FORMAT),fixture) $(if $(RUN),run=$(RUN)) report=work/recon-java/$(CASE).md

sidebyside-java:
	@test -n "$(CASE)" || { echo "usage: make sidebyside-java CASE=<case> [LEGACY=<dir>] [TARGET=<dir>] [TS=format|exact]"; exit 2; }
	mvn -q -f java/interest/pom.xml -DskipTests package
	@work=work/sidebyside-java/$(CASE); legacy=$(or $(LEGACY),fixtures/interest/$(CASE)/expected); \
	target=$(or $(TARGET),work/parity-java/$(CASE)/actual); ts=$(or $(TS),format); \
	$(RECON_JAVA) $(INTEREST_JRE) java -cp $(INTEREST_JAR) $(RECON_MAIN) sidebyside-prepare \
	  legacy=$$legacy target=$$target work=$$work name=$(CASE) timestamps=$$ts || exit 2; \
	PYTHONDONTWRITEBYTECODE=1 $(PY) -c "import pathlib, sys; sys.path.insert(0, '.'); from tools.parity import compare; compare.FIXTURES = pathlib.Path(sys.argv[1]); del sys.argv[1]; sys.exit(compare.main())" \
	  $$work/legacy interest $(CASE) --actual $$work/target --label "side by side, timestamps $$ts" --report $$work/compare.md; \
	$(RECON_JAVA) $(INTEREST_JRE) java -cp $(INTEREST_JAR) $(RECON_MAIN) sidebyside-report \
	  compare=$$work/compare.md prepared=$$work/prepared.txt report=$$work/report.md name=$(CASE) \
	  legacy=$$legacy target=$$target timestamps=$$ts

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
