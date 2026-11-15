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
#   make parity-java [CASE=..]  the Java interest gate: load, calculation, export through PostgreSQL, compare, RC1-RC5 -> work/parity-java/report.md
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

.PHONY: up down build run record parity parity-naive parity-java recon-java sidebyside-java chain-graph chain-graph-check reset baseline shell rules rules-check board-e2e design design-check

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

# The gate (ticket 09): every case runs the whole job under profile `tables` -
# loadExtractsStep -> calcInterestStep on the tables of schema carddemo -> exportDatasetsStep -
# and three things decide a case: the exit code of the job, tools/parity/compare.py on the
# exported files, and the reconciliation report (RC1-RC5 against interest_run, read only) on
# the run the job left in the database -> $(PARITY_JAVA_OUT)/<case>/recon.md.
# A case whose job does not end with 0 is FAILED: it is still compared (the comparator reports
# what is missing) and listed in the report; it is never skipped.
#   make parity-java ROUNDING=HALF_EVEN PARITY_JAVA_OUT=work/parity-java-half-even
# is the negative control (INT-Q01): interest.policy.rounding of the run, default as coded.
PARITY_JAVA_OUT ?= work/parity-java
ROUNDING        ?=

parity-java:
	$(COMPOSE) up -d --wait db
	mvn -q -f java/interest/pom.xml -DskipTests package
	@out=$(PARITY_JAVA_OUT); rm -f $$out/report.md; mkdir -p $$out; fail=0; summary=""; \
	db=$$($(COMPOSE) ps -q db); \
	jre="docker run --rm --network container:$$db --user $(UID):$(GID) -v $(CURDIR):/estate -w /estate $(INTEREST_JRE)"; \
	cases="$(if $(CASE),$(CASE),$$(ls fixtures/interest))"; \
	for case in $$cases; do \
	  rm -rf $$out/$$case; mkdir -p $$out/$$case; \
	  $$jre java -jar $(INTEREST_JAR) --spring.profiles.active=tables --interest.clock.frozen="$(INTEREST_CLOCK)" --logging.level.root=WARN \
	      $(if $(ROUNDING),--interest.policy.rounding=$(ROUNDING)) \
	      parmDate=$(INTEREST_PARM) runId=$$(date +%s)-$$$$-$$case format=fixture \
	      inputDir=fixtures/interest/$$case/input outputDir=$$out/$$case/actual; \
	  rc=$$?; job=PASS; cmp=PASS; recon=PASS; \
	  if [ $$rc -ne 0 ]; then echo "interest/$$case: java job ended with exit code $$rc"; job="FAIL (exit code $$rc)"; fi; \
	  $(PY) tools/parity/compare.py interest $$case --actual $$out/$$case/actual --label java --report $$out/report.md || cmp=FAIL; \
	  if [ $$rc -eq 0 ]; then \
	    $$jre $(RECON_CMD) report title=interest/$$case format=fixture inputDir=fixtures/interest/$$case/input reportFile=$$out/$$case/recon.md > /dev/null \
	      || { echo "interest/$$case: RC1-RC5 do not hold in interest_run, see $$out/$$case/recon.md"; recon=FAIL; }; \
	  else recon="FAIL (no finished run to check)"; fi; \
	  result=PASS; if [ "$$job" != PASS ] || [ $$cmp != PASS ] || [ "$$recon" != PASS ]; then result=FAIL; fail=1; fi; \
	  summary="$$summary| $$case | $$job | $$cmp | $$recon | $$result |\n"; \
	done; \
	{ printf '\n# Gate: make parity-java%s\n\n' "$(if $(ROUNDING), ROUNDING=$(ROUNDING))"; \
	  printf '| Case | Job (load, calculation, export) | Comparator | RC1-RC5 in interest_run | Result |\n|---|---|---|---|---|\n'; \
	  printf "$$summary"; } >> $$out/report.md; \
	printf "$$summary"; echo "report: $$out/report.md"; exit $$fail

