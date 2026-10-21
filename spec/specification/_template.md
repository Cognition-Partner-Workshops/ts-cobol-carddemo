# PROGRAM — short description

Job: JOB (app/jcl/JOB.jcl), step STEPNN. Reads: ... Writes: ...

## Rules

| Rule | COBOL citation | Acceptance test | Status |
|---|---|---|---|
| R01 one sentence a business reader can check | `app/cbl/PROGRAM.cbl:120-134` | `spec/tests/rules/test_program.py::test_program_r01_short_name` | green |

## Data dictionary

One table per copybook record the program reads or writes.

### COPYBOOK RECORD-NAME

| Field | PIC | Offset | Length | Meaning |
|---|---|---|---|---|

## Assumptions

Stubs and anything the run could not observe directly.
