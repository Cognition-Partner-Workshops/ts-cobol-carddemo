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
# Parity entry point of the converted interest stream (FI-6) and its gate (ticket 09).
# Needs JDK 21 and Maven on the host for the build, Docker for the run, python3 (3.10+)
# for the comparison.  The job runs in a JRE container that shares the network of the
# `db` service.  With the profile `tables` (the default) one launch per case runs
# loadExtractsStep, calcInterestStep and exportDatasetsStep through PostgreSQL; after
# each case the gate checks RC1, RC2 and RC5 (modernization/design/strategy-interest.md)
# in carddemo.interest_run against the input files and the exported files.
# Output: work/parity-java/<case>/actual, report: work/parity-java/report.md.
#   INTEREST_PROFILE=default   calcInterestStep alone on the file adapters (no database assertions)
#   INTEREST_OPTS=--interest.policy.rounding=HALF_EVEN INTEREST_OUT=work/parity-java-half-even
#                              the negative control: fails on i02-tie-half-cent only
INTEREST_JAR     = java/interest/target/interest.jar
INTEREST_JRE    ?= eclipse-temurin:21-jre
# PARM of INTCALC.STEP15 (app/jcl/INTCALC.jcl:22) and the clock the harness freezes (tools/runjcl/chains.json)
INTEREST_PARM   ?= 2022071800
INTEREST_CLOCK  ?= 2022-07-05 10:00:00.00
INTEREST_PROFILE ?= tables
INTEREST_OPTS   ?=
INTEREST_OUT    ?= work/parity-java
INTEREST_PSQL    = $(COMPOSE) exec -T db psql -U carddemo -d carddemo -v ON_ERROR_STOP=1

# The file side of RC1, RC2 and RC5, decoded with the estate's copybook decoder
# (tools/parity/copybook.py), printed as psql variables.  argv: input directory, actual directory.
define INTEREST_RC_FILES
import sys
from decimal import Decimal
from pathlib import Path
sys.path.insert(0, "tools/parity")
from copybook import decode_record
inputs, actual = Path(sys.argv[1]), Path(sys.argv[2])
def records(path, layout, lrecl):
    if not path.exists():
        return []
    lines = path.read_text(encoding="latin-1").splitlines()
    return [decode_record(layout, line.ljust(lrecl).encode("latin-1")) for line in lines]
def total(rows, field):
    return sum((Decimal(row[field]) for row in rows), Decimal(0))
accounts = records(inputs / "AWS.M2.CARDDEMO.ACCTDATA.PS.txt", "CVACT01Y", 300)
cards = records(inputs / "AWS.M2.CARDDEMO.CARDXREF.PS.txt", "CVACT03Y", 50)
balances = records(inputs / "AWS.M2.CARDDEMO.TCATBALF.PS.txt", "CVTRA01Y", 50)
rates = records(inputs / "AWS.M2.CARDDEMO.DISCGRP.PS.txt", "CVTRA02Y", 50)
written = records(actual / "datasets" / "AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt", "CVTRA05Y", 350)
after = records(actual / "datasets" / "AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt", "CVACT01Y", 300)
last = max((int(row["TRANCAT-ACCT-ID"]) for row in balances), default=None)
lastCards = {str(row["XREF-CARD-NUM"]) for row in cards if int(row["XREF-ACCT-ID"]) == last}
figures = {
    "n_acct": len(accounts), "n_xref": len(cards), "n_bal": len(balances), "n_rate": len(rates),
    "s_bal": total(accounts, "ACCT-CURR-BAL"), "s_limit": total(accounts, "ACCT-CREDIT-LIMIT"),
    "s_cash": total(accounts, "ACCT-CASH-CREDIT-LIMIT"), "s_credit": total(accounts, "ACCT-CURR-CYC-CREDIT"),
    "s_debit": total(accounts, "ACCT-CURR-CYC-DEBIT"), "s_tcat": total(balances, "TRAN-CAT-BAL"),
    "s_rate": total(rates, "DIS-INT-RATE"), "h_acct": total(accounts, "ACCT-ID"),
    "h_xref": total(cards, "XREF-ACCT-ID"), "h_tcat": total(balances, "TRANCAT-ACCT-ID"),
    "x_tran": len(written), "x_amt": total(written, "TRAN-AMT"), "x_bal": total(after, "ACCT-CURR-BAL"),
    "x_last": total([row for row in written if str(row["TRAN-CARD-NUM"]) in lastCards], "TRAN-AMT"),
}
for name, value in figures.items():
    print(f"\\set {name} {value}")