# ------------------------------------- data-move kit (java/interest, ticket 10)
# recon-java: the reconciliation report (RC1-RC6, TL4) of one loaded and calculated case.
#   make recon-java CASE=<case>            stage the case (load, calculate, export), then report
#   make recon-java CASE=<case> STAGE=no   report on what the tables hold now (RUN=<run_id>, default the latest)
# The report itself only reads.  Output: work/recon-java/<case>/recon.md; the export of the
# tables: work/recon-java/<case>/actual.  Exit 1 when a line of RC1-RC5 is DIFFERENT or NOT AVAILABLE.
# sidebyside-java: field-level differences of two output directories in the recorded format
# (tools/parity/compare.py, unchanged), every difference with its class.
#   make sidebyside-java LEGACY=<dir> TARGET=<dir> [TS=format|exact]
# Output: work/sidebyside-java/report.md (and compare.md, the comparator's own report).
RECON_CMD = java -cp $(INTEREST_JAR) -Dloader.main=com.carddemo.interest.recon.ReconCommand org.springframework.boot.loader.launch.PropertiesLauncher
STAGE    ?= yes
RUN      ?=
TS       ?= format
LEGACY   ?=
TARGET   ?=

recon-java:
	@test -n "$(CASE)" || { echo "usage: make recon-java CASE=<interest case> [STAGE=no [RUN=<run_id>]]"; exit 2; }
	$(COMPOSE) up -d --wait db
	mvn -q -f java/interest/pom.xml -DskipTests package
	@db=$$($(COMPOSE) ps -q db); out=work/recon-java/$(CASE); in=fixtures/interest/$(CASE)/input; \
	jre="docker run --rm --network container:$$db --user $(UID):$(GID) -v $(CURDIR):/estate -w /estate $(INTEREST_JRE)"; \
	if [ "$(STAGE)" = yes ]; then \
	  rm -rf $$out; mkdir -p $$out; \
	  $$jre java -jar $(INTEREST_JAR) --interest.clock.frozen="$(INTEREST_CLOCK)" --logging.level.root=WARN \
	    parmDate=$(INTEREST_PARM) runId=$$(date +%s)-$$$$-$(CASE) format=fixture inputDir=$$in outputDir=$$out/calculated \
	  && $$jre $(RECON_CMD) stage parmDate=$(INTEREST_PARM) format=fixture inputDir=$$in calculatedDir=$$out/calculated outputDir=$$out/actual \
	  || { echo "interest/$(CASE): the case could not be staged"; exit 2; }; \
	fi; \
	mkdir -p $$out; \
	$$jre $(RECON_CMD) report title=interest/$(CASE) format=fixture inputDir=$$in reportFile=$$out/recon.md $(if $(RUN),runId=$(RUN)); \
	rc=$$?; echo "report: $$out/recon.md"; exit $$rc

sidebyside-java:
	@test -n "$(LEGACY)" -a -n "$(TARGET)" || { echo "usage: make sidebyside-java LEGACY=<dir> TARGET=<dir> [TS=format|exact]"; exit 2; }
	@test -d "$(LEGACY)" -a -d "$(TARGET)" || { echo "sidebyside-java: LEGACY and TARGET must be directories"; exit 2; }
	mvn -q -f java/interest/pom.xml -DskipTests package
	@out=work/sidebyside-java; rm -rf $$out; mkdir -p $$out/legacy/interest/run $$out/target; \
	cp -r "$(LEGACY)" $$out/legacy/interest/run/expected; cp -r "$(TARGET)" $$out/target/actual; \
	$(PY) -c 'import sys; from pathlib import Path; from tools.parity import compare; out = Path(sys.argv[1]); compare.FIXTURES = out / "legacy"; diffs, checked = compare.compare_case("interest", "run", out / "target" / "actual"); compare.write_report(out / "compare.md", "interest", "side by side", "target", diffs, checked); esc = lambda v: str(v).replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n"); head = ["#checked\t%d datasets, %d records, %d decoded fields, %d SYSOUT files, return codes" % (checked["datasets"], checked["records"], checked["fields"], checked["sysout"])] if checked["datasets"] else ["#nothing"]; (out / "differences.tsv").write_text("\n".join(head + ["\t".join(esc(d[k]) for k in ("where", "field", "expected", "actual")) for d in diffs]) + "\n", encoding="latin-1")' $$out \
	|| { echo "sidebyside-java: tools/parity/compare.py could not compare the two directories"; exit 2; }; \
	docker run --rm --user $(UID):$(GID) -v $(CURDIR):/estate -w /estate $(INTEREST_JRE) \
	  $(RECON_CMD) classify legacy=$$out/legacy/interest/run/expected target=$$out/target/actual ts=$(TS) \
	    legacyName="$(LEGACY)" targetName="$(TARGET)" differences=$$out/differences.tsv reportFile=$$out/report.md; \
	rc=$$?; echo "report: $$out/report.md (legacy $(LEGACY), target $(TARGET))"; exit $$rc

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
