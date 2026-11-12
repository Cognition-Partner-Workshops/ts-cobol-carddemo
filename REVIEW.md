# Review guidelines

## Record layouts (critical)

- Any change that reads or writes a fixed-length dataset through
  `modern/record-codec` must be checked against the copybook the layout
  mirrors: `app/cpy/*.cpy`, or `tools/parity/layouts/*.cpy` for layouts the
  harness defines. Check every field's offset, length, PIC, usage and sign.
- This applies to layouts the PR only calls, not just the ones it edits: a job
  that writes records through an existing layout inherits that layout's bugs.
- A field the copybook names but the layout treats as FILLER is a bug, because
  the converted job will write spaces where the legacy job writes data.

## Intentional legacy behaviour

- The quirks listed in `AGENTS.md` and `docs/BASELINE.md` are preserved on
  purpose (truncated interest, last TCATBALF account not rewritten, cycle-amount
  limit check, SQLCODE +100 ignored). Do not flag code that reproduces them.

## Ignore

- `fixtures/*/expected` are golden files written by `make record`.