endef

# RC1, RC2 and RC5 for the interest_run row the launch created (run_id above :before).
# One line per assertion: name|t or name|f.
define INTEREST_RC_SQL
WITH r AS (SELECT * FROM carddemo.interest_run WHERE run_id > :before)
SELECT 'one interest_run row of this launch, status EXPORTED',
       coalesce((SELECT count(*) = 1 AND bool_and(status = 'EXPORTED') FROM r), false)
UNION ALL SELECT c.name, coalesce(c.holds, false) FROM r CROSS JOIN LATERAL (VALUES
  ('RC1 loaded_account_count = records of ACCTDATA.PS (' || :n_acct || ')', r.loaded_account_count = :n_acct),
  ('RC1 loaded_card_xref_count = records of CARDXREF.PS (' || :n_xref || ')', r.loaded_card_xref_count = :n_xref),
  ('RC1 loaded_tran_cat_balance_count = records of TCATBALF.PS (' || :n_bal || ')', r.loaded_tran_cat_balance_count = :n_bal),
  ('RC1 loaded_disclosure_group_count = records of DISCGRP.PS (' || :n_rate || ')', r.loaded_disclosure_group_count = :n_rate),
  ('RC1 rows of account, card_xref, tran_cat_balance, disclosure_group = the four loaded counts',
     (SELECT count(*) FROM carddemo.account) = r.loaded_account_count
     AND (SELECT count(*) FROM carddemo.card_xref) = r.loaded_card_xref_count
     AND (SELECT count(*) FROM carddemo.tran_cat_balance) = r.loaded_tran_cat_balance_count
     AND (SELECT count(*) FROM carddemo.disclosure_group) = r.loaded_disclosure_group_count),
  ('RC2 loaded_acct_curr_bal_sum = sum of ACCT-CURR-BAL in the file (' || :s_bal || ')', r.loaded_acct_curr_bal_sum = :s_bal),
  ('RC2 loaded_acct_credit_limit_sum = sum of ACCT-CREDIT-LIMIT in the file (' || :s_limit || ')', r.loaded_acct_credit_limit_sum = :s_limit),
  ('RC2 loaded_acct_cash_credit_limit_sum = sum of ACCT-CASH-CREDIT-LIMIT in the file (' || :s_cash || ')', r.loaded_acct_cash_credit_limit_sum = :s_cash),
  ('RC2 loaded_acct_curr_cyc_credit_sum = sum of ACCT-CURR-CYC-CREDIT in the file (' || :s_credit || ')', r.loaded_acct_curr_cyc_credit_sum = :s_credit),
  ('RC2 loaded_acct_curr_cyc_debit_sum = sum of ACCT-CURR-CYC-DEBIT in the file (' || :s_debit || ')', r.loaded_acct_curr_cyc_debit_sum = :s_debit),
  ('RC2 loaded_tran_cat_bal_sum = sum of TRAN-CAT-BAL in the file (' || :s_tcat || ')', r.loaded_tran_cat_bal_sum = :s_tcat),
  ('RC2 loaded_dis_int_rate_sum = sum of DIS-INT-RATE in the file (' || :s_rate || ')', r.loaded_dis_int_rate_sum = :s_rate),
  ('RC2 loaded_acct_id_sum = hash total of ACCT-ID (' || :h_acct || ')', r.loaded_acct_id_sum = :h_acct),
  ('RC2 loaded_xref_acct_id_sum = hash total of XREF-ACCT-ID (' || :h_xref || ')', r.loaded_xref_acct_id_sum = :h_xref),
  ('RC2 loaded_trancat_acct_id_sum = hash total of TRANCAT-ACCT-ID (' || :h_tcat || ')', r.loaded_trancat_acct_id_sum = :h_tcat),
  ('RC5 written_transaction_count = rows of interest_transaction = records of the exported SYSTRAN (' || :x_tran || ')',
     r.written_transaction_count = :x_tran
     AND (SELECT count(*) FROM carddemo.interest_transaction t WHERE t.run_id = r.run_id) = :x_tran),
  ('RC5 written_tran_amt_sum = sum of TRAN-AMT in the exported SYSTRAN (' || :x_amt || ')', r.written_tran_amt_sum = :x_amt),
  ('RC5 written_acct_curr_bal_sum = sum of ACCT-CURR-BAL in the exported ACCTDATA (' || :x_bal || ')', r.written_acct_curr_bal_sum = :x_bal),
  ('RC5 sum of TRAN-AMT = change of the sum of ACCT-CURR-BAL (' || :x_bal - :s_bal || ') + interest of the last account, not added: INT-Q04 (' || :x_last || ')',
     r.written_tran_amt_sum = (r.written_acct_curr_bal_sum - r.loaded_acct_curr_bal_sum) + :x_last)
  ) AS c(name, holds);
