# record-codec

Fixed-length record layouts for the CardDemo batch datasets, converted from the
copybooks in `app/cpy` and `tools/parity/layouts`. The converted chains under
`modern/` read and write their datasets through these layouts so every job
shares one definition of each record.

| Layout | Copybook | Length |
|---|---|---|
| `Layouts.DALYTRAN` | `CVTRA06Y` | 350 |
| `Layouts.TRAN` | `CVTRA05Y` | 350 |
| `Layouts.REJTRAN` | `tools/parity/layouts/REJTRAN.cpy` | 430 |

Zoned decimals use the EBCDIC overpunch convention (`cobc -fsign=EBCDIC`).
Lines are written the way `tools/parity/record.py` records them, one record per
line with trailing spaces stripped, so the output drops straight into
`tools/parity/compare.py`.

```sh
mvn -q -f modern/record-codec/pom.xml test        # Java 21
```
