# Executable specification of the CardDemo batch path

One Markdown file per program on the batch path (`CBTRN02C.md`, `CBACT04C.md`,
and so on), each written from `_template.md`. A rule is green only when its
acceptance test passes against the GnuCOBOL run; a rule without a passing
COBOL test is amber. The assessment page reads the rule tables from these
files.

The program list comes from `spec/manifest.yaml` and the JCL it points at:

    cd spec && python3 -m harness programs