endef
export INTEREST_RC_FILES INTEREST_RC_SQL

parity-java:
	$(COMPOSE) up -d --wait db
	mvn -q -f java/interest/pom.xml -DskipTests package
	@$(INTEREST_PSQL) -Atc "select to_regclass('carddemo.interest_run') is not null" | grep -qx t \
	  || { echo "db volume has no interest tables: applying db/postgres/02-interest.sql"; $(INTEREST_PSQL) -q < db/postgres/02-interest.sql; }
	@out=$(INTEREST_OUT); rm -f $$out/report.md; mkdir -p $$out; fail=0; \
	db=$$($(COMPOSE) ps -q db); \
	summary="| Case | Java job exit code | Comparator | interest_run (RC1, RC2, RC5) |\n|---|---|---|---|\n"; \
	cases="$(if $(CASE),$(CASE),$$(ls fixtures/interest))"; \
	for case in $$cases; do \
	  rm -rf $$out/$$case; \
	  before=0; if [ "$(INTEREST_PROFILE)" = tables ]; then before=$$($(INTEREST_PSQL) -Atc "select coalesce(max(run_id), 0) from carddemo.interest_run"); fi; \
	  docker run --rm --network container:$$db --user $(UID):$(GID) -v "$(CURDIR)":/estate -w /estate $(INTEREST_JRE) \
	    java -jar $(INTEREST_JAR) --spring.profiles.active=$(INTEREST_PROFILE) --interest.clock.frozen="$(INTEREST_CLOCK)" --logging.level.root=WARN $(INTEREST_OPTS) \
	      parmDate=$(INTEREST_PARM) runId=$$(date +%s)-$$$$-$$case format=fixture \
	      inputDir=fixtures/interest/$$case/input outputDir=$$out/$$case/actual; \
	  jobrc=$$?; \
	  if [ $$jobrc -ne 0 ]; then echo "interest/$$case: java job ended with exit code $$jobrc"; fail=1; fi; \
	  cmp=PASS; $(PY) tools/parity/compare.py interest $$case --actual $$out/$$case/actual --label java --report $$out/report.md || { cmp=FAIL; fail=1; }; \
	  rc="not checked (profile $(INTEREST_PROFILE))"; \
	  if [ "$(INTEREST_PROFILE)" = tables ]; then \
	    checks=$$({ echo "\\set before $$before"; $(PY) -c "$$INTEREST_RC_FILES" fixtures/interest/$$case/input $$out/$$case/actual; echo "$$INTEREST_RC_SQL"; } | $(INTEREST_PSQL) -At 2>&1); \
	    if [ $$? -eq 0 ] && [ -n "$$checks" ] && ! echo "$$checks" | grep -qv '|t$$'; then rc=PASS; else rc=FAIL; fail=1; fi; \
	    echo "interest/$$case [java] interest_run: $$rc ($$(echo "$$checks" | grep -c '|t$$') of $$(echo "$$checks" | grep -c .) assertions of RC1, RC2, RC5 hold)"; \
	    { echo; echo "### interest/$$case [java] interest_run: $$rc"; echo; echo '```'; echo "$$checks"; echo '```'; } >> $$out/report.md; \
	  fi; \
	  summary="$$summary| $$case | $$jobrc | $$cmp | $$rc |\n"; \
	done; \
	{ echo; echo "## Gate: $$( [ $$fail -eq 0 ] && echo PASS || echo FAIL )"; echo; \
	  echo "profile \`$(INTEREST_PROFILE)\`, options \`$(INTEREST_OPTS)\`, PARM $(INTEREST_PARM), clock $(INTEREST_CLOCK). A case whose job did not end with exit code 0 is a failed case."; \
	  echo; printf "$$summary"; } >> $$out/report.md; \
	echo "report: $$out/report.md"; exit $$fail

# ------------------------------- data-move kit of the interest stream (ticket 10)
# make recon-java [CASE=..]       reconciliation report of a load and a run (strategy-interest.md RC1-RC6, TL4)
#                                 -> work/recon-java/<case>.md and work/recon-java/report.md
#   RECON_LOAD=yes (default)      loads and calculates the case first: make parity-java CASE=<case> INTEREST_OUT=work/recon-java/run
#   RECON_LOAD=no                 reports on what the tables of the db service hold now (the last case that ran), with
#                                 RECON_INPUT=<unloaded files> RECON_ACTUAL=<exported files> [RECON_RUN=<interest_run.run_id>]
# make sidebyside-java [CASE=..] [TS=format|exact]
#                                 compares a legacy run's files with a target run's files through tools/parity/compare.py
#                                 and gives every difference a class -> work/sidebyside-java/<name>/report.md
#   LEGACY=<dir> TARGET=<dir> [NAME=<label>]   two output directories in the recorded format (FI-2); without them
#                                 fixtures/interest/<case>/expected against work/parity-java/<case>/actual
# The report reads; it writes nothing to the database.  Exit code 0 only if every report is EQUAL.
.PHONY: recon-java sidebyside-java
RECON_OUT       ?= work/recon-java
RECON_LOAD      ?= yes
RECON_INPUT     ?=
RECON_ACTUAL    ?=
RECON_RUN       ?=
SIDEBYSIDE_OUT  ?= work/sidebyside-java
TS              ?= format
LEGACY          ?=
TARGET          ?=
NAME            ?=
KIT_LAUNCHER     = org.springframework.boot.loader.launch.PropertiesLauncher
KIT_JAVA         = docker run --rm --user $(UID):$(GID) -v "$(CURDIR)":/estate -w /estate

recon-java:
	$(COMPOSE) up -d --wait db
	mvn -q -f java/interest/pom.xml -DskipTests package
	@out=$(RECON_OUT); mkdir -p $$out; rm -f $$out/report.md; fail=0; \
	db=$$($(COMPOSE) ps -q db); \
	cases="$(if $(CASE),$(CASE),$$(ls fixtures/interest))"; \
	for case in $$cases; do \
	  input="$(if $(RECON_INPUT),$(RECON_INPUT),fixtures/interest/$$case/input)"; \
	  if [ "$(RECON_LOAD)" = yes ]; then \
	    actual=$$out/run/$$case/actual; \
	    $(MAKE) --no-print-directory parity-java CASE=$$case INTEREST_OUT=$$out/run > $$out/$$case.load.log 2>&1 \
	      || echo "interest/$$case: the load and the calculation did not pass the gate (see $$out/$$case.load.log)"; \
	  else \
	    actual="$(if $(RECON_ACTUAL),$(RECON_ACTUAL),work/parity-java/$$case/actual)"; \
	  fi; \
	  $(KIT_JAVA) --network container:$$db $(INTEREST_JRE) \
	    java -cp $(INTEREST_JAR) -Dloader.main=com.carddemo.interest.recon.ReconCommand $(KIT_LAUNCHER) \
	      name=$$case inputDir=$$input outputDir=$$actual report=$$out/$$case.md $(if $(RECON_RUN),runId=$(RECON_RUN)) || fail=1; \
	  if [ -f $$out/$$case.md ]; then cat $$out/$$case.md >> $$out/report.md; fi; \
	done; \
	echo "report: $$out/report.md"; exit $$fail

# What the comparator finds between two directories, for the classification: a first line "#" with
# what was checked, then one difference per line (where, field, legacy, target; tabs).  The comparator
# is used unchanged; its fixture root is pointed at a link to the legacy directory.
# argv: legacy directory, target directory, name, output directory.
define SIDEBYSIDE_COMPARE
import sys
from pathlib import Path
sys.path.insert(0, ".")
from tools.parity import compare
legacy, target, name, out = Path(sys.argv[1]).resolve(), Path(sys.argv[2]), sys.argv[3], Path(sys.argv[4])
stage = out / "legacy" / "interest" / name
stage.mkdir(parents=True, exist_ok=True)
link = stage / "expected"
if link.is_symlink():
    link.unlink()
link.symlink_to(legacy, target_is_directory=True)
compare.FIXTURES = (out / "legacy").resolve()
diffs, checked = compare.compare_case("interest", name, target)
(out / "compare.md").unlink(missing_ok=True)
compare.write_report(out / "compare.md", "interest", name, "side by side", diffs, checked)
def column(text):
    return text.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n")
with (out / "differences.tsv").open("w", encoding="utf-8") as fh:
    fh.write("#\t" + "\t".join(f"{key}={value}" for key, value in checked.items()) + "\n")
    for diff in diffs:
        fh.write("\t".join(column(diff[key]) for key in ("where", "field", "expected", "actual")) + "\n")
endef
export SIDEBYSIDE_COMPARE

sidebyside-java:
	mvn -q -f java/interest/pom.xml -DskipTests package
	@out=$(SIDEBYSIDE_OUT); mkdir -p $$out; rm -f $$out/report.md; fail=0; \
	if [ -n "$(LEGACY)$(TARGET)" ]; then \
	  [ -n "$(LEGACY)" ] && [ -n "$(TARGET)" ] || { echo "usage: make sidebyside-java LEGACY=<dir> TARGET=<dir> [NAME=<label>] [TS=format|exact]"; exit 2; }; \
	  names="$(if $(NAME),$(NAME),$(if $(CASE),$(CASE),run))"; \
	else \
	  names="$(if $(CASE),$(CASE),$$(ls fixtures/interest))"; \
	fi; \
	for name in $$names; do \
	  legacy="$(if $(LEGACY),$(LEGACY),fixtures/interest/$$name/expected)"; \
	  target="$(if $(TARGET),$(TARGET),work/parity-java/$$name/actual)"; \
	  rm -rf $$out/$$name; mkdir -p $$out/$$name; \
	  if [ -f "$$legacy/manifest.json" ] && [ -d "$$target" ]; then \
	    PYTHONDONTWRITEBYTECODE=1 $(PY) -c "$$SIDEBYSIDE_COMPARE" "$$legacy" "$$target" $$name $$out/$$name \
	      || echo "interest/$$name: the comparator did not run"; \
	  fi; \
	  $(KIT_JAVA) $(INTEREST_JRE) \
	    java -cp $(INTEREST_JAR) -Dloader.main=com.carddemo.interest.recon.SideBySideCommand $(KIT_LAUNCHER) \
	      name=$$name legacy="$$legacy" target="$$target" differences=$$out/$$name/differences.tsv timestamps=$(TS) report=$$out/$$name/report.md || fail=1; \
	  if [ -f $$out/$$name/report.md ]; then cat $$out/$$name/report.md >> $$out/report.md; fi; \
	done; \
	echo "report: $$out/report.md"; exit $$fail

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
