# Call graph - CardDemo legacy estate

Baseline `a5da4ee6bb4261392ac0a44a0756b82914b97fe8` (`main`). Built from the mftb 0.2.1 scan stores (`store/<MODULE>/*.json`, `store/catalog.json`) by `modernization/legacy/tools/build_graph.py`; the full edge list with every citation is `modernization/legacy/call-graph.json` and the browsable view is `call-graph.html`. Nothing here is taken from member names alone.

## How to read the status

* **PROVEN** - a static statement the scanner read, cited by file and line.
* **CANDIDATE** - a dynamic target the scan narrowed to a set of values, or a link inferred by matching names across modules. Not a fact.
* **OPEN** - the target is not settled in the source. It is an open ask; no target is guessed.

PROVEN and CANDIDATE edges are kept in separate tables throughout.

## Counts

| Edge family | PROVEN | CANDIDATE | OPEN | Total |
|---|---|---|---|---|
| Program to program | 35 | 69 | 14 | 118 |
| Job to step to program | 251 | 0 | 0 | 251 |
| Program to data | 316 | 1 | 17 | 334 |
| Copybook use | 350 | 0 | 0 | 350 |
| Transaction to program | 25 | 0 | 0 | 25 |
| Scheduler | 69 | 32 | 0 | 101 |
| **All edges** | 1046 | 102 | 31 | 1179 |

Nodes: 49 programs (46 COBOL, 2 HLASM leaf, 1 PL/I leaf), 48 jobs, 71 copybooks named in COPY/INCLUDE (30 used by more than one program), 25 transactions.

## 1. Program to program

### 1.1 PROVEN (35)

| From | Relation | To | Target is | Module (from -> to) | Cited at |
|---|---|---|---|---|---|
| `CBACT01C` | CALL | `CEE3ABD` | runtime routine | CARDDEMO -> - | `app/cbl/CBACT01C.cbl:410` |
| `CBACT01C` | CALL | `COBDATFT` | HLASM leaf | CARDDEMO -> CARDDEMO | `app/cbl/CBACT01C.cbl:231` |
| `CBACT02C` | CALL | `CEE3ABD` | runtime routine | CARDDEMO -> - | `app/cbl/CBACT02C.cbl:158` |
| `CBACT03C` | CALL | `CEE3ABD` | runtime routine | CARDDEMO -> - | `app/cbl/CBACT03C.cbl:158` |
| `CBACT04C` | CALL | `CEE3ABD` | runtime routine | CARDDEMO -> - | `app/cbl/CBACT04C.cbl:632` |
| `CBCUS01C` | CALL | `CEE3ABD` | runtime routine | CARDDEMO -> - | `app/cbl/CBCUS01C.cbl:158` |
| `CBEXPORT` | CALL | `CEE3ABD` | runtime routine | CARDDEMO -> - | `app/cbl/CBEXPORT.cbl:579` |
| `CBFEEDRV` | CALL | `CBFEECAL` | PL/I leaf | CARDDEMO -> CARDDEMO | `app/cbl/CBFEEDRV.cbl:101` |
| `CBIMPORT` | CALL | `CEE3ABD` | runtime routine | CARDDEMO -> - | `app/cbl/CBIMPORT.cbl:484` |
| `CBSTM03A` | CALL | `CBSTM03B` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/CBSTM03A.CBL:351`, `app/cbl/CBSTM03A.CBL:377`, `app/cbl/CBSTM03A.CBL:401` (+10 more) |
| `CBSTM03A` | CALL | `CEE3ABD` | runtime routine | CARDDEMO -> - | `app/cbl/CBSTM03A.CBL:923` |
| `CBTRN01C` | CALL | `CEE3ABD` | runtime routine | CARDDEMO -> - | `app/cbl/CBTRN01C.cbl:473` |
| `CBTRN02C` | CALL | `CEE3ABD` | runtime routine | CARDDEMO -> - | `app/cbl/CBTRN02C.cbl:711` |
| `CBTRN03C` | CALL | `CEE3ABD` | runtime routine | CARDDEMO -> - | `app/cbl/CBTRN03C.cbl:630` |
| `COBSWAIT` | CALL | `MVSWAIT` | HLASM leaf | CARDDEMO -> CARDDEMO | `app/cbl/COBSWAIT.cbl:38` |
| `CORPT00C` | CALL | `CSUTLDTC` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/CORPT00C.cbl:392`, `app/cbl/CORPT00C.cbl:412` |
| `COTRN02C` | CALL | `CSUTLDTC` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COTRN02C.cbl:393`, `app/cbl/COTRN02C.cbl:413` |
| `CSUTLDTC` | CALL | `CEEDAYS` | runtime routine | CARDDEMO -> - | `app/cbl/CSUTLDTC.cbl:116` |
| `COSGN00C` | XCTL | `COADM01C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COSGN00C.cbl:231` |
| `COSGN00C` | XCTL | `COMEN01C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COSGN00C.cbl:236` |
| `COPAUA0C` | CALL | `MQCLOSE` | MQ API stub | AUTH -> - | `app/app-authorization-ims-db2-mq/cbl/COPAUA0C.cbl:956` |
| `COPAUA0C` | CALL | `MQGET` | MQ API stub | AUTH -> - | `app/app-authorization-ims-db2-mq/cbl/COPAUA0C.cbl:400` |
| `COPAUA0C` | CALL | `MQOPEN` | MQ API stub | AUTH -> - | `app/app-authorization-ims-db2-mq/cbl/COPAUA0C.cbl:262` |
| `COPAUA0C` | CALL | `MQPUT1` | MQ API stub | AUTH -> - | `app/app-authorization-ims-db2-mq/cbl/COPAUA0C.cbl:758` |
| `DBUNLDGS` | CALL | `CBLTDLI` | IMS interface | AUTH -> - | `app/app-authorization-ims-db2-mq/cbl/DBUNLDGS.CBL:222`, `app/app-authorization-ims-db2-mq/cbl/DBUNLDGS.CBL:267`, `app/app-authorization-ims-db2-mq/cbl/DBUNLDGS.CBL:302` (+1 more) |
| `PAUDBLOD` | CALL | `CBLTDLI` | IMS interface | AUTH -> - | `app/app-authorization-ims-db2-mq/cbl/PAUDBLOD.CBL:244`, `app/app-authorization-ims-db2-mq/cbl/PAUDBLOD.CBL:296`, `app/app-authorization-ims-db2-mq/cbl/PAUDBLOD.CBL:321` |
| `PAUDBUNL` | CALL | `CBLTDLI` | IMS interface | AUTH -> - | `app/app-authorization-ims-db2-mq/cbl/PAUDBUNL.CBL:213`, `app/app-authorization-ims-db2-mq/cbl/PAUDBUNL.CBL:257` |
| `COACCT01` | CALL | `MQCLOSE` | MQ API stub | VSAMMQ -> - | `app/app-vsam-mq/cbl/COACCT01.cbl:557`, `app/app-vsam-mq/cbl/COACCT01.cbl:579`, `app/app-vsam-mq/cbl/COACCT01.cbl:602` |
| `COACCT01` | CALL | `MQGET` | MQ API stub | VSAMMQ -> - | `app/app-vsam-mq/cbl/COACCT01.cbl:352` |
| `COACCT01` | CALL | `MQOPEN` | MQ API stub | VSAMMQ -> - | `app/app-vsam-mq/cbl/COACCT01.cbl:233`, `app/app-vsam-mq/cbl/COACCT01.cbl:267`, `app/app-vsam-mq/cbl/COACCT01.cbl:302` |
| `COACCT01` | CALL | `MQPUT` | MQ API stub | VSAMMQ -> - | `app/app-vsam-mq/cbl/COACCT01.cbl:479`, `app/app-vsam-mq/cbl/COACCT01.cbl:516` |
| `CODATE01` | CALL | `MQCLOSE` | MQ API stub | VSAMMQ -> - | `app/app-vsam-mq/cbl/CODATE01.cbl:461`, `app/app-vsam-mq/cbl/CODATE01.cbl:483`, `app/app-vsam-mq/cbl/CODATE01.cbl:506` |
| `CODATE01` | CALL | `MQGET` | MQ API stub | VSAMMQ -> - | `app/app-vsam-mq/cbl/CODATE01.cbl:301` |
| `CODATE01` | CALL | `MQOPEN` | MQ API stub | VSAMMQ -> - | `app/app-vsam-mq/cbl/CODATE01.cbl:182`, `app/app-vsam-mq/cbl/CODATE01.cbl:216`, `app/app-vsam-mq/cbl/CODATE01.cbl:251` |
| `CODATE01` | CALL | `MQPUT` | MQ API stub | VSAMMQ -> - | `app/app-vsam-mq/cbl/CODATE01.cbl:383`, `app/app-vsam-mq/cbl/CODATE01.cbl:420` |

HLASM and PL/I members are leaf nodes: `COBDATFT` and `MVSWAIT` (HLASM), `CBFEECAL` (PL/I). The scan records the CALL into them; their own logic is not analysed.

### 1.2 CANDIDATE (69)

Dynamic `XCTL`/`LINK` whose target field the scan narrowed to these values. Each needs confirming.

| From | Relation | To | Target is | Module (from -> to) | Cited at |
|---|---|---|---|---|---|
| `COACTUPC` | XCTL | `COACTUPC` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COACTUPC.cbl:956` |
| `COACTUPC` | XCTL | `COMEN01C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COACTUPC.cbl:956` |
| `COACTVWC` | XCTL | `COACTVWC` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COACTVWC.cbl:349` |
| `COACTVWC` | XCTL | `COMEN01C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COACTVWC.cbl:349` |
| `COADM01C` | XCTL | `COSGN00C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COADM01C.cbl:168` |
| `COADM01C` | XCTL | `COTRTLIC` | COBOL program | CARDDEMO -> TRANTYPE **(cross-module)** | `app/cbl/COADM01C.cbl:145` |
| `COADM01C` | XCTL | `COTRTUPC` | COBOL program | CARDDEMO -> TRANTYPE **(cross-module)** | `app/cbl/COADM01C.cbl:145` |
| `COADM01C` | XCTL | `COUSR00C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COADM01C.cbl:145` |
| `COADM01C` | XCTL | `COUSR01C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COADM01C.cbl:145` |
| `COADM01C` | XCTL | `COUSR02C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COADM01C.cbl:145` |
| `COADM01C` | XCTL | `COUSR03C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COADM01C.cbl:145` |
| `COBIL00C` | XCTL | `COBIL00C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COBIL00C.cbl:281` |
| `COBIL00C` | XCTL | `COMEN01C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COBIL00C.cbl:281` |
| `COBIL00C` | XCTL | `COSGN00C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COBIL00C.cbl:281` |
| `COCRDLIC` | XCTL | `COCRDLIC` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COCRDLIC.cbl:538`, `app/cbl/COCRDLIC.cbl:566` |
| `COCRDLIC` | XCTL | `COCRDSLC` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COCRDLIC.cbl:538`, `app/cbl/COCRDLIC.cbl:566` |
| `COCRDLIC` | XCTL | `COCRDUPC` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COCRDLIC.cbl:538`, `app/cbl/COCRDLIC.cbl:566` |
| `COCRDLIC` | XCTL | `COMEN01C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COCRDLIC.cbl:402` |
| `COCRDSLC` | XCTL | `COCRDSLC` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COCRDSLC.cbl:331` |
| `COCRDSLC` | XCTL | `COMEN01C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COCRDSLC.cbl:331` |
| `COCRDUPC` | XCTL | `COCRDUPC` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COCRDUPC.cbl:473` |
| `COCRDUPC` | XCTL | `COMEN01C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COCRDUPC.cbl:473` |
| `COMEN01C` | XCTL | `COACTUPC` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COMEN01C.cbl:184` |
| `COMEN01C` | XCTL | `COACTVWC` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COMEN01C.cbl:184` |
| `COMEN01C` | XCTL | `COBIL00C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COMEN01C.cbl:184` |
| `COMEN01C` | XCTL | `COCRDLIC` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COMEN01C.cbl:184` |
| `COMEN01C` | XCTL | `COCRDSLC` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COMEN01C.cbl:184` |
| `COMEN01C` | XCTL | `COCRDUPC` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COMEN01C.cbl:184` |
| `COMEN01C` | XCTL | `COPAUS0C` | COBOL program | CARDDEMO -> AUTH **(cross-module)** | `app/cbl/COMEN01C.cbl:156` |
| `COMEN01C` | XCTL | `CORPT00C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COMEN01C.cbl:184` |
| `COMEN01C` | XCTL | `COSGN00C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COMEN01C.cbl:201` |
| `COMEN01C` | XCTL | `COTRN00C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COMEN01C.cbl:184` |
| `COMEN01C` | XCTL | `COTRN01C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COMEN01C.cbl:184` |
| `COMEN01C` | XCTL | `COTRN02C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COMEN01C.cbl:184` |
| `CORPT00C` | XCTL | `COMEN01C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/CORPT00C.cbl:548` |
| `CORPT00C` | XCTL | `COSGN00C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/CORPT00C.cbl:548` |
| `COTRN00C` | XCTL | `COMEN01C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COTRN00C.cbl:518` |
| `COTRN00C` | XCTL | `COSGN00C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COTRN00C.cbl:518` |
| `COTRN00C` | XCTL | `COTRN01C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COTRN00C.cbl:192` |
| `COTRN01C` | XCTL | `COMEN01C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COTRN01C.cbl:205` |
| `COTRN01C` | XCTL | `COSGN00C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COTRN01C.cbl:205` |
| `COTRN01C` | XCTL | `COTRN00C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COTRN01C.cbl:205` |
| `COTRN01C` | XCTL | `COTRN01C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COTRN01C.cbl:205` |
| `COTRN02C` | XCTL | `COMEN01C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COTRN02C.cbl:508` |
| `COTRN02C` | XCTL | `COSGN00C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COTRN02C.cbl:508` |
| `COTRN02C` | XCTL | `COTRN02C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COTRN02C.cbl:508` |
| `COUSR00C` | XCTL | `COADM01C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COUSR00C.cbl:514` |
| `COUSR00C` | XCTL | `COSGN00C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COUSR00C.cbl:514` |
| `COUSR00C` | XCTL | `COUSR02C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COUSR00C.cbl:196` |
| `COUSR00C` | XCTL | `COUSR03C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COUSR00C.cbl:206` |
| `COUSR01C` | XCTL | `COADM01C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COUSR01C.cbl:175` |
| `COUSR01C` | XCTL | `COSGN00C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COUSR01C.cbl:175` |
| `COUSR02C` | XCTL | `COADM01C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COUSR02C.cbl:258` |
| `COUSR02C` | XCTL | `COSGN00C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COUSR02C.cbl:258` |
| `COUSR02C` | XCTL | `COUSR02C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COUSR02C.cbl:258` |
| `COUSR03C` | XCTL | `COADM01C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COUSR03C.cbl:205` |
| `COUSR03C` | XCTL | `COSGN00C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COUSR03C.cbl:205` |
| `COUSR03C` | XCTL | `COUSR03C` | COBOL program | CARDDEMO -> CARDDEMO | `app/cbl/COUSR03C.cbl:205` |
| `COPAUS1C` | LINK | `COPAUS2C` | COBOL program | AUTH -> AUTH | `app/app-authorization-ims-db2-mq/cbl/COPAUS1C.cbl:248` |
| `COPAUS0C` | XCTL | `COMEN01C` | COBOL program | AUTH -> CARDDEMO **(cross-module)** | `app/app-authorization-ims-db2-mq/cbl/COPAUS0C.cbl:674` |
| `COPAUS0C` | XCTL | `COPAUS0C` | COBOL program | AUTH -> AUTH | `app/app-authorization-ims-db2-mq/cbl/COPAUS0C.cbl:674` |
| `COPAUS0C` | XCTL | `COPAUS1C` | COBOL program | AUTH -> AUTH | `app/app-authorization-ims-db2-mq/cbl/COPAUS0C.cbl:322`, `app/app-authorization-ims-db2-mq/cbl/COPAUS0C.cbl:674` |
| `COPAUS0C` | XCTL | `COSGN00C` | COBOL program | AUTH -> CARDDEMO **(cross-module)** | `app/app-authorization-ims-db2-mq/cbl/COPAUS0C.cbl:674` |
| `COPAUS1C` | XCTL | `COPAUS0C` | COBOL program | AUTH -> AUTH | `app/app-authorization-ims-db2-mq/cbl/COPAUS1C.cbl:367` |
| `COTRTLIC` | XCTL | `COADM01C` | COBOL program | TRANTYPE -> CARDDEMO **(cross-module)** | `app/app-transaction-type-db2/cbl/COTRTLIC.cbl:620` |
| `COTRTLIC` | XCTL | `COTRTLIC` | COBOL program | TRANTYPE -> TRANTYPE | `app/app-transaction-type-db2/cbl/COTRTLIC.cbl:620` |
| `COTRTLIC` | XCTL | `COTRTUPC` | COBOL program | TRANTYPE -> TRANTYPE | `app/app-transaction-type-db2/cbl/COTRTLIC.cbl:620`, `app/app-transaction-type-db2/cbl/COTRTLIC.cbl:648` |
| `COTRTUPC` | XCTL | `COADM01C` | COBOL program | TRANTYPE -> CARDDEMO **(cross-module)** | `app/app-transaction-type-db2/cbl/COTRTUPC.cbl:457` |
| `COTRTUPC` | XCTL | `COTRTUPC` | COBOL program | TRANTYPE -> TRANTYPE | `app/app-transaction-type-db2/cbl/COTRTUPC.cbl:457` |

### 1.3 Cross-module edges (no child scan could see both ends)

| From (module) | Relation | To (module) | Status | Cited at |
|---|---|---|---|---|
| `COADM01C` (CARDDEMO) | XCTL | `COTRTLIC` (TRANTYPE) | CANDIDATE | `app/cbl/COADM01C.cbl:145` |
| `COADM01C` (CARDDEMO) | XCTL | `COTRTUPC` (TRANTYPE) | CANDIDATE | `app/cbl/COADM01C.cbl:145` |
| `COMEN01C` (CARDDEMO) | XCTL | `COPAUS0C` (AUTH) | CANDIDATE | `app/cbl/COMEN01C.cbl:156` |
| `COPAUS0C` (AUTH) | XCTL | `COMEN01C` (CARDDEMO) | CANDIDATE | `app/app-authorization-ims-db2-mq/cbl/COPAUS0C.cbl:674` |
| `COPAUS0C` (AUTH) | XCTL | `COSGN00C` (CARDDEMO) | CANDIDATE | `app/app-authorization-ims-db2-mq/cbl/COPAUS0C.cbl:674` |
| `COTRTLIC` (TRANTYPE) | XCTL | `COADM01C` (CARDDEMO) | CANDIDATE | `app/app-transaction-type-db2/cbl/COTRTLIC.cbl:620` |
| `COTRTUPC` (TRANTYPE) | XCTL | `COADM01C` (CARDDEMO) | CANDIDATE | `app/app-transaction-type-db2/cbl/COTRTUPC.cbl:457` |

All program-to-program edges between modules are CANDIDATE: the base menus transfer control through a table (`EXEC CICS XCTL PROGRAM(CDEMO-MENU-OPT-PGMNAME(WS-OPTION))`, `app/cbl/COMEN01C.cbl:156`), not a literal. `store/estate.json` lists two of them (`COMEN01C` to `COPAUS0C`, `COADM01C` to `COTRTLIC`) as proven; the module traces grade them candidate and this graph follows the traces.

### 1.4 OPEN - dynamic targets not settled (14 open asks)

| Program | Statement | Field | Cited at | Owner |
|---|---|---|---|---|
| `COACTUPC` | XCTL | (not settled: CDEMO-TO-PROGRAM) | `app/cbl/COACTUPC.cbl:956` | _TBD_ |
| `COACTVWC` | XCTL | (not settled: CDEMO-TO-PROGRAM) | `app/cbl/COACTVWC.cbl:349` | _TBD_ |
| `COADM01C` | XCTL | (not settled: CDEMO-TO-PROGRAM) | `app/cbl/COADM01C.cbl:168` | _TBD_ |
| `COBIL00C` | XCTL | (not settled: CDEMO-TO-PROGRAM) | `app/cbl/COBIL00C.cbl:281` | _TBD_ |
| `COCRDSLC` | XCTL | (not settled: CDEMO-TO-PROGRAM) | `app/cbl/COCRDSLC.cbl:331` | _TBD_ |
| `COCRDUPC` | XCTL | (not settled: CDEMO-TO-PROGRAM) | `app/cbl/COCRDUPC.cbl:473` | _TBD_ |
| `COMEN01C` | XCTL | (not settled: CDEMO-TO-PROGRAM) | `app/cbl/COMEN01C.cbl:201` | _TBD_ |
| `COTRN01C` | XCTL | (not settled: CDEMO-TO-PROGRAM) | `app/cbl/COTRN01C.cbl:205` | _TBD_ |
| `COTRN02C` | XCTL | (not settled: CDEMO-TO-PROGRAM) | `app/cbl/COTRN02C.cbl:508` | _TBD_ |
| `COUSR02C` | XCTL | (not settled: CDEMO-TO-PROGRAM) | `app/cbl/COUSR02C.cbl:258` | _TBD_ |
| `COUSR03C` | XCTL | (not settled: CDEMO-TO-PROGRAM) | `app/cbl/COUSR03C.cbl:205` | _TBD_ |
| `COPAUS0C` | XCTL | (not settled: CDEMO-TO-PROGRAM) | `app/app-authorization-ims-db2-mq/cbl/COPAUS0C.cbl:674` | _TBD_ |
| `CBTRNDYN` | CALL (dynamic) | (not settled: WS-PGM-NAME) | `app/app-transaction-type-db2/cbl/CBTRNDYN.cbl:62` | _TBD_ |
| `COTRTUPC` | XCTL | (not settled: CDEMO-TO-PROGRAM) | `app/app-transaction-type-db2/cbl/COTRTUPC.cbl:457` | _TBD_ |

## 2. Job to step to program

All 251 edges are PROVEN (JCL `EXEC` statements). One row per step.

| Job (module) | Step | EXEC | Program run | Cited at |
|---|---|---|---|---|
| `ACCTFILE` (CARDDEMO) | STEP05 | PGM IDCAMS | - | `app/jcl/ACCTFILE.jcl:22` |
| `ACCTFILE` (CARDDEMO) | STEP10 | PGM IDCAMS | - | `app/jcl/ACCTFILE.jcl:33` |
| `ACCTFILE` (CARDDEMO) | STEP15 | PGM IDCAMS | - | `app/jcl/ACCTFILE.jcl:54` |
| `CARDFILE` (CARDDEMO) | CLCIFIL | PGM SDSF | - | `app/jcl/CARDFILE.jcl:22` |
| `CARDFILE` (CARDDEMO) | STEP05 | PGM IDCAMS | - | `app/jcl/CARDFILE.jcl:33` |
| `CARDFILE` (CARDDEMO) | STEP10 | PGM IDCAMS | - | `app/jcl/CARDFILE.jcl:47` |
| `CARDFILE` (CARDDEMO) | STEP15 | PGM IDCAMS | - | `app/jcl/CARDFILE.jcl:68` |
| `CARDFILE` (CARDDEMO) | STEP40 | PGM IDCAMS | - | `app/jcl/CARDFILE.jcl:80` |
| `CARDFILE` (CARDDEMO) | STEP50 | PGM IDCAMS | - | `app/jcl/CARDFILE.jcl:97` |
| `CARDFILE` (CARDDEMO) | STEP60 | PGM IDCAMS | - | `app/jcl/CARDFILE.jcl:107` |
| `CARDFILE` (CARDDEMO) | OPCIFIL | PGM SDSF | - | `app/jcl/CARDFILE.jcl:118` |
| `CBADMCDJ` (CARDDEMO) | STEP1 | PGM DFHCSDUP | - | `app/jcl/CBADMCDJ.jcl:27` |
| `CBEXPORT` (CARDDEMO) | STEP01 | PGM IDCAMS | - | `app/jcl/CBEXPORT.jcl:24` |
| `CBEXPORT` (CARDDEMO) | STEP02 | PGM CBEXPORT | - | `app/jcl/CBEXPORT.jcl:43` |
| `CBIMPORT` (CARDDEMO) | STEP01 | PGM CBIMPORT | - | `app/jcl/CBIMPORT.jcl:22` |
| `CLOSEFIL` (CARDDEMO) | CLCIFIL | PGM SDSF | - | `app/jcl/CLOSEFIL.jcl:22` |
| `COMBTRAN` (CARDDEMO) | STEP05R | PGM SORT | - | `app/jcl/COMBTRAN.jcl:22` |
| `COMBTRAN` (CARDDEMO) | STEP10 | PGM IDCAMS | - | `app/jcl/COMBTRAN.jcl:41` |
| `CREASTMT` (CARDDEMO) | DELDEF01 | PGM IDCAMS | - | `app/jcl/CREASTMT.JCL:22` |
| `CREASTMT` (CARDDEMO) | STEP010 | PGM SORT | - | `app/jcl/CREASTMT.JCL:44` |
| `CREASTMT` (CARDDEMO) | STEP020 | PGM IDCAMS | - | `app/jcl/CREASTMT.JCL:56` |
| `CREASTMT` (CARDDEMO) | STEP030 | PGM IEFBR14 | - | `app/jcl/CREASTMT.JCL:66` |
| `CREASTMT` (CARDDEMO) | STEP040 | PGM CBSTM03A | - | `app/jcl/CREASTMT.JCL:79` |
| `CUSTFILE` (CARDDEMO) | CLCIFIL | PGM SDSF | - | `app/jcl/CUSTFILE.jcl:22` |
| `CUSTFILE` (CARDDEMO) | STEP05 | PGM IDCAMS | - | `app/jcl/CUSTFILE.jcl:32` |
| `CUSTFILE` (CARDDEMO) | STEP10 | PGM IDCAMS | - | `app/jcl/CUSTFILE.jcl:43` |
| `CUSTFILE` (CARDDEMO) | STEP15 | PGM IDCAMS | - | `app/jcl/CUSTFILE.jcl:64` |
| `CUSTFILE` (CARDDEMO) | OPCIFIL | PGM SDSF | - | `app/jcl/CUSTFILE.jcl:76` |
| `DALYREJS` (CARDDEMO) | STEP05 | PGM IDCAMS | - | `app/jcl/DALYREJS.jcl:21` |
| `DEFCUST` (CARDDEMO) | STEP05 | PGM IDCAMS | - | `app/jcl/DEFCUST.jcl:32` |
| `DEFGDGB` (CARDDEMO) | STEP05 | PGM IDCAMS | - | `app/jcl/DEFGDGB.jcl:21` |
| `DEFGDGD` (CARDDEMO) | STEP10 | PGM IDCAMS | - | `app/jcl/DEFGDGD.jcl:24` |
| `DEFGDGD` (CARDDEMO) | STEP20 | PGM IEBGENER | - | `app/jcl/DEFGDGD.jcl:36` |
| `DEFGDGD` (CARDDEMO) | STEP30 | PGM IDCAMS | - | `app/jcl/DEFGDGD.jcl:47` |
| `DEFGDGD` (CARDDEMO) | STEP40 | PGM IEBGENER | - | `app/jcl/DEFGDGD.jcl:59` |
| `DEFGDGD` (CARDDEMO) | STEP50 | PGM IDCAMS | - | `app/jcl/DEFGDGD.jcl:70` |
| `DEFGDGD` (CARDDEMO) | STEP60 | PGM IEBGENER | - | `app/jcl/DEFGDGD.jcl:82` |
| `DISCGRP` (CARDDEMO) | STEP05 | PGM IDCAMS | - | `app/jcl/DISCGRP.jcl:22` |
| `DISCGRP` (CARDDEMO) | STEP10 | PGM IDCAMS | - | `app/jcl/DISCGRP.jcl:33` |
| `DISCGRP` (CARDDEMO) | STEP15 | PGM IDCAMS | - | `app/jcl/DISCGRP.jcl:54` |
| `DUSRSECJ` (CARDDEMO) | PREDEL | PGM IEFBR14 | - | `app/jcl/DUSRSECJ.jcl:23` |
| `DUSRSECJ` (CARDDEMO) | STEP01 | PGM IEBGENER | - | `app/jcl/DUSRSECJ.jcl:32` |
| `DUSRSECJ` (CARDDEMO) | STEP02 | PGM IDCAMS | - | `app/jcl/DUSRSECJ.jcl:58` |
| `DUSRSECJ` (CARDDEMO) | STEP03 | PGM IDCAMS | - | `app/jcl/DUSRSECJ.jcl:80` |
| `ESDSRRDS` (CARDDEMO) | PREDEL | PGM IEFBR14 | - | `app/jcl/ESDSRRDS.jcl:24` |
| `ESDSRRDS` (CARDDEMO) | STEP01 | PGM IEBGENER | - | `app/jcl/ESDSRRDS.jcl:33` |
| `ESDSRRDS` (CARDDEMO) | STEP02 | PGM IDCAMS | - | `app/jcl/ESDSRRDS.jcl:59` |
| `ESDSRRDS` (CARDDEMO) | STEP03 | PGM IDCAMS | - | `app/jcl/ESDSRRDS.jcl:79` |
| `ESDSRRDS` (CARDDEMO) | STEP04 | PGM IDCAMS | - | `app/jcl/ESDSRRDS.jcl:93` |
| `ESDSRRDS` (CARDDEMO) | STEP05 | PGM IDCAMS | - | `app/jcl/ESDSRRDS.jcl:113` |
| `FEECALC` (CARDDEMO) | STEP05 | PGM CBFEEDRV | - | `app/jcl/FEECALC.jcl:24` |
| `FTPJCLS` (CARDDEMO) | STEP1 | PGM FTP | - | `app/jcl/FTPJCL.JCL:30` |
| `INTCALC` (CARDDEMO) | STEP15 | PGM CBACT04C | - | `app/jcl/INTCALC.jcl:22` |
| `INTRDRJ1` (CARDDEMO) | IDCAMS | PGM IDCAMS | - | `app/jcl/INTRDRJ1.JCL:6` |
| `INTRDRJ1` (CARDDEMO) | STEP01 | PGM IEBGENER | - | `app/jcl/INTRDRJ1.JCL:14` |
| `INTRDRJ2` (CARDDEMO) | IDCAMS | PGM IDCAMS | - | `app/jcl/INTRDRJ2.JCL:7` |
| `OPENFIL` (CARDDEMO) | OPCIFIL | PGM SDSF | - | `app/jcl/OPENFIL.jcl:22` |
| `POSTTRAN` (CARDDEMO) | STEP15 | PGM CBTRN02C | - | `app/jcl/POSTTRAN.jcl:23` |
| `PRTCATBL` (CARDDEMO) | DELDEF | PGM IEFBR14 | - | `app/jcl/PRTCATBL.jcl:21` |
| `PRTCATBL` (CARDDEMO) | STEP05R | PROC REPROC PGM IDCAMS | - | `app/proc/REPROC.prc:21` |
| `PRTCATBL` (CARDDEMO) | STEP10R | PGM SORT | - | `app/jcl/PRTCATBL.jcl:43` |
| `READACCT` (CARDDEMO) | PREDEL | PGM IEFBR14 | - | `app/jcl/READACCT.jcl:22` |
| `READACCT` (CARDDEMO) | STEP05 | PGM CBACT01C | - | `app/jcl/READACCT.jcl:32` |
| `READCARD` (CARDDEMO) | STEP05 | PGM CBACT02C | - | `app/jcl/READCARD.jcl:22` |
| `READCUST` (CARDDEMO) | STEP05 | PGM CBCUS01C | - | `app/jcl/READCUST.jcl:21` |
| `READXREF` (CARDDEMO) | STEP05 | PGM CBACT03C | - | `app/jcl/READXREF.jcl:22` |
| `REPTFILE` (CARDDEMO) | STEP05 | PGM IDCAMS | - | `app/jcl/REPTFILE.jcl:22` |
| `TCATBALF` (CARDDEMO) | STEP05 | PGM IDCAMS | - | `app/jcl/TCATBALF.jcl:22` |
| `TCATBALF` (CARDDEMO) | STEP10 | PGM IDCAMS | - | `app/jcl/TCATBALF.jcl:33` |
| `TCATBALF` (CARDDEMO) | STEP15 | PGM IDCAMS | - | `app/jcl/TCATBALF.jcl:54` |
| `TRANBKP` (CARDDEMO) | STEP05R | PROC REPROC PGM IDCAMS | - | `app/proc/REPROC.prc:21` |
| `TRANBKP` (CARDDEMO) | STEP05 | PGM IDCAMS | - | `app/jcl/TRANBKP.jcl:37` |
| `TRANBKP` (CARDDEMO) | STEP10 | PGM IDCAMS | - | `app/jcl/TRANBKP.jcl:51` |
| `TRANCATG` (CARDDEMO) | STEP05 | PGM IDCAMS | - | `app/jcl/TRANCATG.jcl:22` |
| `TRANCATG` (CARDDEMO) | STEP10 | PGM IDCAMS | - | `app/jcl/TRANCATG.jcl:33` |
| `TRANCATG` (CARDDEMO) | STEP15 | PGM IDCAMS | - | `app/jcl/TRANCATG.jcl:54` |
| `TRANFILE` (CARDDEMO) | CLCIFIL | PGM SDSF | - | `app/jcl/TRANFILE.jcl:22` |
| `TRANFILE` (CARDDEMO) | STEP05 | PGM IDCAMS | - | `app/jcl/TRANFILE.jcl:32` |
| `TRANFILE` (CARDDEMO) | STEP10 | PGM IDCAMS | - | `app/jcl/TRANFILE.jcl:46` |
| `TRANFILE` (CARDDEMO) | STEP15 | PGM IDCAMS | - | `app/jcl/TRANFILE.jcl:67` |
| `TRANFILE` (CARDDEMO) | STEP20 | PGM IDCAMS | - | `app/jcl/TRANFILE.jcl:79` |
| `TRANFILE` (CARDDEMO) | STEP25 | PGM IDCAMS | - | `app/jcl/TRANFILE.jcl:96` |
| `TRANFILE` (CARDDEMO) | STEP30 | PGM IDCAMS | - | `app/jcl/TRANFILE.jcl:106` |
| `TRANFILE` (CARDDEMO) | OPCIFIL | PGM SDSF | - | `app/jcl/TRANFILE.jcl:116` |
| `TRANIDX` (CARDDEMO) | STEP20 | PGM IDCAMS | - | `app/jcl/TRANIDX.jcl:22` |
| `TRANIDX` (CARDDEMO) | STEP25 | PGM IDCAMS | - | `app/jcl/TRANIDX.jcl:39` |
| `TRANIDX` (CARDDEMO) | STEP30 | PGM IDCAMS | - | `app/jcl/TRANIDX.jcl:49` |
| `TRANREPT` (CARDDEMO) | STEP05R | PGM SORT | - | `app/jcl/TRANREPT.jcl:37` |
| `TRANREPT` (CARDDEMO) | STEP10R | PGM CBTRN03C | - | `app/jcl/TRANREPT.jcl:59` |
| `TRANTYPE` (CARDDEMO) | STEP05 | PGM IDCAMS | - | `app/jcl/TRANTYPE.jcl:22` |
| `TRANTYPE` (CARDDEMO) | STEP10 | PGM IDCAMS | - | `app/jcl/TRANTYPE.jcl:33` |
| `TRANTYPE` (CARDDEMO) | STEP15 | PGM IDCAMS | - | `app/jcl/TRANTYPE.jcl:54` |
| `TXT2PDF1` (CARDDEMO) | TXT2PDF | PGM IKJEFT1B | - | `app/jcl/TXT2PDF1.JCL:24` |
| `WAITSTEP` (CARDDEMO) | WAIT | PGM COBSWAIT | - | `app/jcl/WAITSTEP.jcl:22` |
| `XREFFILE` (CARDDEMO) | STEP05 | PGM IDCAMS | - | `app/jcl/XREFFILE.jcl:22` |
| `XREFFILE` (CARDDEMO) | STEP10 | PGM IDCAMS | - | `app/jcl/XREFFILE.jcl:36` |
| `XREFFILE` (CARDDEMO) | STEP15 | PGM IDCAMS | - | `app/jcl/XREFFILE.jcl:57` |
| `XREFFILE` (CARDDEMO) | STEP20 | PGM IDCAMS | - | `app/jcl/XREFFILE.jcl:69` |
| `XREFFILE` (CARDDEMO) | STEP25 | PGM IDCAMS | - | `app/jcl/XREFFILE.jcl:87` |
| `XREFFILE` (CARDDEMO) | STEP30 | PGM IDCAMS | - | `app/jcl/XREFFILE.jcl:97` |
| `CBPAUP0J` (AUTH) | STEP01 | PGM DFSRRC00 | `CBPAUP0C` (under DFSRRC00) | `app/app-authorization-ims-db2-mq/jcl/CBPAUP0J.jcl:24` |
| `DBPAUTP0` (AUTH) | STEPDEL | PGM IEFBR14 | - | `app/app-authorization-ims-db2-mq/jcl/DBPAUTP0.jcl:7` |
| `DBPAUTP0` (AUTH) | UNLOAD | PGM DFSRRC00 | - | `app/app-authorization-ims-db2-mq/jcl/DBPAUTP0.jcl:15` |
| `LOADPADB` (AUTH) | STEP01 | PGM DFSRRC00 | `PAUDBLOD` (under DFSRRC00) | `app/app-authorization-ims-db2-mq/jcl/LOADPADB.JCL:26` |
| `UNLDGSAM` (AUTH) | STEP01 | PGM DFSRRC00 | `DBUNLDGS` (under DFSRRC00) | `app/app-authorization-ims-db2-mq/jcl/UNLDGSAM.JCL:26` |
| `UNLDPADB` (AUTH) | STEP0 | PGM IEFBR14 | - | `app/app-authorization-ims-db2-mq/jcl/UNLDPADB.JCL:25` |
| `UNLDPADB` (AUTH) | STEP01 | PGM DFSRRC00 | `PAUDBUNL` (under DFSRRC00) | `app/app-authorization-ims-db2-mq/jcl/UNLDPADB.JCL:38` |
| `CREADB2` (TRANTYPE) | FREEPLN | PGM IKJEFT01 | - | `app/app-transaction-type-db2/jcl/CREADB21.jcl:40` |
| `CREADB2` (TRANTYPE) | CRCRDDB | PGM IKJEFT01 | - | `app/app-transaction-type-db2/jcl/CREADB21.jcl:52` |
| `CREADB2` (TRANTYPE) | LDTTYPE | PGM IEFBR14 | - | `app/app-transaction-type-db2/jcl/CREADB21.jcl:64` |
| `CREADB2` (TRANTYPE) | RUNTEP2 | PGM IKJEFT01 | - | `app/app-transaction-type-db2/jcl/CREADB21.jcl:65` |
| `CREADB2` (TRANTYPE) | LDTCCAT | PGM IKJEFT01 | - | `app/app-transaction-type-db2/jcl/CREADB21.jcl:77` |
| `MNTTRDB2` (TRANTYPE) | STEP1 | PGM IKJEFT01 | `COBTUPDT` (under IKJEFT01) | `app/app-transaction-type-db2/jcl/MNTTRDB2.jcl:21` |
| `MNTTRDYN` (TRANTYPE) | STEP1 | PGM IKJEFT01 | `CBTRNDYN` (under IKJEFT01) | `app/app-transaction-type-db2/jcl/MNTTRDYN.jcl:23` |
| `TRANEXTR` (TRANTYPE) | STEP10 | PGM IEBGENER | - | `app/app-transaction-type-db2/jcl/TRANEXTR.jcl:31` |
| `TRANEXTR` (TRANTYPE) | STEP20 | PGM IEBGENER | - | `app/app-transaction-type-db2/jcl/TRANEXTR.jcl:42` |
| `TRANEXTR` (TRANTYPE) | STEP30 | PGM IEFBR14 | - | `app/app-transaction-type-db2/jcl/TRANEXTR.jcl:53` |
| `TRANEXTR` (TRANTYPE) | STEP40 | PGM IKJEFT01 | - | `app/app-transaction-type-db2/jcl/TRANEXTR.jcl:65` |
| `TRANEXTR` (TRANTYPE) | STEP50 | PGM IKJEFT01 | - | `app/app-transaction-type-db2/jcl/TRANEXTR.jcl:95` |

## 3. Program to dataset, table, queue or map

### 3.1 PROVEN - programs (153)

| From | Direction | To | Store | Status | Cited at | Note |
|---|---|---|---|---|---|---|
| `CBACT01C` | reads | `AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS` | dataset | PROVEN | `app/cbl/CBACT01C.cbl:29`, `app/jcl/READACCT.jcl:35` | DD ACCTFILE in READACCT.STEP05, INDEXED |
| `CBACT01C` | writes | `AWS.M2.CARDDEMO.ACCTDATA.PSCOMP` | dataset | PROVEN | `app/cbl/CBACT01C.cbl:35`, `app/jcl/READACCT.jcl:37` | DD OUTFILE in READACCT.STEP05, SEQUENTIAL |
| `CBACT01C` | writes | `AWS.M2.CARDDEMO.ACCTDATA.ARRYPS` | dataset | PROVEN | `app/cbl/CBACT01C.cbl:40`, `app/jcl/READACCT.jcl:41` | DD ARRYFILE in READACCT.STEP05, SEQUENTIAL |
| `CBACT01C` | writes | `AWS.M2.CARDDEMO.ACCTDATA.VBPS` | dataset | PROVEN | `app/cbl/CBACT01C.cbl:45`, `app/jcl/READACCT.jcl:45` | DD VBRCFILE in READACCT.STEP05, SEQUENTIAL |
| `CBACT02C` | reads | `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS` | dataset | PROVEN | `app/cbl/CBACT02C.cbl:29`, `app/jcl/READCARD.jcl:25` | DD CARDFILE in READCARD.STEP05, INDEXED |
| `CBACT03C` | reads | `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS` | dataset | PROVEN | `app/cbl/CBACT03C.cbl:29`, `app/jcl/READXREF.jcl:25` | DD XREFFILE in READXREF.STEP05, INDEXED |
| `CBACT04C` | reads | `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS` | dataset | PROVEN | `app/cbl/CBACT04C.cbl:28`, `app/jcl/INTCALC.jcl:27` | DD TCATBALF in INTCALC.STEP15, INDEXED |
| `CBACT04C` | reads | `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS` | dataset | PROVEN | `app/cbl/CBACT04C.cbl:34`, `app/jcl/INTCALC.jcl:29` | DD XREFFILE in INTCALC.STEP15, INDEXED |
| `CBACT04C` | updates | `AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS` | dataset | PROVEN | `app/cbl/CBACT04C.cbl:41`, `app/jcl/INTCALC.jcl:33` | DD ACCTFILE in INTCALC.STEP15, INDEXED |
| `CBACT04C` | reads | `AWS.M2.CARDDEMO.DISCGRP.VSAM.KSDS` | dataset | PROVEN | `app/cbl/CBACT04C.cbl:47`, `app/jcl/INTCALC.jcl:35` | DD DISCGRP in INTCALC.STEP15, INDEXED |
| `CBACT04C` | writes | `AWS.M2.CARDDEMO.SYSTRAN(+1)` | dataset | PROVEN | `app/cbl/CBACT04C.cbl:53`, `app/jcl/INTCALC.jcl:37` | DD TRANSACT in INTCALC.STEP15, SEQUENTIAL |
| `CBCUS01C` | reads | `AWS.M2.CARDDEMO.CUSTDATA.VSAM.KSDS` | dataset | PROVEN | `app/cbl/CBCUS01C.cbl:29`, `app/jcl/READCUST.jcl:24` | DD CUSTFILE in READCUST.STEP05, INDEXED |
| `CBEXPORT` | reads | `AWS.M2.CARDDEMO.CUSTDATA.VSAM.KSDS` | dataset | PROVEN | `app/cbl/CBEXPORT.cbl:35`, `app/jcl/CBEXPORT.jcl:49` | DD CUSTFILE in CBEXPORT.STEP02, INDEXED |
| `CBEXPORT` | reads | `AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS` | dataset | PROVEN | `app/cbl/CBEXPORT.cbl:41`, `app/jcl/CBEXPORT.jcl:51` | DD ACCTFILE in CBEXPORT.STEP02, INDEXED |
| `CBEXPORT` | reads | `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS` | dataset | PROVEN | `app/cbl/CBEXPORT.cbl:47`, `app/jcl/CBEXPORT.jcl:53` | DD XREFFILE in CBEXPORT.STEP02, INDEXED |
| `CBEXPORT` | reads | `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS` | dataset | PROVEN | `app/cbl/CBEXPORT.cbl:53`, `app/jcl/CBEXPORT.jcl:55` | DD TRANSACT in CBEXPORT.STEP02, INDEXED |
| `CBEXPORT` | reads | `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS` | dataset | PROVEN | `app/cbl/CBEXPORT.cbl:59`, `app/jcl/CBEXPORT.jcl:57` | DD CARDFILE in CBEXPORT.STEP02, INDEXED |
| `CBEXPORT` | writes | `AWS.M2.CARDDEMO.EXPORT.DATA` | dataset | PROVEN | `app/cbl/CBEXPORT.cbl:65`, `app/jcl/CBEXPORT.jcl:62` | DD EXPFILE in CBEXPORT.STEP02, INDEXED |
| `CBFEEDRV` | reads | `AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS` | dataset | PROVEN | `app/cbl/CBFEEDRV.cbl:32`, `app/jcl/FEECALC.jcl:27` | DD ACCTFILE in FEECALC.STEP05, INDEXED |
| `CBFEEDRV` | writes | `AWS.M2.CARDDEMO.LATEFEES(+1)` | dataset | PROVEN | `app/cbl/CBFEEDRV.cbl:37`, `app/jcl/FEECALC.jcl:28` | DD FEEOUT in FEECALC.STEP05, SEQUENTIAL |
| `CBIMPORT` | reads | `AWS.M2.CARDDEMO.EXPORT.DATA` | dataset | PROVEN | `app/cbl/CBIMPORT.cbl:37`, `app/jcl/CBIMPORT.jcl:28` | DD EXPFILE in CBIMPORT.STEP01, INDEXED |
| `CBIMPORT` | writes | `AWS.M2.CARDDEMO.CUSTDATA.IMPORT` | dataset | PROVEN | `app/cbl/CBIMPORT.cbl:43`, `app/jcl/CBIMPORT.jcl:33` | DD CUSTOUT in CBIMPORT.STEP01, SEQUENTIAL |
| `CBIMPORT` | writes | `AWS.M2.CARDDEMO.ACCTDATA.IMPORT` | dataset | PROVEN | `app/cbl/CBIMPORT.cbl:48`, `app/jcl/CBIMPORT.jcl:38` | DD ACCTOUT in CBIMPORT.STEP01, SEQUENTIAL |
| `CBIMPORT` | writes | `AWS.M2.CARDDEMO.CARDXREF.IMPORT` | dataset | PROVEN | `app/cbl/CBIMPORT.cbl:53`, `app/jcl/CBIMPORT.jcl:43` | DD XREFOUT in CBIMPORT.STEP01, SEQUENTIAL |
| `CBIMPORT` | writes | `AWS.M2.CARDDEMO.TRANSACT.IMPORT` | dataset | PROVEN | `app/cbl/CBIMPORT.cbl:58`, `app/jcl/CBIMPORT.jcl:48` | DD TRNXOUT in CBIMPORT.STEP01, SEQUENTIAL |
| `CBIMPORT` | writes | `DD CARDOUT (missing)` | dataset | PROVEN | `app/cbl/CBIMPORT.cbl:63`, `app/jcl/CBIMPORT.jcl:22` | DD CARDOUT in CBIMPORT.STEP01, SEQUENTIAL |
| `CBIMPORT` | writes | `AWS.M2.CARDDEMO.IMPORT.ERRORS` | dataset | PROVEN | `app/cbl/CBIMPORT.cbl:68`, `app/jcl/CBIMPORT.jcl:56` | DD ERROUT in CBIMPORT.STEP01, SEQUENTIAL |
| `CBSTM03A` | writes | `AWS.M2.CARDDEMO.STATEMNT.PS` | dataset | PROVEN | `app/cbl/CBSTM03A.CBL:39`, `app/jcl/CREASTMT.JCL:87` | DD STMTFILE in CREASTMT.STEP040, SEQUENTIAL |
| `CBSTM03A` | writes | `AWS.M2.CARDDEMO.STATEMNT.HTML` | dataset | PROVEN | `app/cbl/CBSTM03A.CBL:40`, `app/jcl/CREASTMT.JCL:92` | DD HTMLFILE in CREASTMT.STEP040, SEQUENTIAL |
| `CBSTM03B` | reads | `AWS.M2.CARDDEMO.TRXFL.VSAM.KSDS` | dataset | PROVEN | `app/cbl/CBSTM03B.CBL:31`, `app/jcl/CREASTMT.JCL:83` | DD TRNXFILE in CREASTMT.STEP040, INDEXED |
| `CBSTM03B` | reads | `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS` | dataset | PROVEN | `app/cbl/CBSTM03B.CBL:37`, `app/jcl/CREASTMT.JCL:84` | DD XREFFILE in CREASTMT.STEP040, INDEXED |
| `CBSTM03B` | reads | `AWS.M2.CARDDEMO.CUSTDATA.VSAM.KSDS` | dataset | PROVEN | `app/cbl/CBSTM03B.CBL:43`, `app/jcl/CREASTMT.JCL:86` | DD CUSTFILE in CREASTMT.STEP040, INDEXED |
| `CBSTM03B` | reads | `AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS` | dataset | PROVEN | `app/cbl/CBSTM03B.CBL:49`, `app/jcl/CREASTMT.JCL:85` | DD ACCTFILE in CREASTMT.STEP040, INDEXED |
| `CBTRN02C` | reads | `AWS.M2.CARDDEMO.DALYTRAN.PS` | dataset | PROVEN | `app/cbl/CBTRN02C.cbl:29`, `app/jcl/POSTTRAN.jcl:30` | DD DALYTRAN in POSTTRAN.STEP15, SEQUENTIAL |
| `CBTRN02C` | writes | `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS` | dataset | PROVEN | `app/cbl/CBTRN02C.cbl:34`, `app/jcl/POSTTRAN.jcl:28` | DD TRANFILE in POSTTRAN.STEP15, INDEXED |
| `CBTRN02C` | reads | `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS` | dataset | PROVEN | `app/cbl/CBTRN02C.cbl:40`, `app/jcl/POSTTRAN.jcl:32` | DD XREFFILE in POSTTRAN.STEP15, INDEXED |
| `CBTRN02C` | writes | `AWS.M2.CARDDEMO.DALYREJS(+1)` | dataset | PROVEN | `app/cbl/CBTRN02C.cbl:46`, `app/jcl/POSTTRAN.jcl:34` | DD DALYREJS in POSTTRAN.STEP15, SEQUENTIAL |
| `CBTRN02C` | updates | `AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS` | dataset | PROVEN | `app/cbl/CBTRN02C.cbl:51`, `app/jcl/POSTTRAN.jcl:39` | DD ACCTFILE in POSTTRAN.STEP15, INDEXED |
| `CBTRN02C` | updates | `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS` | dataset | PROVEN | `app/cbl/CBTRN02C.cbl:57`, `app/jcl/POSTTRAN.jcl:41` | DD TCATBALF in POSTTRAN.STEP15, INDEXED |
| `CBTRN03C` | reads | `AWS.M2.CARDDEMO.TRANSACT.DALY(+1)` | dataset | PROVEN | `app/cbl/CBTRN03C.cbl:29`, `app/jcl/TRANREPT.jcl:65` | DD TRANFILE in TRANREPT.STEP10R, SEQUENTIAL |
| `CBTRN03C` | reads | `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS` | dataset | PROVEN | `app/cbl/CBTRN03C.cbl:33`, `app/jcl/TRANREPT.jcl:67` | DD CARDXREF in TRANREPT.STEP10R, INDEXED |
| `CBTRN03C` | reads | `AWS.M2.CARDDEMO.TRANTYPE.VSAM.KSDS` | dataset | PROVEN | `app/cbl/CBTRN03C.cbl:39`, `app/jcl/TRANREPT.jcl:69` | DD TRANTYPE in TRANREPT.STEP10R, INDEXED |
| `CBTRN03C` | reads | `AWS.M2.CARDDEMO.TRANCATG.VSAM.KSDS` | dataset | PROVEN | `app/cbl/CBTRN03C.cbl:45`, `app/jcl/TRANREPT.jcl:71` | DD TRANCATG in TRANREPT.STEP10R, INDEXED |
| `CBTRN03C` | writes | `AWS.M2.CARDDEMO.TRANREPT(+1)` | dataset | PROVEN | `app/cbl/CBTRN03C.cbl:51`, `app/jcl/TRANREPT.jcl:76` | DD TRANREPT in TRANREPT.STEP10R, SEQUENTIAL |
| `CBTRN03C` | reads | `AWS.M2.CARDDEMO.DATEPARM` | dataset | PROVEN | `app/cbl/CBTRN03C.cbl:55`, `app/jcl/TRANREPT.jcl:73` | DD DATEPARM in TRANREPT.STEP10R, SEQUENTIAL |
| `COACTUPC` | reads | `ACCTDAT` | CICS file | PROVEN | `app/cbl/COACTUPC.cbl:3703`, `app/cbl/COACTUPC.cbl:3894` | EXEC CICS READ |
| `COACTUPC` | updates | `ACCTDAT` | CICS file | PROVEN | `app/cbl/COACTUPC.cbl:4065` | EXEC CICS REWRITE |
| `COACTVWC` | reads | `ACCTDAT` | CICS file | PROVEN | `app/cbl/COACTVWC.cbl:776` | EXEC CICS READ |
| `COBIL00C` | reads | `ACCTDAT` | CICS file | PROVEN | `app/cbl/COBIL00C.cbl:345` | EXEC CICS READ |
| `COBIL00C` | updates | `ACCTDAT` | CICS file | PROVEN | `app/cbl/COBIL00C.cbl:379` | EXEC CICS REWRITE |
| `COCRDSLC` | reads | `CARDAIX` | CICS file | PROVEN | `app/cbl/COCRDSLC.cbl:783` | EXEC CICS READ |
| `COCRDLIC` | reads | `CARDDAT` | CICS file | PROVEN | `app/cbl/COCRDLIC.cbl:1129`, `app/cbl/COCRDLIC.cbl:1146` (+6 more) | EXEC CICS ENDBR, READNEXT, READPREV, STARTBR |
| `COCRDSLC` | reads | `CARDDAT` | CICS file | PROVEN | `app/cbl/COCRDSLC.cbl:742` | EXEC CICS READ |
| `COCRDUPC` | reads | `CARDDAT` | CICS file | PROVEN | `app/cbl/COCRDUPC.cbl:1382`, `app/cbl/COCRDUPC.cbl:1427` | EXEC CICS READ |
| `COCRDUPC` | updates | `CARDDAT` | CICS file | PROVEN | `app/cbl/COCRDUPC.cbl:1477` | EXEC CICS REWRITE |
| `COTRN02C` | reads | `CCXREF` | CICS file | PROVEN | `app/cbl/COTRN02C.cbl:611` | EXEC CICS READ |
| `COACTUPC` | reads | `CUSTDAT` | CICS file | PROVEN | `app/cbl/COACTUPC.cbl:3753`, `app/cbl/COACTUPC.cbl:3921` | EXEC CICS READ |
| `COACTUPC` | updates | `CUSTDAT` | CICS file | PROVEN | `app/cbl/COACTUPC.cbl:4085` | EXEC CICS REWRITE |
| `COACTVWC` | reads | `CUSTDAT` | CICS file | PROVEN | `app/cbl/COACTVWC.cbl:826` | EXEC CICS READ |
| `COACTUPC` | reads | `CXACAIX` | CICS file | PROVEN | `app/cbl/COACTUPC.cbl:3654` | EXEC CICS READ |
| `COACTVWC` | reads | `CXACAIX` | CICS file | PROVEN | `app/cbl/COACTVWC.cbl:727` | EXEC CICS READ |
| `COBIL00C` | reads | `CXACAIX` | CICS file | PROVEN | `app/cbl/COBIL00C.cbl:410` | EXEC CICS READ |
| `COTRN02C` | reads | `CXACAIX` | CICS file | PROVEN | `app/cbl/COTRN02C.cbl:578` | EXEC CICS READ |
| `COBIL00C` | reads | `TRANSACT` | CICS file | PROVEN | `app/cbl/COBIL00C.cbl:443`, `app/cbl/COBIL00C.cbl:474` (+1 more) | EXEC CICS ENDBR, READPREV, STARTBR |
| `COBIL00C` | writes | `TRANSACT` | CICS file | PROVEN | `app/cbl/COBIL00C.cbl:512` | EXEC CICS WRITE |
| `COTRN00C` | reads | `TRANSACT` | CICS file | PROVEN | `app/cbl/COTRN00C.cbl:593`, `app/cbl/COTRN00C.cbl:626` (+2 more) | EXEC CICS ENDBR, READNEXT, READPREV, STARTBR |
| `COTRN01C` | reads | `TRANSACT` | CICS file | PROVEN | `app/cbl/COTRN01C.cbl:269` | EXEC CICS READ |
| `COTRN02C` | reads | `TRANSACT` | CICS file | PROVEN | `app/cbl/COTRN02C.cbl:644`, `app/cbl/COTRN02C.cbl:675` (+1 more) | EXEC CICS ENDBR, READPREV, STARTBR |
| `COTRN02C` | writes | `TRANSACT` | CICS file | PROVEN | `app/cbl/COTRN02C.cbl:713` | EXEC CICS WRITE |
| `COSGN00C` | reads | `USRSEC` | CICS file | PROVEN | `app/cbl/COSGN00C.cbl:211` | EXEC CICS READ |
| `COUSR00C` | reads | `USRSEC` | CICS file | PROVEN | `app/cbl/COUSR00C.cbl:588`, `app/cbl/COUSR00C.cbl:621` (+2 more) | EXEC CICS ENDBR, READNEXT, READPREV, STARTBR |
| `COUSR01C` | writes | `USRSEC` | CICS file | PROVEN | `app/cbl/COUSR01C.cbl:240` | EXEC CICS WRITE |
| `COUSR02C` | reads | `USRSEC` | CICS file | PROVEN | `app/cbl/COUSR02C.cbl:322` | EXEC CICS READ |
| `COUSR02C` | updates | `USRSEC` | CICS file | PROVEN | `app/cbl/COUSR02C.cbl:360` | EXEC CICS REWRITE |
| `COUSR03C` | reads | `USRSEC` | CICS file | PROVEN | `app/cbl/COUSR03C.cbl:269` | EXEC CICS READ |
| `COUSR03C` | updates | `USRSEC` | CICS file | PROVEN | `app/cbl/COUSR03C.cbl:307` | EXEC CICS DELETE |
| `CORPT00C` | writes | `JOBS` | TD queue | PROVEN | `app/cbl/CORPT00C.cbl:517` | EXEC CICS WRITEQ TD |
| `COACTUPC` | reads (RECEIVE) | `COACTUP` | map | PROVEN | `app/cbl/COACTUPC.cbl:1040` | EXEC CICS RECEIVE MAP |
| `COACTVWC` | reads (RECEIVE) | `COACTVW` | map | PROVEN | `app/cbl/COACTVWC.cbl:611` | EXEC CICS RECEIVE MAP |
| `COADM01C` | writes (SEND) | `COADM01` | map | PROVEN | `app/cbl/COADM01C.cbl:182` | EXEC CICS SEND MAP |
| `COADM01C` | reads (RECEIVE) | `COADM01` | map | PROVEN | `app/cbl/COADM01C.cbl:194` | EXEC CICS RECEIVE MAP |
| `COBIL00C` | writes (SEND) | `COBIL00` | map | PROVEN | `app/cbl/COBIL00C.cbl:295` | EXEC CICS SEND MAP |
| `COBIL00C` | reads (RECEIVE) | `COBIL00` | map | PROVEN | `app/cbl/COBIL00C.cbl:308` | EXEC CICS RECEIVE MAP |
| `COCRDLIC` | writes (SEND) | `COCRDLI` | map | PROVEN | `app/cbl/COCRDLIC.cbl:939` | EXEC CICS SEND MAP |
| `COCRDLIC` | reads (RECEIVE) | `COCRDLI` | map | PROVEN | `app/cbl/COCRDLIC.cbl:963` | EXEC CICS RECEIVE MAP |
| `COCRDSLC` | reads (RECEIVE) | `COCRDSL` | map | PROVEN | `app/cbl/COCRDSLC.cbl:597` | EXEC CICS RECEIVE MAP |
| `COCRDUPC` | reads (RECEIVE) | `COCRDUP` | map | PROVEN | `app/cbl/COCRDUPC.cbl:579` | EXEC CICS RECEIVE MAP |
| `COMEN01C` | writes (SEND) | `COMEN01` | map | PROVEN | `app/cbl/COMEN01C.cbl:215` | EXEC CICS SEND MAP |
| `COMEN01C` | reads (RECEIVE) | `COMEN01` | map | PROVEN | `app/cbl/COMEN01C.cbl:227` | EXEC CICS RECEIVE MAP |
| `CORPT00C` | writes (SEND) | `CORPT00` | map | PROVEN | `app/cbl/CORPT00C.cbl:563`, `app/cbl/CORPT00C.cbl:571` | EXEC CICS SEND MAP |
| `CORPT00C` | reads (RECEIVE) | `CORPT00` | map | PROVEN | `app/cbl/CORPT00C.cbl:598` | EXEC CICS RECEIVE MAP |
| `COSGN00C` | reads (RECEIVE) | `COSGN00` | map | PROVEN | `app/cbl/COSGN00C.cbl:110` | EXEC CICS RECEIVE MAP |
| `COSGN00C` | writes (SEND) | `COSGN00` | map | PROVEN | `app/cbl/COSGN00C.cbl:151` | EXEC CICS SEND MAP |
| `COTRN00C` | writes (SEND) | `COTRN00` | map | PROVEN | `app/cbl/COTRN00C.cbl:534`, `app/cbl/COTRN00C.cbl:542` | EXEC CICS SEND MAP |
| `COTRN00C` | reads (RECEIVE) | `COTRN00` | map | PROVEN | `app/cbl/COTRN00C.cbl:556` | EXEC CICS RECEIVE MAP |
| `COTRN01C` | writes (SEND) | `COTRN01` | map | PROVEN | `app/cbl/COTRN01C.cbl:219` | EXEC CICS SEND MAP |
| `COTRN01C` | reads (RECEIVE) | `COTRN01` | map | PROVEN | `app/cbl/COTRN01C.cbl:232` | EXEC CICS RECEIVE MAP |
| `COTRN02C` | writes (SEND) | `COTRN02` | map | PROVEN | `app/cbl/COTRN02C.cbl:522` | EXEC CICS SEND MAP |
| `COTRN02C` | reads (RECEIVE) | `COTRN02` | map | PROVEN | `app/cbl/COTRN02C.cbl:541` | EXEC CICS RECEIVE MAP |
| `COUSR00C` | writes (SEND) | `COUSR00` | map | PROVEN | `app/cbl/COUSR00C.cbl:529`, `app/cbl/COUSR00C.cbl:537` | EXEC CICS SEND MAP |
| `COUSR00C` | reads (RECEIVE) | `COUSR00` | map | PROVEN | `app/cbl/COUSR00C.cbl:551` | EXEC CICS RECEIVE MAP |
| `COUSR01C` | writes (SEND) | `COUSR01` | map | PROVEN | `app/cbl/COUSR01C.cbl:190` | EXEC CICS SEND MAP |
| `COUSR01C` | reads (RECEIVE) | `COUSR01` | map | PROVEN | `app/cbl/COUSR01C.cbl:203` | EXEC CICS RECEIVE MAP |
| `COUSR02C` | writes (SEND) | `COUSR02` | map | PROVEN | `app/cbl/COUSR02C.cbl:272` | EXEC CICS SEND MAP |
| `COUSR02C` | reads (RECEIVE) | `COUSR02` | map | PROVEN | `app/cbl/COUSR02C.cbl:285` | EXEC CICS RECEIVE MAP |
| `COUSR03C` | writes (SEND) | `COUSR03` | map | PROVEN | `app/cbl/COUSR03C.cbl:219` | EXEC CICS SEND MAP |
| `COUSR03C` | reads (RECEIVE) | `COUSR03` | map | PROVEN | `app/cbl/COUSR03C.cbl:232` | EXEC CICS RECEIVE MAP |
| `ACCTDAT` | defined as | `AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS` | CSD FILE to dataset | PROVEN | `app/csd/CARDDEMO.CSD:1` |  |
| `CARDAIX` | defined as | `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX.PATH` | CSD FILE to dataset | PROVEN | `app/csd/CARDDEMO.CSD:13` |  |
| `CARDDAT` | defined as | `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS` | CSD FILE to dataset | PROVEN | `app/csd/CARDDEMO.CSD:25` |  |
| `CCXREF` | defined as | `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS` | CSD FILE to dataset | PROVEN | `app/csd/CARDDEMO.CSD:37` |  |
| `CUSTDAT` | defined as | `AWS.M2.CARDDEMO.CUSTDATA.VSAM.KSDS` | CSD FILE to dataset | PROVEN | `app/csd/CARDDEMO.CSD:50` |  |
| `CXACAIX` | defined as | `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX.PATH` | CSD FILE to dataset | PROVEN | `app/csd/CARDDEMO.CSD:63` |  |
| `TRANSACT` | defined as | `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS` | CSD FILE to dataset | PROVEN | `app/csd/CARDDEMO.CSD:76` |  |
| `USRSEC` | defined as | `AWS.M2.CARDDEMO.USRSEC.VSAM.KSDS` | CSD FILE to dataset | PROVEN | `app/csd/CARDDEMO.CSD:88` |  |
| `PAUDBLOD` | reads | `AWS.M2.CARDDEMO.PAUTDB.ROOT.FILEO` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/cbl/PAUDBLOD.CBL:26`, `app/app-authorization-ims-db2-mq/jcl/LOADPADB.JCL:36` | DD INFILE1 in LOADPADB.STEP01, SEQUENTIAL |
| `PAUDBLOD` | reads | `AWS.M2.CARDDEMO.PAUTDB.CHILD.FILEO` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/cbl/PAUDBLOD.CBL:32`, `app/app-authorization-ims-db2-mq/jcl/LOADPADB.JCL:38` | DD INFILE2 in LOADPADB.STEP01, SEQUENTIAL |
| `PAUDBUNL` | writes | `AWS.M2.CARDDEMO.PAUTDB.ROOT.FILEO` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/cbl/PAUDBUNL.CBL:26`, `app/app-authorization-ims-db2-mq/jcl/UNLDPADB.JCL:48` | DD OUTFIL1 in UNLDPADB.STEP01, SEQUENTIAL |
| `PAUDBUNL` | writes | `AWS.M2.CARDDEMO.PAUTDB.CHILD.FILEO` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/cbl/PAUDBUNL.CBL:32`, `app/app-authorization-ims-db2-mq/jcl/UNLDPADB.JCL:53` | DD OUTFIL2 in UNLDPADB.STEP01, SEQUENTIAL |
| `COPAUA0C` | reads | `ACCTDAT` | CICS file | PROVEN | `app/app-authorization-ims-db2-mq/cbl/COPAUA0C.cbl:525` | EXEC CICS READ |
| `COPAUS0C` | reads | `ACCTDAT` | CICS file | PROVEN | `app/app-authorization-ims-db2-mq/cbl/COPAUS0C.cbl:869` | EXEC CICS READ |
| `COPAUA0C` | reads | `CCXREF` | CICS file | PROVEN | `app/app-authorization-ims-db2-mq/cbl/COPAUA0C.cbl:477` | EXEC CICS READ |
| `COPAUA0C` | reads | `CUSTDAT` | CICS file | PROVEN | `app/app-authorization-ims-db2-mq/cbl/COPAUA0C.cbl:573` | EXEC CICS READ |
| `COPAUS0C` | reads | `CUSTDAT` | CICS file | PROVEN | `app/app-authorization-ims-db2-mq/cbl/COPAUS0C.cbl:920` | EXEC CICS READ |
| `COPAUS0C` | reads | `CXACAIX` | CICS file | PROVEN | `app/app-authorization-ims-db2-mq/cbl/COPAUS0C.cbl:818` | EXEC CICS READ |
| `COPAUA0C` | writes | `CSSL` | TD queue | PROVEN | `app/app-authorization-ims-db2-mq/cbl/COPAUA0C.cbl:1001` | EXEC CICS WRITEQ TD |
| `COPAUS0C` | writes (SEND) | `COPAU00` | map | PROVEN | `app/app-authorization-ims-db2-mq/cbl/COPAUS0C.cbl:695`, `app/app-authorization-ims-db2-mq/cbl/COPAUS0C.cbl:703` | EXEC CICS SEND MAP |
| `COPAUS0C` | reads (RECEIVE) | `COPAU00` | map | PROVEN | `app/app-authorization-ims-db2-mq/cbl/COPAUS0C.cbl:715` | EXEC CICS RECEIVE MAP |
| `COPAUS1C` | writes (SEND) | `COPAU01` | map | PROVEN | `app/app-authorization-ims-db2-mq/cbl/COPAUS1C.cbl:381`, `app/app-authorization-ims-db2-mq/cbl/COPAUS1C.cbl:389` | EXEC CICS SEND MAP |
| `COPAUS1C` | reads (RECEIVE) | `COPAU01` | map | PROVEN | `app/app-authorization-ims-db2-mq/cbl/COPAUS1C.cbl:400` | EXEC CICS RECEIVE MAP |
| `COPAUS2C` | writes+updates | `CARDDEMO.AUTHFRDS` | Db2 table | PROVEN | `app/app-authorization-ims-db2-mq/cbl/COPAUS2C.cbl:141-198`, `app/app-authorization-ims-db2-mq/cbl/COPAUS2C.cbl:222-229` | SQL INSERT, UPDATE |
| `CBPAUP0C` | reads | `IMS segment PAUTSUM0` | IMS segment | PROVEN | `app/app-authorization-ims-db2-mq/cbl/CBPAUP0C.cbl:223` | DL/I GN |
| `CBPAUP0C` | reads | `IMS segment PAUTDTL1` | IMS segment | PROVEN | `app/app-authorization-ims-db2-mq/cbl/CBPAUP0C.cbl:255` | DL/I GNP |
| `CBPAUP0C` | updates | `IMS segment PAUTDTL1` | IMS segment | PROVEN | `app/app-authorization-ims-db2-mq/cbl/CBPAUP0C.cbl:310` | DL/I DLET |
| `CBPAUP0C` | updates | `IMS segment PAUTSUM0` | IMS segment | PROVEN | `app/app-authorization-ims-db2-mq/cbl/CBPAUP0C.cbl:335` | DL/I DLET |
| `COPAUA0C` | reads | `IMS segment PAUTSUM0` | IMS segment | PROVEN | `app/app-authorization-ims-db2-mq/cbl/COPAUA0C.cbl:620` | DL/I GU |
| `COPAUA0C` | updates | `IMS segment PAUTSUM0` | IMS segment | PROVEN | `app/app-authorization-ims-db2-mq/cbl/COPAUA0C.cbl:825` | DL/I REPL |
| `COPAUA0C` | writes | `IMS segment PAUTSUM0` | IMS segment | PROVEN | `app/app-authorization-ims-db2-mq/cbl/COPAUA0C.cbl:830`, `app/app-authorization-ims-db2-mq/cbl/COPAUA0C.cbl:913` | DL/I ISRT |
| `COPAUS0C` | reads | `IMS segment PAUTDTL1` | IMS segment | PROVEN | `app/app-authorization-ims-db2-mq/cbl/COPAUS0C.cbl:461`, `app/app-authorization-ims-db2-mq/cbl/COPAUS0C.cbl:493` | DL/I GNP |
| `COPAUS0C` | reads | `IMS segment PAUTSUM0` | IMS segment | PROVEN | `app/app-authorization-ims-db2-mq/cbl/COPAUS0C.cbl:973` | DL/I GU |
| `COPAUS1C` | reads | `IMS segment PAUTSUM0` | IMS segment | PROVEN | `app/app-authorization-ims-db2-mq/cbl/COPAUS1C.cbl:439` | DL/I GU |
| `COPAUS1C` | reads | `IMS segment PAUTDTL1` | IMS segment | PROVEN | `app/app-authorization-ims-db2-mq/cbl/COPAUS1C.cbl:465`, `app/app-authorization-ims-db2-mq/cbl/COPAUS1C.cbl:495` | DL/I GNP |
| `COPAUS1C` | updates | `IMS segment PAUTDTL1` | IMS segment | PROVEN | `app/app-authorization-ims-db2-mq/cbl/COPAUS1C.cbl:525` | DL/I REPL |
| `CBTRNDYN` | reads | `DYNSQL` | dataset | PROVEN | `app/app-transaction-type-db2/cbl/CBTRNDYN.cbl:34`, `app/app-transaction-type-db2/jcl/MNTTRDYN.jcl:29` | DD DYNSQL in MNTTRDYN.STEP1, SEQUENTIAL |
| `COBTUPDT` | reads | `INPFILE` | dataset | PROVEN | `app/app-transaction-type-db2/cbl/COBTUPDT.cbl:31`, `app/app-transaction-type-db2/jcl/MNTTRDB2.jcl:27` | DD INPFILE in MNTTRDB2.STEP1, SEQUENTIAL |
| `COTRTLIC` | reads (RECEIVE) | `COTRTLI` | map | PROVEN | `app/app-transaction-type-db2/cbl/COTRTLIC.cbl:931` | EXEC CICS RECEIVE MAP |
| `COTRTLIC` | writes (SEND) | `COTRTLI` | map | PROVEN | `app/app-transaction-type-db2/cbl/COTRTLIC.cbl:1588` | EXEC CICS SEND MAP |
| `COTRTUPC` | reads (RECEIVE) | `COTRTUP` | map | PROVEN | `app/app-transaction-type-db2/cbl/COTRTUPC.cbl:642` | EXEC CICS RECEIVE MAP |
| `COBTUPDT` | writes+updates | `CARDDEMO.TRANSACTION_TYPE` | Db2 table | PROVEN | `app/app-transaction-type-db2/dcl/DCLTRTYP.dcl:28-31`, `app/app-transaction-type-db2/cbl/COBTUPDT.cbl:137-148` (+2 more) | SQL DELETE, INSERT, UPDATE |
| `COTRTLIC` | reads+updates | `CARDDEMO.TRANSACTION_TYPE` | Db2 table | PROVEN | `app/app-transaction-type-db2/dcl/DCLTRTYP.dcl:28-31`, `app/app-transaction-type-db2/cbl/COTRTLIC.cbl:338-352` (+7 more) | SQL CURSOR, DELETE, FETCH, SELECT, UPDATE |
| `COTRTUPC` | reads+writes+updates | `CARDDEMO.TRANSACTION_TYPE` | Db2 table | PROVEN | `app/app-transaction-type-db2/dcl/DCLTRTYP.dcl:28-31`, `app/app-transaction-type-db2/cbl/COTRTUPC.cbl:1475-1482` (+3 more) | SQL DELETE, INSERT, SELECT, UPDATE |
| `COTRTLIC` | reads | `SYSIBM.SYSDUMMY1` | Db2 table | PROVEN | `app/app-transaction-type-db2/cpy/CSDB2RPY.cpy:23-28` | SQL SELECT |
| `COACCT01` | reads | `ACCTDAT` | CICS file | PROVEN | `app/app-vsam-mq/cbl/COACCT01.cbl:396` | EXEC CICS READ |

### 3.2 PROVEN - utility steps (163)

A utility step names the dataset on a DD; the direction shown is the JCL `DISP`, not a read/write claim.

| From | Direction | To | Store | Status | Cited at | Note |
|---|---|---|---|---|---|---|
| `READACCT.PREDEL (IEFBR14)` | DISP=(MOD,DELETE,DELETE) | `AWS.M2.CARDDEMO.ACCTDATA.ARRYPS` | dataset | PROVEN | `app/jcl/READACCT.jcl:25` | utility step, DD DD02; sequential (QSAM by DD DCB) |
| `ACCTFILE.STEP15 (IDCAMS)` | DISP=SHR | `AWS.M2.CARDDEMO.ACCTDATA.PS` | dataset | PROVEN | `app/jcl/ACCTFILE.jcl:56` | utility step, DD ACCTDATA; sequential (no catalog evidence) |
| `READACCT.PREDEL (IEFBR14)` | DISP=(MOD,DELETE,DELETE) | `AWS.M2.CARDDEMO.ACCTDATA.PSCOMP` | dataset | PROVEN | `app/jcl/READACCT.jcl:23` | utility step, DD DD01; sequential (QSAM by DD DCB) |
| `READACCT.PREDEL (IEFBR14)` | DISP=(MOD,DELETE,DELETE) | `AWS.M2.CARDDEMO.ACCTDATA.VBPS` | dataset | PROVEN | `app/jcl/READACCT.jcl:27` | utility step, DD DD03; sequential (QSAM by DD DCB) |
| `ACCTFILE.STEP15 (IDCAMS)` | DISP=SHR | `AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS` | dataset | PROVEN | `app/jcl/ACCTFILE.jcl:58` | utility step, DD ACCTVSAM; KSDS |
| `CARDFILE.STEP15 (IDCAMS)` | DISP=SHR | `AWS.M2.CARDDEMO.CARDDATA.PS` | dataset | PROVEN | `app/jcl/CARDFILE.jcl:70` | utility step, DD CARDDATA; sequential (no catalog evidence) |
| `CARDFILE.STEP15 (IDCAMS)` | DISP=SHR | `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS` | dataset | PROVEN | `app/jcl/CARDFILE.jcl:72` | utility step, DD CARDVSAM; KSDS |
| `XREFFILE.STEP15 (IDCAMS)` | DISP=SHR | `AWS.M2.CARDDEMO.CARDXREF.PS` | dataset | PROVEN | `app/jcl/XREFFILE.jcl:59` | utility step, DD XREFDATA; sequential (no catalog evidence) |
| `XREFFILE.STEP15 (IDCAMS)` | DISP=SHR | `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS` | dataset | PROVEN | `app/jcl/XREFFILE.jcl:61` | utility step, DD XREFVSAM; KSDS |
| `PRTCATBL.STEP05R (IDCAMS)` | DISP=SHR | `AWS.M2.CARDDEMO.CNTL` | dataset | PROVEN | `app/proc/REPROC.prc:27` | utility step, DD SYSIN; PDS member |
| `TRANBKP.STEP05R (IDCAMS)` | DISP=SHR | `AWS.M2.CARDDEMO.CNTL` | dataset | PROVEN | `app/proc/REPROC.prc:27` | utility step, DD SYSIN; PDS member |
| `TRANREPT.STEP05R (IDCAMS)` | DISP=SHR | `AWS.M2.CARDDEMO.CNTL` | dataset | PROVEN | `app/proc/REPROC.prc:27` | utility step, DD SYSIN; PDS member |
| `CUSTFILE.STEP15 (IDCAMS)` | DISP=SHR | `AWS.M2.CARDDEMO.CUSTDATA.PS` | dataset | PROVEN | `app/jcl/CUSTFILE.jcl:66` | utility step, DD CUSTDATA; sequential (no catalog evidence) |
| `CUSTFILE.STEP15 (IDCAMS)` | DISP=SHR | `AWS.M2.CARDDEMO.CUSTDATA.VSAM.KSDS` | dataset | PROVEN | `app/jcl/CUSTFILE.jcl:68` | utility step, DD CUSTVSAM; KSDS |
| `TRANFILE.STEP15 (IDCAMS)` | DISP=SHR | `AWS.M2.CARDDEMO.DALYTRAN.PS.INIT` | dataset | PROVEN | `app/jcl/TRANFILE.jcl:69` | utility step, DD TRANSACT; sequential (no catalog evidence) |
| `DEFGDGD.STEP60 (IEBGENER)` | DISP=(NEW,CATLG) | `AWS.M2.CARDDEMO.DISCGRP.BKUP` | dataset | PROVEN | `app/jcl/DEFGDGD.jcl:86` | utility step, DD SYSUT2; GDG |
| `DEFGDGD.STEP60 (IEBGENER)` | DISP=SHR | `AWS.M2.CARDDEMO.DISCGRP.PS` | dataset | PROVEN | `app/jcl/DEFGDGD.jcl:85` | utility step, DD SYSUT1; sequential (no catalog evidence) |
| `DISCGRP.STEP15 (IDCAMS)` | DISP=SHR | `AWS.M2.CARDDEMO.DISCGRP.PS` | dataset | PROVEN | `app/jcl/DISCGRP.jcl:56` | utility step, DD DISCGRP; sequential (no catalog evidence) |
| `DISCGRP.STEP15 (IDCAMS)` | DISP=OLD | `AWS.M2.CARDDEMO.DISCGRP.VSAM.KSDS` | dataset | PROVEN | `app/jcl/DISCGRP.jcl:58` | utility step, DD DISCVSAM; KSDS |
| `ESDSRRDS.PREDEL (IEFBR14)` | DISP=(MOD,DELETE,DELETE) | `AWS.M2.CARDDEMO.ESDSRRDS.PS` | dataset | PROVEN | `app/jcl/ESDSRRDS.jcl:26` | utility step, DD DD01; sequential (QSAM by DD DCB) |
| `ESDSRRDS.STEP01 (IEBGENER)` | DISP=(NEW,CATLG,DELETE) | `AWS.M2.CARDDEMO.ESDSRRDS.PS` | dataset | PROVEN | `app/jcl/ESDSRRDS.jcl:47` | utility step, DD SYSUT2; sequential (QSAM by DD DCB) |
| `ESDSRRDS.STEP03 (IDCAMS)` | DISP=SHR | `AWS.M2.CARDDEMO.ESDSRRDS.PS` | dataset | PROVEN | `app/jcl/ESDSRRDS.jcl:81` | utility step, DD IN; sequential (QSAM by DD DCB) |
| `ESDSRRDS.STEP05 (IDCAMS)` | DISP=SHR | `AWS.M2.CARDDEMO.ESDSRRDS.PS` | dataset | PROVEN | `app/jcl/ESDSRRDS.jcl:115` | utility step, DD IN; sequential (QSAM by DD DCB) |
| `INTRDRJ1.STEP01 (IEBGENER)` | DISP=SHR | `AWS.M2.CARDDEMO.JCL` | dataset | PROVEN | `app/jcl/INTRDRJ1.JCL:17` | utility step, DD SYSUT1; PDS member |
| `CREASTMT.STEP030 (IEFBR14)` | DISP=(MOD,DELETE,DELETE) | `AWS.M2.CARDDEMO.STATEMNT.HTML` | dataset | PROVEN | `app/jcl/CREASTMT.JCL:67` | utility step, DD HTMLFILE; sequential (QSAM by DD DCB) |
| `CREASTMT.STEP030 (IEFBR14)` | DISP=(MOD,DELETE,DELETE) | `AWS.M2.CARDDEMO.STATEMNT.PS` | dataset | PROVEN | `app/jcl/CREASTMT.JCL:72` | utility step, DD STMTFILE; sequential (QSAM by DD DCB) |
| `TXT2PDF1.TXT2PDF (IKJEFT1B)` | DISP=SHR | `AWS.M2.CARDDEMO.STATEMNT.PS` | dataset | PROVEN | `app/jcl/TXT2PDF1.JCL:33` | utility step, DD INDD; sequential (QSAM by DD DCB) |
| `COMBTRAN.STEP05R (SORT)` | DISP=SHR | `AWS.M2.CARDDEMO.SYSTRAN` | dataset | PROVEN | `app/jcl/COMBTRAN.jcl:25` | utility step, DD SORTIN; GDG |
| `PRTCATBL.STEP05R (IDCAMS)` | DISP=(NEW,CATLG,DELETE) | `AWS.M2.CARDDEMO.TCATBALF.BKUP` | dataset | PROVEN | `app/jcl/PRTCATBL.jcl:35` | utility step, DD FILEOUT; GDG |
| `PRTCATBL.STEP10R (SORT)` | DISP=SHR | `AWS.M2.CARDDEMO.TCATBALF.BKUP` | dataset | PROVEN | `app/jcl/PRTCATBL.jcl:44` | utility step, DD SORTIN; GDG |
| `TCATBALF.STEP15 (IDCAMS)` | DISP=SHR | `AWS.M2.CARDDEMO.TCATBALF.PS` | dataset | PROVEN | `app/jcl/TCATBALF.jcl:56` | utility step, DD TCATBAL; sequential (no catalog evidence) |
| `PRTCATBL.DELDEF (IEFBR14)` | DISP=(MOD,DELETE) | `AWS.M2.CARDDEMO.TCATBALF.REPT` | dataset | PROVEN | `app/jcl/PRTCATBL.jcl:22` | utility step, DD THEFILE; sequential (QSAM by DD DCB) |
| `PRTCATBL.STEP10R (SORT)` | DISP=(NEW,CATLG,DELETE) | `AWS.M2.CARDDEMO.TCATBALF.REPT` | dataset | PROVEN | `app/jcl/PRTCATBL.jcl:59` | utility step, DD SORTOUT; sequential (QSAM by DD DCB) |
| `PRTCATBL.STEP05R (IDCAMS)` | DISP=SHR | `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS` | dataset | PROVEN | `app/jcl/PRTCATBL.jcl:32` | utility step, DD FILEIN; KSDS |
| `TCATBALF.STEP15 (IDCAMS)` | DISP=OLD | `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS` | dataset | PROVEN | `app/jcl/TCATBALF.jcl:58` | utility step, DD TCATBALV; KSDS |
| `DEFGDGD.STEP40 (IEBGENER)` | DISP=SHR | `AWS.M2.CARDDEMO.TRANCATG.PS` | dataset | PROVEN | `app/jcl/DEFGDGD.jcl:62` | utility step, DD SYSUT1; sequential (no catalog evidence) |
| `TRANCATG.STEP15 (IDCAMS)` | DISP=SHR | `AWS.M2.CARDDEMO.TRANCATG.PS` | dataset | PROVEN | `app/jcl/TRANCATG.jcl:56` | utility step, DD TRANCATG; sequential (no catalog evidence) |
| `DEFGDGD.STEP40 (IEBGENER)` | DISP=(NEW,CATLG) | `AWS.M2.CARDDEMO.TRANCATG.PS.BKUP` | dataset | PROVEN | `app/jcl/DEFGDGD.jcl:63` | utility step, DD SYSUT2; GDG |
| `TRANCATG.STEP15 (IDCAMS)` | DISP=OLD | `AWS.M2.CARDDEMO.TRANCATG.VSAM.KSDS` | dataset | PROVEN | `app/jcl/TRANCATG.jcl:58` | utility step, DD TCATVSAM; KSDS |
| `COMBTRAN.STEP05R (SORT)` | DISP=SHR | `AWS.M2.CARDDEMO.TRANSACT.BKUP` | dataset | PROVEN | `app/jcl/COMBTRAN.jcl:23` | utility step, DD SORTIN; GDG |
| `TRANBKP.STEP05R (IDCAMS)` | DISP=(NEW,CATLG,DELETE) | `AWS.M2.CARDDEMO.TRANSACT.BKUP` | dataset | PROVEN | `app/jcl/TRANBKP.jcl:29` | utility step, DD FILEOUT; GDG |
| `TRANREPT.STEP05R (IDCAMS)` | DISP=(NEW,CATLG,DELETE) | `AWS.M2.CARDDEMO.TRANSACT.BKUP` | dataset | PROVEN | `app/jcl/TRANREPT.jcl:29` | utility step, DD FILEOUT; GDG |
| `TRANREPT.STEP05R (SORT)` | DISP=SHR | `AWS.M2.CARDDEMO.TRANSACT.BKUP` | dataset | PROVEN | `app/jcl/TRANREPT.jcl:38` | utility step, DD SORTIN; GDG |
| `COMBTRAN.STEP05R (SORT)` | DISP=(NEW,CATLG,DELETE) | `AWS.M2.CARDDEMO.TRANSACT.COMBINED` | dataset | PROVEN | `app/jcl/COMBTRAN.jcl:33` | utility step, DD SORTOUT; GDG |
| `COMBTRAN.STEP10 (IDCAMS)` | DISP=SHR | `AWS.M2.CARDDEMO.TRANSACT.COMBINED` | dataset | PROVEN | `app/jcl/COMBTRAN.jcl:43` | utility step, DD TRANSACT; GDG |
| `TRANREPT.STEP05R (SORT)` | DISP=(NEW,CATLG,DELETE) | `AWS.M2.CARDDEMO.TRANSACT.DALY` | dataset | PROVEN | `app/jcl/TRANREPT.jcl:51` | utility step, DD SORTOUT; GDG |
| `COMBTRAN.STEP10 (IDCAMS)` | DISP=SHR | `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS` | dataset | PROVEN | `app/jcl/COMBTRAN.jcl:45` | utility step, DD TRANVSAM; KSDS |
| `CREASTMT.STEP010 (SORT)` | DISP=SHR | `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS` | dataset | PROVEN | `app/jcl/CREASTMT.JCL:45` | utility step, DD SORTIN; KSDS |
| `TRANBKP.STEP05R (IDCAMS)` | DISP=SHR | `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS` | dataset | PROVEN | `app/jcl/TRANBKP.jcl:26` | utility step, DD FILEIN; KSDS |
| `TRANFILE.STEP15 (IDCAMS)` | DISP=SHR | `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS` | dataset | PROVEN | `app/jcl/TRANFILE.jcl:71` | utility step, DD TRANVSAM; KSDS |
| `TRANREPT.STEP05R (IDCAMS)` | DISP=SHR | `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS` | dataset | PROVEN | `app/jcl/TRANREPT.jcl:26` | utility step, DD FILEIN; KSDS |
| `DEFGDGD.STEP20 (IEBGENER)` | DISP=(NEW,CATLG) | `AWS.M2.CARDDEMO.TRANTYPE.BKUP` | dataset | PROVEN | `app/jcl/DEFGDGD.jcl:40` | utility step, DD SYSUT2; GDG |
| `DEFGDGD.STEP20 (IEBGENER)` | DISP=SHR | `AWS.M2.CARDDEMO.TRANTYPE.PS` | dataset | PROVEN | `app/jcl/DEFGDGD.jcl:39` | utility step, DD SYSUT1; sequential (no catalog evidence) |
| `TRANTYPE.STEP15 (IDCAMS)` | DISP=SHR | `AWS.M2.CARDDEMO.TRANTYPE.PS` | dataset | PROVEN | `app/jcl/TRANTYPE.jcl:56` | utility step, DD TRANTYPE; sequential (no catalog evidence) |
| `TRANTYPE.STEP15 (IDCAMS)` | DISP=OLD | `AWS.M2.CARDDEMO.TRANTYPE.VSAM.KSDS` | dataset | PROVEN | `app/jcl/TRANTYPE.jcl:58` | utility step, DD TTYPVSAM; KSDS |
| `CREASTMT.STEP010 (SORT)` | DISP=(NEW,CATLG,DELETE) | `AWS.M2.CARDDEMO.TRXFL.SEQ` | dataset | PROVEN | `app/jcl/CREASTMT.JCL:48` | utility step, DD SORTOUT; sequential (QSAM by DD DCB) |
| `CREASTMT.STEP020 (IDCAMS)` | DISP=SHR | `AWS.M2.CARDDEMO.TRXFL.SEQ` | dataset | PROVEN | `app/jcl/CREASTMT.JCL:58` | utility step, DD INFILE; sequential (QSAM by DD DCB) |
| `CREASTMT.STEP020 (IDCAMS)` | DISP=SHR | `AWS.M2.CARDDEMO.TRXFL.VSAM.KSDS` | dataset | PROVEN | `app/jcl/CREASTMT.JCL:59` | utility step, DD OUTFILE; KSDS |
| `DUSRSECJ.PREDEL (IEFBR14)` | DISP=(MOD,DELETE,DELETE) | `AWS.M2.CARDDEMO.USRSEC.PS` | dataset | PROVEN | `app/jcl/DUSRSECJ.jcl:25` | utility step, DD DD01; sequential (QSAM by DD DCB) |
| `DUSRSECJ.STEP01 (IEBGENER)` | DISP=(NEW,CATLG,DELETE) | `AWS.M2.CARDDEMO.USRSEC.PS` | dataset | PROVEN | `app/jcl/DUSRSECJ.jcl:46` | utility step, DD SYSUT2; sequential (QSAM by DD DCB) |
| `DUSRSECJ.STEP03 (IDCAMS)` | DISP=SHR | `AWS.M2.CARDDEMO.USRSEC.PS` | dataset | PROVEN | `app/jcl/DUSRSECJ.jcl:82` | utility step, DD IN; sequential (QSAM by DD DCB) |
| `ESDSRRDS.STEP03 (IDCAMS)` | DISP=SHR | `AWS.M2.CARDDEMO.USRSEC.VSAM.ESDS` | dataset | PROVEN | `app/jcl/ESDSRRDS.jcl:82` | utility step, DD OUT; ESDS |
| `DUSRSECJ.STEP03 (IDCAMS)` | DISP=SHR | `AWS.M2.CARDDEMO.USRSEC.VSAM.KSDS` | dataset | PROVEN | `app/jcl/DUSRSECJ.jcl:83` | utility step, DD OUT; KSDS |
| `ESDSRRDS.STEP05 (IDCAMS)` | DISP=SHR | `AWS.M2.CARDDEMO.USRSEC.VSAM.RRDS` | dataset | PROVEN | `app/jcl/ESDSRRDS.jcl:116` | utility step, DD OUT; RRDS |
| `INTRDRJ1.IDCAMS (IDCAMS)` | DISP=SHR | `AWS.M2.CARDEMO.FTP.TEST` | dataset | PROVEN | `app/jcl/INTRDRJ1.JCL:8` | utility step, DD IN; sequential (no catalog evidence) |
| `INTRDRJ1.IDCAMS (IDCAMS)` | DISP=SHR | `AWS.M2.CARDEMO.FTP.TEST.BKUP` | dataset | PROVEN | `app/jcl/INTRDRJ1.JCL:9` | utility step, DD OUT; sequential (no catalog evidence) |
| `INTRDRJ2.IDCAMS (IDCAMS)` | DISP=SHR | `AWS.M2.CARDEMO.FTP.TEST.BKUP` | dataset | PROVEN | `app/jcl/INTRDRJ2.JCL:9` | utility step, DD IN; sequential (no catalog evidence) |
| `INTRDRJ2.IDCAMS (IDCAMS)` | DISP=SHR | `AWS.M2.CARDEMO.FTP.TEST.BKUP.INTRDR` | dataset | PROVEN | `app/jcl/INTRDRJ2.JCL:10` | utility step, DD OUT; sequential (no catalog evidence) |
| `TXT2PDF1.TXT2PDF (IKJEFT1B)` | DISP=SHR | `AWS.M2.LBD.TXT2PDF.EXEC` | dataset | PROVEN | `app/jcl/TXT2PDF1.JCL:31` | utility step, DD SYSEXEC; sequential (no catalog evidence) |
| `TXT2PDF1.TXT2PDF (IKJEFT1B)` | DISP=SHR | `AWS.M2.LBD.TXT2PDF.LOAD` | dataset | PROVEN | `app/jcl/TXT2PDF1.JCL:28` | utility step, DD STEPLIB; sequential (no catalog evidence) |
| `CBADMCDJ.STEP1 (DFHCSDUP)` | DISP=SHR | `OEM.CICSTS.DFHCSD` | dataset | PROVEN | `app/jcl/CBADMCDJ.jcl:30` | utility step, DD DFHCSD; sequential (no catalog evidence) |
| `CBADMCDJ.STEP1 (DFHCSDUP)` | DISP=SHR | `OEM.CICSTS.V05R06M0.CICS.SDFHLOAD` | dataset | PROVEN | `app/jcl/CBADMCDJ.jcl:29` | utility step, DD STEPLIB; sequential (no catalog evidence) |
| `DBPAUTP0.STEPDEL (IEFBR14)` | DISP=(MOD,DELETE) | `AWS.M2.CARDDEMO.IMSDATA.DBPAUTP0` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/DBPAUTP0.jcl:9` | utility step, DD SYSUT1; sequential (QSAM by DD DCB) |
| `DBPAUTP0.UNLOAD (DFSRRC00)` | DISP=(,CATLG,DELETE) | `AWS.M2.CARDDEMO.IMSDATA.DBPAUTP0` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/DBPAUTP0.jcl:25` | utility step, DD DFSURGU1; sequential (QSAM by DD DCB) |
| `DBPAUTP0.UNLOAD (DFSRRC00)` | DISP=SHR | `AWS.M2.CARDDEMO.LOADLIB` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/DBPAUTP0.jcl:19` | utility step, DD STEPLIB; sequential (no catalog evidence) |
| `LOADPADB.STEP01 (PAUDBLOD)` | DISP=SHR | `AWS.M2.CARDDEMO.LOADLIB` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/LOADPADB.JCL:30` | utility step, DD STEPLIB; sequential (no catalog evidence) |
| `UNLDGSAM.STEP01 (DBUNLDGS)` | DISP=SHR | `AWS.M2.CARDDEMO.LOADLIB` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/UNLDGSAM.JCL:30` | utility step, DD STEPLIB; sequential (no catalog evidence) |
| `UNLDPADB.STEP01 (PAUDBUNL)` | DISP=SHR | `AWS.M2.CARDDEMO.LOADLIB` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/UNLDPADB.JCL:42` | utility step, DD STEPLIB; sequential (no catalog evidence) |
| `LOADPADB.STEP01 (PAUDBLOD)` | DISP=SHR | `AWS.M2.CARDDEMO.PAUTDB.CHILD.FILEO` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/LOADPADB.JCL:38` | utility step, DD INFILE2; sequential (QSAM by DD DCB) |
| `UNLDPADB.STEP0 (IEFBR14)` | DISP=(OLD,DELETE,DELETE) | `AWS.M2.CARDDEMO.PAUTDB.CHILD.FILEO` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/UNLDPADB.JCL:35` | utility step, DD DD2; sequential (QSAM by DD DCB) |
| `UNLDPADB.STEP01 (PAUDBUNL)` | DISP=(NEW,CATLG,DELETE) | `AWS.M2.CARDDEMO.PAUTDB.CHILD.FILEO` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/UNLDPADB.JCL:53` | utility step, DD OUTFIL2; sequential (QSAM by DD DCB) |
| `UNLDGSAM.STEP01 (DBUNLDGS)` | DISP=(OLD,KEEP,KEEP) | `AWS.M2.CARDDEMO.PAUTDB.CHILD.GSAM` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/UNLDGSAM.JCL:39` | utility step, DD PADFILOP; sequential (no catalog evidence) |
| `LOADPADB.STEP01 (PAUDBLOD)` | DISP=SHR | `AWS.M2.CARDDEMO.PAUTDB.ROOT.FILEO` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/LOADPADB.JCL:36` | utility step, DD INFILE1; sequential (QSAM by DD DCB) |
| `UNLDPADB.STEP0 (IEFBR14)` | DISP=(OLD,DELETE,DELETE) | `AWS.M2.CARDDEMO.PAUTDB.ROOT.FILEO` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/UNLDPADB.JCL:33` | utility step, DD DD1; sequential (QSAM by DD DCB) |
| `UNLDPADB.STEP01 (PAUDBUNL)` | DISP=(NEW,CATLG,DELETE) | `AWS.M2.CARDDEMO.PAUTDB.ROOT.FILEO` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/UNLDPADB.JCL:48` | utility step, DD OUTFIL1; sequential (QSAM by DD DCB) |
| `UNLDGSAM.STEP01 (DBUNLDGS)` | DISP=(OLD,KEEP,KEEP) | `AWS.M2.CARDDEMO.PAUTDB.ROOT.GSAM` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/UNLDGSAM.JCL:36` | utility step, DD PASFILOP; sequential (no catalog evidence) |
| `CBPAUP0J.STEP01 (CBPAUP0C)` | DISP=SHR | `IMS.DBDLIB` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/CBPAUP0J.jcl:34` | utility step, DD IMS; sequential (no catalog evidence) |
| `CBPAUP0J.STEP01 (CBPAUP0C)` | DISP=SHR | `IMS.PROCLIB` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/CBPAUP0J.jcl:29` | utility step, DD PROCLIB; sequential (no catalog evidence) |
| `CBPAUP0J.STEP01 (CBPAUP0C)` | DISP=SHR | `IMS.PSBLIB` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/CBPAUP0J.jcl:33` | utility step, DD IMS; sequential (no catalog evidence) |
| `CBPAUP0J.STEP01 (CBPAUP0C)` | DISP=SHR | `IMS.SDFSRESL` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/CBPAUP0J.jcl:26` | utility step, DD STEPLIB; sequential (no catalog evidence) |
| `CBPAUP0J.STEP01 (CBPAUP0C)` | DISP=SHR | `IMS.SDFSRESL` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/CBPAUP0J.jcl:28` | utility step, DD DFSRESLB; sequential (no catalog evidence) |
| `CBPAUP0J.STEP01 (CBPAUP0C)` | DISP=SHR | `IMS.SDFSRESL` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/CBPAUP0J.jcl:31` | utility step, DD DFSSEL; sequential (no catalog evidence) |
| `DBPAUTP0.UNLOAD (DFSRRC00)` | DISP=SHR | `OEM.IMS.IMSP.DBDLIB` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/DBPAUTP0.jcl:22` | utility step, DD IMS; sequential (no catalog evidence) |
| `LOADPADB.STEP01 (PAUDBLOD)` | DISP=SHR | `OEM.IMS.IMSP.DBDLIB` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/LOADPADB.JCL:34` | utility step, DD IMS; sequential (no catalog evidence) |
| `UNLDGSAM.STEP01 (DBUNLDGS)` | DISP=SHR | `OEM.IMS.IMSP.DBDLIB` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/UNLDGSAM.JCL:34` | utility step, DD IMS; sequential (no catalog evidence) |
| `UNLDPADB.STEP01 (PAUDBUNL)` | DISP=SHR | `OEM.IMS.IMSP.DBDLIB` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/UNLDPADB.JCL:46` | utility step, DD IMS; sequential (no catalog evidence) |
| `DBPAUTP0.UNLOAD (DFSRRC00)` | DISP=SHR | `OEM.IMS.IMSP.PAUTHDB` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/DBPAUTP0.jcl:31` | utility step, DD DDPAUTP0; sequential (no catalog evidence) |
| `UNLDGSAM.STEP01 (DBUNLDGS)` | DISP=SHR | `OEM.IMS.IMSP.PAUTHDB` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/UNLDGSAM.JCL:42` | utility step, DD DDPAUTP0; sequential (no catalog evidence) |
| `UNLDPADB.STEP01 (PAUDBUNL)` | DISP=SHR | `OEM.IMS.IMSP.PAUTHDB` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/UNLDPADB.JCL:58` | utility step, DD DDPAUTP0; sequential (no catalog evidence) |
| `DBPAUTP0.UNLOAD (DFSRRC00)` | DISP=SHR | `OEM.IMS.IMSP.PAUTHDBX` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/DBPAUTP0.jcl:32` | utility step, DD DDPAUTX0; sequential (no catalog evidence) |
| `UNLDGSAM.STEP01 (DBUNLDGS)` | DISP=SHR | `OEM.IMS.IMSP.PAUTHDBX` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/UNLDGSAM.JCL:43` | utility step, DD DDPAUTX0; sequential (no catalog evidence) |
| `UNLDPADB.STEP01 (PAUDBUNL)` | DISP=SHR | `OEM.IMS.IMSP.PAUTHDBX` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/UNLDPADB.JCL:59` | utility step, DD DDPAUTX0; sequential (no catalog evidence) |
| `DBPAUTP0.UNLOAD (DFSRRC00)` | DISP=SHR | `OEM.IMS.IMSP.PSBLIB` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/DBPAUTP0.jcl:21` | utility step, DD IMS; sequential (no catalog evidence) |
| `LOADPADB.STEP01 (PAUDBLOD)` | DISP=SHR | `OEM.IMS.IMSP.PSBLIB` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/LOADPADB.JCL:33` | utility step, DD IMS; sequential (no catalog evidence) |
| `UNLDGSAM.STEP01 (DBUNLDGS)` | DISP=SHR | `OEM.IMS.IMSP.PSBLIB` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/UNLDGSAM.JCL:33` | utility step, DD IMS; sequential (no catalog evidence) |
| `UNLDPADB.STEP01 (PAUDBUNL)` | DISP=SHR | `OEM.IMS.IMSP.PSBLIB` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/UNLDPADB.JCL:45` | utility step, DD IMS; sequential (no catalog evidence) |
| `DBPAUTP0.UNLOAD (DFSRRC00)` | DISP=SHR | `OEM.IMS.IMSP.RECON1` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/DBPAUTP0.jcl:40` | utility step, DD RECON1; sequential (no catalog evidence) |
| `DBPAUTP0.UNLOAD (DFSRRC00)` | DISP=SHR | `OEM.IMS.IMSP.RECON2` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/DBPAUTP0.jcl:41` | utility step, DD RECON2; sequential (no catalog evidence) |
| `DBPAUTP0.UNLOAD (DFSRRC00)` | DISP=SHR | `OEM.IMS.IMSP.RECON3` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/DBPAUTP0.jcl:42` | utility step, DD RECON3; sequential (no catalog evidence) |
| `DBPAUTP0.UNLOAD (DFSRRC00)` | DISP=SHR | `OEMA.IMS.IMSP.SDFSRESL` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/DBPAUTP0.jcl:18` | utility step, DD STEPLIB; sequential (no catalog evidence) |
| `DBPAUTP0.UNLOAD (DFSRRC00)` | DISP=SHR | `OEMA.IMS.IMSP.SDFSRESL` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/DBPAUTP0.jcl:20` | utility step, DD DFSRESLB; sequential (no catalog evidence) |
| `LOADPADB.STEP01 (PAUDBLOD)` | DISP=SHR | `OEMA.IMS.IMSP.SDFSRESL` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/LOADPADB.JCL:28` | utility step, DD STEPLIB; sequential (no catalog evidence) |
| `LOADPADB.STEP01 (PAUDBLOD)` | DISP=SHR | `OEMA.IMS.IMSP.SDFSRESL` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/LOADPADB.JCL:31` | utility step, DD DFSRESLB; sequential (no catalog evidence) |
| `UNLDGSAM.STEP01 (DBUNLDGS)` | DISP=SHR | `OEMA.IMS.IMSP.SDFSRESL` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/UNLDGSAM.JCL:28` | utility step, DD STEPLIB; sequential (no catalog evidence) |
| `UNLDGSAM.STEP01 (DBUNLDGS)` | DISP=SHR | `OEMA.IMS.IMSP.SDFSRESL` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/UNLDGSAM.JCL:31` | utility step, DD DFSRESLB; sequential (no catalog evidence) |
| `UNLDPADB.STEP01 (PAUDBUNL)` | DISP=SHR | `OEMA.IMS.IMSP.SDFSRESL` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/UNLDPADB.JCL:40` | utility step, DD STEPLIB; sequential (no catalog evidence) |
| `UNLDPADB.STEP01 (PAUDBUNL)` | DISP=SHR | `OEMA.IMS.IMSP.SDFSRESL` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/UNLDPADB.JCL:43` | utility step, DD DFSRESLB; sequential (no catalog evidence) |
| `LOADPADB.STEP01 (PAUDBLOD)` | DISP=SHR | `OEMA.IMS.IMSP.SDFSRESL.V151` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/LOADPADB.JCL:29` | utility step, DD STEPLIB; sequential (no catalog evidence) |
| `UNLDGSAM.STEP01 (DBUNLDGS)` | DISP=SHR | `OEMA.IMS.IMSP.SDFSRESL.V151` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/UNLDGSAM.JCL:29` | utility step, DD STEPLIB; sequential (no catalog evidence) |
| `UNLDPADB.STEP01 (PAUDBUNL)` | DISP=SHR | `OEMA.IMS.IMSP.SDFSRESL.V151` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/UNLDPADB.JCL:41` | utility step, DD STEPLIB; sequential (no catalog evidence) |
| `DBPAUTP0.UNLOAD (DFSRRC00)` | DISP=SHR | `OEMPP.IMS.V15R01MB.PROCLIB` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/DBPAUTP0.jcl:33` | utility step, DD DFSVSAMP; PDS member |
| `LOADPADB.STEP01 (PAUDBLOD)` | DISP=SHR | `OEMPP.IMS.V15R01MB.PROCLIB` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/LOADPADB.JCL:44` | utility step, DD DFSVSAMP; PDS member |
| `UNLDGSAM.STEP01 (DBUNLDGS)` | DISP=SHR | `OEMPP.IMS.V15R01MB.PROCLIB` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/UNLDGSAM.JCL:46` | utility step, DD DFSVSAMP; PDS member |
| `UNLDPADB.STEP01 (PAUDBUNL)` | DISP=SHR | `OEMPP.IMS.V15R01MB.PROCLIB` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/UNLDPADB.JCL:62` | utility step, DD DFSVSAMP; PDS member |
| `CBPAUP0J.STEP01 (CBPAUP0C)` | DISP=SHR | `XXXXXXXX.PROD.LOADLIB` | dataset | PROVEN | `app/app-authorization-ims-db2-mq/jcl/CBPAUP0J.jcl:27` | utility step, DD STEPLIB; sequential (no catalog evidence) |
| `TRANEXTR.STEP20 (IEBGENER)` | DISP=SHR | `&HLQ..TRANCATG.PS` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/TRANEXTR.jcl:45` | utility step, DD SYSUT1; sequential (no catalog evidence) |
| `TRANEXTR.STEP30 (IEFBR14)` | DISP=(MOD,DELETE,DELETE) | `&HLQ..TRANCATG.PS` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/TRANEXTR.jcl:58` | utility step, DD DD02; sequential (no catalog evidence) |
| `TRANEXTR.STEP50 (IKJEFT01)` | DISP=(NEW,CATLG,DELETE) | `&HLQ..TRANCATG.PS` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/TRANEXTR.jcl:102` | utility step, DD SYSREC00; sequential (no catalog evidence) |
| `TRANEXTR.STEP20 (IEBGENER)` | DISP=(NEW,CATLG) | `&HLQ..TRANCATG.PS.BKUP` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/TRANEXTR.jcl:46` | utility step, DD SYSUT2; sequential (QSAM by DD DCB) |
| `TRANEXTR.STEP10 (IEBGENER)` | DISP=(NEW,CATLG) | `&HLQ..TRANTYPE.BKUP` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/TRANEXTR.jcl:35` | utility step, DD SYSUT2; sequential (QSAM by DD DCB) |
| `TRANEXTR.STEP10 (IEBGENER)` | DISP=SHR | `&HLQ..TRANTYPE.PS` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/TRANEXTR.jcl:34` | utility step, DD SYSUT1; sequential (no catalog evidence) |
| `TRANEXTR.STEP30 (IEFBR14)` | DISP=(MOD,DELETE,DELETE) | `&HLQ..TRANTYPE.PS` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/TRANEXTR.jcl:54` | utility step, DD DD01; sequential (no catalog evidence) |
| `TRANEXTR.STEP40 (IKJEFT01)` | DISP=(NEW,CATLG,DELETE) | `&HLQ..TRANTYPE.PS` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/TRANEXTR.jcl:72` | utility step, DD SYSREC00; sequential (no catalog evidence) |
| `CREADB2.FREEPLN (IKJEFT01)` | DISP=SHR | `&LBNM..CNTL` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/CREADB21.jcl:46` | utility step, DD SYSTSIN; PDS member |
| `CREADB2.CRCRDDB (IKJEFT01)` | DISP=SHR | `&LBNM..CNTL` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/CREADB21.jcl:58` | utility step, DD SYSTSIN; PDS member |
| `CREADB2.CRCRDDB (IKJEFT01)` | DISP=SHR | `&LBNM..CNTL` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/CREADB21.jcl:59` | utility step, DD SYSIN; PDS member |
| `CREADB2.RUNTEP2 (IKJEFT01)` | DISP=SHR | `&LBNM..CNTL` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/CREADB21.jcl:71` | utility step, DD SYSTSIN; PDS member |
| `CREADB2.RUNTEP2 (IKJEFT01)` | DISP=SHR | `&LBNM..CNTL` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/CREADB21.jcl:72` | utility step, DD SYSIN; PDS member |
| `CREADB2.LDTCCAT (IKJEFT01)` | DISP=SHR | `&LBNM..CNTL` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/CREADB21.jcl:83` | utility step, DD SYSTSIN; PDS member |
| `CREADB2.LDTCCAT (IKJEFT01)` | DISP=SHR | `&LBNM..CNTL` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/CREADB21.jcl:84` | utility step, DD SYSIN; PDS member |
| `MNTTRDB2.STEP1 (COBTUPDT)` | DISP=SHR | `AWS.M2.CARDDEMO.DBRMLIB` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/MNTTRDB2.jcl:25` | utility step, DD DBRMLIB; sequential (no catalog evidence) |
| `MNTTRDYN.STEP1 (CBTRNDYN)` | DISP=SHR | `AWS.M2.CARDDEMO.DBRMLIB` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/MNTTRDYN.jcl:27` | utility step, DD DBRMLIB; sequential (no catalog evidence) |
| `MNTTRDB2.STEP1 (COBTUPDT)` | DISP=SHR | `AWS.M2.CARDDEMO.LOADLIB` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/MNTTRDB2.jcl:24` | utility step, DD STEPLIB; sequential (no catalog evidence) |
| `MNTTRDYN.STEP1 (CBTRNDYN)` | DISP=SHR | `AWS.M2.CARDDEMO.LOADLIB` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/MNTTRDYN.jcl:26` | utility step, DD STEPLIB; sequential (no catalog evidence) |
| `MNTTRDYN.STEP1 (CBTRNDYN)` | DISP=SHR | `DYNSQL` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/MNTTRDYN.jcl:29` | utility step, DD DYNSQL; sequential (no catalog evidence) |
| `MNTTRDB2.STEP1 (COBTUPDT)` | DISP=SHR | `INPFILE` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/MNTTRDB2.jcl:27` | utility step, DD INPFILE; sequential (no catalog evidence) |
| `MNTTRDYN.STEP1 (CBTRNDYN)` | DISP=SHR | `INPFILE` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/MNTTRDYN.jcl:30` | utility step, DD INPFILE; sequential (no catalog evidence) |
| `CREADB2.CRCRDDB (IKJEFT01)` | DISP=SHR | `OEM.DB2.&DB2S..RUNLIB.LOAD` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/CREADB21.jcl:53` | utility step, DD STEPLIB; sequential (no catalog evidence) |
| `CREADB2.RUNTEP2 (IKJEFT01)` | DISP=SHR | `OEM.DB2.&DB2S..RUNLIB.LOAD` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/CREADB21.jcl:66` | utility step, DD STEPLIB; sequential (no catalog evidence) |
| `CREADB2.LDTCCAT (IKJEFT01)` | DISP=SHR | `OEM.DB2.&DB2S..RUNLIB.LOAD` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/CREADB21.jcl:78` | utility step, DD STEPLIB; sequential (no catalog evidence) |
| `TRANEXTR.STEP40 (IKJEFT01)` | DISP=SHR | `OEM.DB2.DAZ1.RUNLIB.LOAD` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/TRANEXTR.jcl:66` | utility step, DD STEPLIB; sequential (no catalog evidence) |
| `TRANEXTR.STEP50 (IKJEFT01)` | DISP=SHR | `OEM.DB2.DAZ1.RUNLIB.LOAD` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/TRANEXTR.jcl:96` | utility step, DD STEPLIB; sequential (no catalog evidence) |
| `CREADB2.FREEPLN (IKJEFT01)` | DISP=SHR | `OEM.DB2.DAZ1.SDSNEXIT` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/CREADB21.jcl:41` | utility step, DD STEPLIB; sequential (no catalog evidence) |
| `MNTTRDB2.STEP1 (COBTUPDT)` | DISP=SHR | `OEM.DB2.DAZ1.SDSNEXIT` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/MNTTRDB2.jcl:22` | utility step, DD STEPLIB; sequential (no catalog evidence) |
| `MNTTRDYN.STEP1 (CBTRNDYN)` | DISP=SHR | `OEM.DB2.DAZ1.SDSNEXIT` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/MNTTRDYN.jcl:24` | utility step, DD STEPLIB; sequential (no catalog evidence) |
| `CREADB2.FREEPLN (IKJEFT01)` | DISP=SHR | `OEMA.DB2.VERSIONA.SDSNLOAD` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/CREADB21.jcl:42` | utility step, DD STEPLIB; sequential (no catalog evidence) |
| `CREADB2.CRCRDDB (IKJEFT01)` | DISP=SHR | `OEMA.DB2.VERSIONA.SDSNLOAD` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/CREADB21.jcl:54` | utility step, DD STEPLIB; sequential (no catalog evidence) |
| `CREADB2.RUNTEP2 (IKJEFT01)` | DISP=SHR | `OEMA.DB2.VERSIONA.SDSNLOAD` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/CREADB21.jcl:67` | utility step, DD STEPLIB; sequential (no catalog evidence) |
| `CREADB2.LDTCCAT (IKJEFT01)` | DISP=SHR | `OEMA.DB2.VERSIONA.SDSNLOAD` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/CREADB21.jcl:79` | utility step, DD STEPLIB; sequential (no catalog evidence) |
| `MNTTRDB2.STEP1 (COBTUPDT)` | DISP=SHR | `OEMA.DB2.VERSIONA.SDSNLOAD` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/MNTTRDB2.jcl:23` | utility step, DD STEPLIB; sequential (no catalog evidence) |
| `MNTTRDYN.STEP1 (CBTRNDYN)` | DISP=SHR | `OEMA.DB2.VERSIONA.SDSNLOAD` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/MNTTRDYN.jcl:25` | utility step, DD STEPLIB; sequential (no catalog evidence) |
| `TRANEXTR.STEP40 (IKJEFT01)` | DISP=SHR | `OEMA.DB2.VERSIONA.SDSNLOAD` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/TRANEXTR.jcl:67` | utility step, DD STEPLIB; sequential (no catalog evidence) |
| `TRANEXTR.STEP50 (IKJEFT01)` | DISP=SHR | `OEMA.DB2.VERSIONA.SDSNLOAD` | dataset | PROVEN | `app/app-transaction-type-db2/jcl/TRANEXTR.jcl:97` | utility step, DD STEPLIB; sequential (no catalog evidence) |

### 3.3 CANDIDATE (1)

| From | Direction | To | Store | Status | Cited at | Note |
|---|---|---|---|---|---|---|
| `CORPT00C` | writes | `job TRNRPT00 (via TD queue JOBS, JES internal reader)` | submits job | CANDIDATE | `app/cbl/CORPT00C.cbl:517`, `app/csd/CARDDEMO.CSD:499` | scan status partial; executes TRANREPT |

### 3.4 OPEN (17 open asks)

| From | Direction | To | Store | Status | Cited at | Note |
|---|---|---|---|---|---|---|
| `CBTRN01C` | reads | `DD DALYTRAN (dataset not bound by any scanned JCL)` | dataset | OPEN | `app/cbl/CBTRN01C.cbl:29` | binding: unbound |
| `CBTRN01C` | reads | `DD CUSTFILE (dataset not bound by any scanned JCL)` | dataset | OPEN | `app/cbl/CBTRN01C.cbl:34` | binding: unbound |
| `CBTRN01C` | reads | `DD XREFFILE (dataset not bound by any scanned JCL)` | dataset | OPEN | `app/cbl/CBTRN01C.cbl:40` | binding: unbound |
| `CBTRN01C` | reads | `DD CARDFILE (dataset not bound by any scanned JCL)` | dataset | OPEN | `app/cbl/CBTRN01C.cbl:46` | binding: unbound |
| `CBTRN01C` | reads | `DD ACCTFILE (dataset not bound by any scanned JCL)` | dataset | OPEN | `app/cbl/CBTRN01C.cbl:52` | binding: unbound |
| `CBTRN01C` | reads | `DD TRANFILE (dataset not bound by any scanned JCL)` | dataset | OPEN | `app/cbl/CBTRN01C.cbl:58` | binding: unbound |
| `COACTUPC` | writes (SEND) | `(map name not settled in the source)` | map | OPEN | `app/cbl/COACTUPC.cbl:3594` | EXEC CICS SEND MAP |
| `COACTVWC` | writes (SEND) | `(map name not settled in the source)` | map | OPEN | `app/cbl/COACTVWC.cbl:583` | EXEC CICS SEND MAP |
| `COCRDSLC` | writes (SEND) | `(map name not settled in the source)` | map | OPEN | `app/cbl/COCRDSLC.cbl:569` | EXEC CICS SEND MAP |
| `COCRDUPC` | writes (SEND) | `(map name not settled in the source)` | map | OPEN | `app/cbl/COCRDUPC.cbl:1329` | EXEC CICS SEND MAP |
| `COPAUA0C` | reads | `MQ queue (name not captured by the scan)` | MQ queue | OPEN | `app/app-authorization-ims-db2-mq/cbl/COPAUA0C.cbl:400` | CALL 'MQGET' is static and cited; which queue it addresses is an open ask |
| `COPAUA0C` | writes | `MQ queue (name not captured by the scan)` | MQ queue | OPEN | `app/app-authorization-ims-db2-mq/cbl/COPAUA0C.cbl:758` | CALL 'MQPUT1' is static and cited; which queue it addresses is an open ask |
| `COTRTUPC` | writes (SEND) | `(map name not settled in the source)` | map | OPEN | `app/app-transaction-type-db2/cbl/COTRTUPC.cbl:1433` | EXEC CICS SEND MAP |
| `COACCT01` | reads | `MQ queue (name not captured by the scan)` | MQ queue | OPEN | `app/app-vsam-mq/cbl/COACCT01.cbl:352` | CALL 'MQGET' is static and cited; which queue it addresses is an open ask |
| `COACCT01` | writes | `MQ queue (name not captured by the scan)` | MQ queue | OPEN | `app/app-vsam-mq/cbl/COACCT01.cbl:479`, `app/app-vsam-mq/cbl/COACCT01.cbl:516` | CALL 'MQPUT' is static and cited; which queue it addresses is an open ask |
| `CODATE01` | reads | `MQ queue (name not captured by the scan)` | MQ queue | OPEN | `app/app-vsam-mq/cbl/CODATE01.cbl:301` | CALL 'MQGET' is static and cited; which queue it addresses is an open ask |
| `CODATE01` | writes | `MQ queue (name not captured by the scan)` | MQ queue | OPEN | `app/app-vsam-mq/cbl/CODATE01.cbl:383`, `app/app-vsam-mq/cbl/CODATE01.cbl:420` | CALL 'MQPUT' is static and cited; which queue it addresses is an open ask |

## 4. Programs sharing copybook layouts

30 copybooks are copied by more than one program; 21 of them by programs in more than one module. Every use is a PROVEN `COPY`/`INCLUDE` line (all 350 are in `call-graph.json`).

| Copybook | Member | Programs | Modules | Used by | First citation |
|---|---|---|---|---|---|
| `COCOM01Y` | `app/cpy/COCOM01Y.cpy` | 21 | AUTH, CARDDEMO, TRANTYPE | COACTUPC, COACTVWC, COADM01C, COBIL00C, COCRDLIC, COCRDSLC, COCRDUPC, COMEN01C, COPAUS0C, COPAUS1C, CORPT00C, COSGN00C, COTRN00C, COTRN01C, COTRN02C, COTRTLIC, COTRTUPC, COUSR00C, COUSR01C, COUSR02C, COUSR03C | `app/cbl/COACTUPC.cbl:650` |
| `COTTL01Y` | `app/cpy/COTTL01Y.cpy` | 21 | AUTH, CARDDEMO, TRANTYPE | COACTUPC, COACTVWC, COADM01C, COBIL00C, COCRDLIC, COCRDSLC, COCRDUPC, COMEN01C, COPAUS0C, COPAUS1C, CORPT00C, COSGN00C, COTRN00C, COTRN01C, COTRN02C, COTRTLIC, COTRTUPC, COUSR00C, COUSR01C, COUSR02C, COUSR03C | `app/cbl/COACTUPC.cbl:620` |
| `CSDAT01Y` | `app/cpy/CSDAT01Y.cpy` | 21 | AUTH, CARDDEMO, TRANTYPE | COACTUPC, COACTVWC, COADM01C, COBIL00C, COCRDLIC, COCRDSLC, COCRDUPC, COMEN01C, COPAUS0C, COPAUS1C, CORPT00C, COSGN00C, COTRN00C, COTRN01C, COTRN02C, COTRTLIC, COTRTUPC, COUSR00C, COUSR01C, COUSR02C, COUSR03C | `app/cbl/COACTUPC.cbl:626` |
| `CSMSG01Y` | `app/cpy/CSMSG01Y.cpy` | 21 | AUTH, CARDDEMO, TRANTYPE | COACTUPC, COACTVWC, COADM01C, COBIL00C, COCRDLIC, COCRDSLC, COCRDUPC, COMEN01C, COPAUS0C, COPAUS1C, CORPT00C, COSGN00C, COTRN00C, COTRN01C, COTRN02C, COTRTLIC, COTRTUPC, COUSR00C, COUSR01C, COUSR02C, COUSR03C | `app/cbl/COACTUPC.cbl:629` |
| `DFHAID` | not in the estate | 21 | AUTH, CARDDEMO, TRANTYPE | COACTUPC, COACTVWC, COADM01C, COBIL00C, COCRDLIC, COCRDSLC, COCRDUPC, COMEN01C, COPAUS0C, COPAUS1C, CORPT00C, COSGN00C, COTRN00C, COTRN01C, COTRN02C, COTRTLIC, COTRTUPC, COUSR00C, COUSR01C, COUSR02C, COUSR03C | `app/cbl/COACTUPC.cbl:616` |
| `DFHBMSCA` | not in the estate | 21 | AUTH, CARDDEMO, TRANTYPE | COACTUPC, COACTVWC, COADM01C, COBIL00C, COCRDLIC, COCRDSLC, COCRDUPC, COMEN01C, COPAUS0C, COPAUS1C, CORPT00C, COSGN00C, COTRN00C, COTRN01C, COTRN02C, COTRTLIC, COTRTUPC, COUSR00C, COUSR01C, COUSR02C, COUSR03C | `app/cbl/COACTUPC.cbl:615` |
| `CVACT01Y` | `app/cpy/CVACT01Y.cpy` | 15 | AUTH, CARDDEMO, VSAMMQ | CBACT01C, CBACT04C, CBEXPORT, CBFEEDRV, CBIMPORT, CBSTM03A, CBTRN01C, CBTRN02C, COACCT01, COACTUPC, COACTVWC, COBIL00C, COPAUA0C, COPAUS0C, COTRN02C | `app/cbl/CBACT01C.cbl:89` |
| `CSUSR01Y` | `app/cpy/CSUSR01Y.cpy` | 14 | CARDDEMO, TRANTYPE | COACTUPC, COACTVWC, COADM01C, COCRDLIC, COCRDSLC, COCRDUPC, COMEN01C, COSGN00C, COTRTLIC, COTRTUPC, COUSR00C, COUSR01C, COUSR02C, COUSR03C | `app/cbl/COACTUPC.cbl:635` |
| `CVACT03Y` | `app/cpy/CVACT03Y.cpy` | 14 | AUTH, CARDDEMO | CBACT03C, CBACT04C, CBEXPORT, CBIMPORT, CBSTM03A, CBTRN01C, CBTRN02C, CBTRN03C, COACTUPC, COACTVWC, COBIL00C, COPAUA0C, COPAUS0C, COTRN02C | `app/cbl/CBACT03C.cbl:45` |
| `CVTRA05Y` | `app/cpy/CVTRA05Y.cpy` | 11 | CARDDEMO | CBACT04C, CBEXPORT, CBIMPORT, CBTRN01C, CBTRN02C, CBTRN03C, COBIL00C, CORPT00C, COTRN00C, COTRN01C, COTRN02C | `app/cbl/CBACT04C.cbl:117` |
| `CVACT02Y` | `app/cpy/CVACT02Y.cpy` | 10 | AUTH, CARDDEMO, TRANTYPE | CBACT02C, CBEXPORT, CBIMPORT, CBTRN01C, COACTVWC, COCRDLIC, COCRDSLC, COCRDUPC, COPAUS0C, COTRTLIC | `app/cbl/CBACT02C.cbl:45` |
| `CVCUS01Y` | `app/cpy/CVCUS01Y.cpy` | 10 | AUTH, CARDDEMO | CBCUS01C, CBEXPORT, CBIMPORT, CBTRN01C, COACTUPC, COACTVWC, COCRDSLC, COCRDUPC, COPAUA0C, COPAUS0C | `app/cbl/CBCUS01C.cbl:45` |
| `CIPAUDTY` | `app/app-authorization-ims-db2-mq/cpy/CIPAUDTY.cpy` | 8 | AUTH | CBPAUP0C, COPAUA0C, COPAUS0C, COPAUS1C, COPAUS2C, DBUNLDGS, PAUDBLOD, PAUDBUNL | `app/app-authorization-ims-db2-mq/cbl/CBPAUP0C.cbl:121` |
| `CIPAUSMY` | `app/app-authorization-ims-db2-mq/cpy/CIPAUSMY.cpy` | 7 | AUTH | CBPAUP0C, COPAUA0C, COPAUS0C, COPAUS1C, DBUNLDGS, PAUDBLOD, PAUDBUNL | `app/app-authorization-ims-db2-mq/cbl/CBPAUP0C.cbl:117` |
| `CSMSG02Y` | `app/cpy/CSMSG02Y.cpy` | 7 | AUTH, CARDDEMO, TRANTYPE | COACTUPC, COACTVWC, COCRDSLC, COCRDUPC, COPAUS0C, COPAUS1C, COTRTUPC | `app/cbl/COACTUPC.cbl:632` |
| `CVCRD01Y` | `app/cpy/CVCRD01Y.cpy` | 7 | CARDDEMO, TRANTYPE | COACTUPC, COACTVWC, COCRDLIC, COCRDSLC, COCRDUPC, COTRTLIC, COTRTUPC | `app/cbl/COACTUPC.cbl:597` |
| `SQLCA` | not in the estate | 5 | AUTH, TRANTYPE | CBTRNDYN, COBTUPDT, COPAUS2C, COTRTLIC, COTRTUPC | `app/app-authorization-ims-db2-mq/cbl/COPAUS2C.cbl:65` |
| `CMQGMOV` | not in the estate | 3 | AUTH, VSAMMQ | COACCT01, CODATE01, COPAUA0C | `app/app-authorization-ims-db2-mq/cbl/COPAUA0C.cbl:170` |
| `CMQMDV` | not in the estate | 3 | AUTH, VSAMMQ | COACCT01, CODATE01, COPAUA0C | `app/app-authorization-ims-db2-mq/cbl/COPAUA0C.cbl:152` |
| `CMQODV` | not in the estate | 3 | AUTH, VSAMMQ | COACCT01, CODATE01, COPAUA0C | `app/app-authorization-ims-db2-mq/cbl/COPAUA0C.cbl:149` |
| `CMQPMOV` | not in the estate | 3 | AUTH, VSAMMQ | COACCT01, CODATE01, COPAUA0C | `app/app-authorization-ims-db2-mq/cbl/COPAUA0C.cbl:167` |
| `CMQTML` | not in the estate | 3 | AUTH, VSAMMQ | COACCT01, CODATE01, COPAUA0C | `app/app-authorization-ims-db2-mq/cbl/COPAUA0C.cbl:164` |
| `CMQV` | not in the estate | 3 | AUTH, VSAMMQ | COACCT01, CODATE01, COPAUA0C | `app/app-authorization-ims-db2-mq/cbl/COPAUA0C.cbl:161` |
| `DCLTRTYP` | `app/app-transaction-type-db2/dcl/DCLTRTYP.dcl` | 3 | TRANTYPE | COBTUPDT, COTRTLIC, COTRTUPC | `app/app-transaction-type-db2/cbl/COBTUPDT.cbl:54` |
| `IMSFUNCS` | `app/app-authorization-ims-db2-mq/cpy/IMSFUNCS.cpy` | 3 | AUTH | DBUNLDGS, PAUDBLOD, PAUDBUNL | `app/app-authorization-ims-db2-mq/cbl/DBUNLDGS.CBL:136` |
| `PAUTBPCB` | `app/app-authorization-ims-db2-mq/cpy/PAUTBPCB.CPY` | 3 | AUTH | DBUNLDGS, PAUDBLOD, PAUDBUNL | `app/app-authorization-ims-db2-mq/cbl/DBUNLDGS.CBL:154` |
| `CSSETATY` | `app/cpy/CSSETATY.cpy` | 2 | CARDDEMO, TRANTYPE | COACTUPC, COTRTUPC | `app/cbl/COACTUPC.cbl:3208` |
| `CVEXPORT` | `app/cpy/CVEXPORT.cpy` | 2 | CARDDEMO | CBEXPORT, CBIMPORT | `app/cbl/CBEXPORT.cbl:96` |
| `CVTRA01Y` | `app/cpy/CVTRA01Y.cpy` | 2 | CARDDEMO | CBACT04C, CBTRN02C | `app/cbl/CBACT04C.cbl:97` |
| `CVTRA06Y` | `app/cpy/CVTRA06Y.cpy` | 2 | CARDDEMO | CBTRN01C, CBTRN02C | `app/cbl/CBTRN01C.cbl:99` |

## 5. Transaction id to initial program (CSD)

| Transaction | Initial program | CSD module | Program module | Status | Cited at |
|---|---|---|---|---|---|
| `CA00` | `COADM01C` | CARDDEMO | CARDDEMO | PROVEN | `app/csd/CARDDEMO.CSD:327` |
| `CAUP` | `COACTUPC` | CARDDEMO | CARDDEMO | PROVEN | `app/csd/CARDDEMO.CSD:306` |
| `CAVW` | `COACTVWC` | CARDDEMO | CARDDEMO | PROVEN | `app/csd/CARDDEMO.CSD:317` |
| `CB00` | `COBIL00C` | CARDDEMO | CARDDEMO | PROVEN | `app/csd/CARDDEMO.CSD:337` |
| `CC00` | `COSGN00C` | CARDDEMO | CARDDEMO | PROVEN | `app/csd/CARDDEMO.CSD:378` |
| `CCDL` | `COCRDSLC` | CARDDEMO | CARDDEMO | PROVEN | `app/csd/CARDDEMO.CSD:347` |
| `CCLI` | `COCRDLIC` | CARDDEMO | CARDDEMO | PROVEN | `app/csd/CARDDEMO.CSD:357` |
| `CCUP` | `COCRDUPC` | CARDDEMO | CARDDEMO | PROVEN | `app/csd/CARDDEMO.CSD:367` |
| `CDV1` | `COCRDSEC` | CARDDEMO | **program not in the estate** | PROVEN | `app/csd/CARDDEMO.CSD:388` |
| `CM00` | `COMEN01C` | CARDDEMO | CARDDEMO | PROVEN | `app/csd/CARDDEMO.CSD:399` |
| `CR00` | `CORPT00C` | CARDDEMO | CARDDEMO | PROVEN | `app/csd/CARDDEMO.CSD:409` |
| `CT00` | `COTRN00C` | CARDDEMO | CARDDEMO | PROVEN | `app/csd/CARDDEMO.CSD:419` |
| `CT01` | `COTRN01C` | CARDDEMO | CARDDEMO | PROVEN | `app/csd/CARDDEMO.CSD:429` |
| `CT02` | `COTRN02C` | CARDDEMO | CARDDEMO | PROVEN | `app/csd/CARDDEMO.CSD:439` |
| `CU00` | `COUSR00C` | CARDDEMO | CARDDEMO | PROVEN | `app/csd/CARDDEMO.CSD:449` |
| `CU01` | `COUSR01C` | CARDDEMO | CARDDEMO | PROVEN | `app/csd/CARDDEMO.CSD:459` |
| `CU02` | `COUSR02C` | CARDDEMO | CARDDEMO | PROVEN | `app/csd/CARDDEMO.CSD:469` |
| `CU03` | `COUSR03C` | CARDDEMO | CARDDEMO | PROVEN | `app/csd/CARDDEMO.CSD:479` |
| `CP00` | `COPAUA0C` | AUTH | AUTH | PROVEN | `app/app-authorization-ims-db2-mq/csd/CRDDEMO2.csd:59` |
| `CPVD` | `COPAUS1C` | AUTH | AUTH | PROVEN | `app/app-authorization-ims-db2-mq/csd/CRDDEMO2.csd:39` |
| `CPVS` | `COPAUS0C` | AUTH | AUTH | PROVEN | `app/app-authorization-ims-db2-mq/csd/CRDDEMO2.csd:49` |
| `CTLI` | `COTRTLIC` | TRANTYPE | TRANTYPE | PROVEN | `app/app-transaction-type-db2/csd/CRDDEMOD.csd:25` |
| `CTTU` | `COTRTUPC` | TRANTYPE | TRANTYPE | PROVEN | `app/app-transaction-type-db2/csd/CRDDEMOD.csd:35` |
| `CDRA` | `COACCT01` | VSAMMQ | VSAMMQ | PROVEN | `app/app-vsam-mq/csd/CRDDEMOM.csd:17` |
| `CDRD` | `CODATE01` | VSAMMQ | VSAMMQ | PROVEN | `app/app-vsam-mq/csd/CRDDEMOM.csd:27` |

## 6. Scheduler order

From `app/scheduler/CardDemo.ca7` and `app/scheduler/CardDemo.controlm`. "Runs before" edges are PROVEN from the export. The link from a scheduled job to a JCL member is CANDIDATE: it is a match on member name across modules, and the exports name no library.

### Table `CARDDEMO` (ca7-ljob, `app/scheduler/CardDemo.ca7`)

| Job | JCL member matched | Cited at |
|---|---|---|
| `CLOSEFIL` | `app/jcl/CLOSEFIL.jcl` | `app/scheduler/CardDemo.ca7:18` |
| `CBPAUP0J` | `app/app-authorization-ims-db2-mq/jcl/CBPAUP0J.jcl` | `app/scheduler/CardDemo.ca7:43` |
| `POSTTRAN` | `app/jcl/POSTTRAN.jcl` | `app/scheduler/CardDemo.ca7:70` |
| `WAITSTEP` | `app/jcl/WAITSTEP.jcl` | `app/scheduler/CardDemo.ca7:97` |
| `OPENFIL` | `app/jcl/OPENFIL.jcl` | `app/scheduler/CardDemo.ca7:124` |
| `TRANTYPE` | `app/jcl/TRANTYPE.jcl` | `app/scheduler/CardDemo.ca7:162` |
| `CLOSEFIL1` | `app/jcl/CLOSEFIL.jcl` | `app/scheduler/CardDemo.ca7:216` |
| `CLOSEFIL2` | `app/jcl/CLOSEFIL.jcl` | `app/scheduler/CardDemo.ca7:217` |
| `TRANCATG` | `app/jcl/TRANCATG.jcl` | `app/scheduler/CardDemo.ca7:244` |
| `TCATBALF` | `app/jcl/TCATBALF.jcl` | `app/scheduler/CardDemo.ca7:271` |
| `READACCT` | `app/jcl/READACCT.jcl` | `app/scheduler/CardDemo.ca7:340` |
| `READCARD` | `app/jcl/READCARD.jcl` | `app/scheduler/CardDemo.ca7:367` |
| `READCUST` | `app/jcl/READCUST.jcl` | `app/scheduler/CardDemo.ca7:394` |
| `READXREF` | `app/jcl/READXREF.jcl` | `app/scheduler/CardDemo.ca7:421` |
| `CREASTMT` | `app/jcl/CREASTMT.JCL` | `app/scheduler/CardDemo.ca7:468` |
| `TXT2PDF1` | `app/jcl/TXT2PDF1.JCL` | `app/scheduler/CardDemo.ca7:495` |
| `PRTCATBL` | `app/jcl/PRTCATBL.jcl` | `app/scheduler/CardDemo.ca7:537` |

Order: `WAITSTEP` -> `CLOSEFIL`; `OPENFIL` -> `CLOSEFIL`; `CLOSEFIL` -> `CBPAUP0J`; `CBPAUP0J` -> `POSTTRAN`; `POSTTRAN` -> `WAITSTEP`; `TRANTYPE` -> `WAITSTEP`; `TRANCATG` -> `WAITSTEP`; `TCATBALF` -> `WAITSTEP`; `READXREF` -> `WAITSTEP`; `TXT2PDF1` -> `WAITSTEP`; `PRTCATBL` -> `WAITSTEP`; `WAITSTEP` -> `OPENFIL`; `WAITSTEP` -> `OPENFIL`; `CLOSEFIL` -> `TRANTYPE`; `WAITSTEP` -> `CLOSEFIL1`; `WAITSTEP` -> `CLOSEFIL2`; `CLOSEFIL1` -> `TRANCATG`; `CLOSEFIL2` -> `TCATBALF`; `CLOSEFIL` -> `READACCT`; `READACCT` -> `READCARD`; `READCARD` -> `READCUST`; `READCUST` -> `READXREF`; `CLOSEFIL` -> `CREASTMT`; `CREASTMT` -> `TXT2PDF1`; `CLOSEFIL` -> `PRTCATBL`

### Table `DAILY-TransactionBackup` (controlm-xml, `app/scheduler/CardDemo.controlm`)

| Job | JCL member matched | Cited at |
|---|---|---|
| `CLOSEFIL` | `app/jcl/CLOSEFIL.jcl` | `app/scheduler/CardDemo.controlm:4` |
| `TRANBKP` | `app/jcl/TRANBKP.jcl` | `app/scheduler/CardDemo.controlm:8` |
| `WAITSTEP` | `app/jcl/WAITSTEP.jcl` | `app/scheduler/CardDemo.controlm:14` |
| `OPENFIL` | `app/jcl/OPENFIL.jcl` | `app/scheduler/CardDemo.controlm:20` |

Order: `CLOSEFIL` -> `TRANBKP`; `TRANBKP` -> `WAITSTEP`; `WAITSTEP` -> `OPENFIL`

### Table `WEEKLY-TransactionTypesDBRefresh` (controlm-xml, `app/scheduler/CardDemo.controlm`)

| Job | JCL member matched | Cited at |
|---|---|---|
| `MNTTRDB2` | `app/app-transaction-type-db2/jcl/MNTTRDB2.jcl` | `app/scheduler/CardDemo.controlm:27` |

Order: single job, no predecessor in this table.

### Table `DisclosureGroupsRefresh` (controlm-xml, `app/scheduler/CardDemo.controlm`)

| Job | JCL member matched | Cited at |
|---|---|---|
| `CLOSEFIL` | `app/jcl/CLOSEFIL.jcl` | `app/scheduler/CardDemo.controlm:33` |
| `DISCGRP` | `app/jcl/DISCGRP.jcl` | `app/scheduler/CardDemo.controlm:38` |
| `WAITSTEP` | `app/jcl/WAITSTEP.jcl` | `app/scheduler/CardDemo.controlm:44` |
| `OPENFIL` | `app/jcl/OPENFIL.jcl` | `app/scheduler/CardDemo.controlm:50` |

Order: `CLOSEFIL` -> `DISCGRP`; `DISCGRP` -> `WAITSTEP`; `WAITSTEP` -> `OPENFIL`

### Table `TransactionTypesDBRefresh` (controlm-xml, `app/scheduler/CardDemo.controlm`)

| Job | JCL member matched | Cited at |
|---|---|---|
| `TRANEXTR` | `app/app-transaction-type-db2/jcl/TRANEXTR.jcl` | `app/scheduler/CardDemo.controlm:58` |

Order: single job, no predecessor in this table.

### Table `MONTHLY-InterestCalculation` (controlm-xml, `app/scheduler/CardDemo.controlm`)

| Job | JCL member matched | Cited at |
|---|---|---|
| `CLOSEFIL` | `app/jcl/CLOSEFIL.jcl` | `app/scheduler/CardDemo.controlm:65` |
| `INTCALC` | `app/jcl/INTCALC.jcl` | `app/scheduler/CardDemo.controlm:69` |
| `COMBTRAN` | `app/jcl/COMBTRAN.jcl` | `app/scheduler/CardDemo.controlm:75` |
| `WAITSTEP` | `app/jcl/WAITSTEP.jcl` | `app/scheduler/CardDemo.controlm:81` |
| `OPENFIL` | `app/jcl/OPENFIL.jcl` | `app/scheduler/CardDemo.controlm:87` |

Order: `CLOSEFIL` -> `INTCALC`; `INTCALC` -> `COMBTRAN`; `COMBTRAN` -> `WAITSTEP`; `WAITSTEP` -> `OPENFIL`

Across tables:

* MNTTRDB2 (table WEEKLY-TransactionTypesDBRefresh) -> CLOSEFIL (table DisclosureGroupsRefresh) (condition WEEKLY-TransactionTypesDBRefresh-MNTTRDB2) - PROVEN, `app/scheduler/CardDemo.controlm`
* MNTTRDB2 (table WEEKLY-TransactionTypesDBRefresh) -> TRANEXTR (table TransactionTypesDBRefresh) (condition WEEKLY-TransactionTypesDBRefresh-MNTTRDB2) - PROVEN, `app/scheduler/CardDemo.controlm`

### Unscheduled JCL jobs (27)

Jobs in the estate that neither scheduler export names.

| Job | Module | Member |
|---|---|---|
| `ACCTFILE` | CARDDEMO | `app/jcl/ACCTFILE.jcl` |
| `CARDFILE` | CARDDEMO | `app/jcl/CARDFILE.jcl` |
| `CBADMCDJ` | CARDDEMO | `app/jcl/CBADMCDJ.jcl` |
| `CBEXPORT` | CARDDEMO | `app/jcl/CBEXPORT.jcl` |
| `CBIMPORT` | CARDDEMO | `app/jcl/CBIMPORT.jcl` |
| `CREADB2` | TRANTYPE | `app/app-transaction-type-db2/jcl/CREADB21.jcl` |
| `CUSTFILE` | CARDDEMO | `app/jcl/CUSTFILE.jcl` |
| `DALYREJS` | CARDDEMO | `app/jcl/DALYREJS.jcl` |
| `DBPAUTP0` | AUTH | `app/app-authorization-ims-db2-mq/jcl/DBPAUTP0.jcl` |
| `DEFCUST` | CARDDEMO | `app/jcl/DEFCUST.jcl` |
| `DEFGDGB` | CARDDEMO | `app/jcl/DEFGDGB.jcl` |
| `DEFGDGD` | CARDDEMO | `app/jcl/DEFGDGD.jcl` |
| `DUSRSECJ` | CARDDEMO | `app/jcl/DUSRSECJ.jcl` |
| `ESDSRRDS` | CARDDEMO | `app/jcl/ESDSRRDS.jcl` |
| `FEECALC` | CARDDEMO | `app/jcl/FEECALC.jcl` |
| `FTPJCLS` | CARDDEMO | `app/jcl/FTPJCL.JCL` |
| `INTRDRJ1` | CARDDEMO | `app/jcl/INTRDRJ1.JCL` |
| `INTRDRJ2` | CARDDEMO | `app/jcl/INTRDRJ2.JCL` |
| `LOADPADB` | AUTH | `app/app-authorization-ims-db2-mq/jcl/LOADPADB.JCL` |
| `MNTTRDYN` | TRANTYPE | `app/app-transaction-type-db2/jcl/MNTTRDYN.jcl` |
| `REPTFILE` | CARDDEMO | `app/jcl/REPTFILE.jcl` |
| `TRANFILE` | CARDDEMO | `app/jcl/TRANFILE.jcl` |
| `TRANIDX` | CARDDEMO | `app/jcl/TRANIDX.jcl` |
| `TRANREPT` | CARDDEMO | `app/jcl/TRANREPT.jcl` |
| `UNLDGSAM` | AUTH | `app/app-authorization-ims-db2-mq/jcl/UNLDGSAM.JCL` |
| `UNLDPADB` | AUTH | `app/app-authorization-ims-db2-mq/jcl/UNLDPADB.JCL` |
| `XREFFILE` | CARDDEMO | `app/jcl/XREFFILE.jcl` |

## 7. Uncalled programs and unreferenced members

### Uncalled programs (1)

No PROVEN or CANDIDATE call, no JCL step and no CSD transaction reaches them.

| Program | Module | Member |
|---|---|---|
| `CBTRN01C` | CARDDEMO | `app/cbl/CBTRN01C.cbl` |

Programs reached only through CANDIDATE edges or only by a transaction are not in this list; `call-graph.json` (`entered`) shows what reaches each program.

### Unreferenced members (19)

Members that no scanned member refers to by name. "Text mentions" is a plain word search of `app/` for the member name, given so a reader can tell a truly unused member from a scanner gap; it is not evidence of use.

| Member | Kind | Text mentions elsewhere | Where |
|---|---|---|---|
| `app/app-authorization-ims-db2-mq/ddl/XAUTHFRD.ddl` | unclassified | 1 | `app/app-authorization-ims-db2-mq/README.md` |
| `app/app-authorization-ims-db2-mq/ims/DBPAUTP0.dbd` | ims-dbd | 7 | `app/app-authorization-ims-db2-mq/ims/PAUTBUNL.PSB`, `app/app-authorization-ims-db2-mq/ims/DBPAUTX0.dbd`, `app/app-authorization-ims-db2-mq/ims/PSBPAUTB.psb`, `app/app-authorization-ims-db2-mq/ims/DLIGSAMP.PSB` |
| `app/app-authorization-ims-db2-mq/ims/DBPAUTX0.dbd` | ims-dbd | 2 | `app/app-authorization-ims-db2-mq/ims/DBPAUTP0.dbd`, `app/app-authorization-ims-db2-mq/README.md` |
| `app/app-authorization-ims-db2-mq/ims/DLIGSAMP.PSB` | ims-psb | 1 | `app/app-authorization-ims-db2-mq/jcl/UNLDGSAM.JCL` |
| `app/app-authorization-ims-db2-mq/ims/PADFLDBD.DBD` | ims-dbd | 1 | `app/app-authorization-ims-db2-mq/ims/DLIGSAMP.PSB` |
| `app/app-authorization-ims-db2-mq/ims/PASFLDBD.DBD` | ims-dbd | 1 | `app/app-authorization-ims-db2-mq/ims/DLIGSAMP.PSB` |
| `app/app-authorization-ims-db2-mq/ims/PAUTBUNL.PSB` | ims-psb | 1 | `app/app-authorization-ims-db2-mq/jcl/UNLDPADB.JCL` |
| `app/app-authorization-ims-db2-mq/ims/PSBPAUTB.psb` | ims-psb | 7 | `app/app-authorization-ims-db2-mq/cbl/COPAUS1C.cbl`, `app/app-authorization-ims-db2-mq/cbl/CBPAUP0C.cbl`, `app/app-authorization-ims-db2-mq/cbl/COPAUA0C.cbl`, `app/app-authorization-ims-db2-mq/cbl/COPAUS0C.cbl` |
| `app/app-authorization-ims-db2-mq/ims/PSBPAUTL.psb` | ims-psb | 1 | `app/app-authorization-ims-db2-mq/README.md` |
| `app/app-transaction-type-db2/ddl/TRNTYCAT.ddl` | sql-ddl | 0 | - |
| `app/app-transaction-type-db2/ddl/TRNTYPE.ddl` | sql-ddl | 0 | - |
| `app/app-transaction-type-db2/ddl/XTRNTYCAT.ddl` | unclassified | 0 | - |
| `app/app-transaction-type-db2/ddl/XTRNTYPE.ddl` | unclassified | 0 | - |
| `app/catlg/LISTCAT.txt` | sql-script | 0 | - |
| `app/cpy/CSSTRPFY.cpy` | cobol-copybook | 7 | `app/cbl/COACTVWC.cbl`, `app/cbl/COCRDSLC.cbl`, `app/cbl/COCRDLIC.cbl`, `app/cbl/COCRDUPC.cbl` |
| `app/cpy/CSUTLDWY.cpy` | cobol-copybook | 2 | `app/cbl/COACTUPC.cbl`, `app/app-transaction-type-db2/cbl/COTRTUPC.cbl` |
| `app/cpy/UNUSED1Y.cpy` | cobol-copybook | 0 | - |
| `app/data/EBCDIC/AWS.M2.CARDDEMO.ACCDATA.PS` | binary | 0 | - |
| `app/maclib/ASMWAIT.mac` | assembler-macro | 1 | `app/asm/MVSWAIT.asm` |

## 8. Disagreements with `docs/chain-graph.md` (22)

| What | Item | Scan | docs/chain-graph.md |
|---|---|---|---|
| program missing from docs/chain-graph.md | `CBPAUP0C` | app/app-authorization-ims-db2-mq/cbl/CBPAUP0C.cbl (COBOL, module AUTH) | not listed |
| program missing from docs/chain-graph.md | `COACCT01` | app/app-vsam-mq/cbl/COACCT01.cbl (COBOL, module VSAMMQ) | not listed |
| program missing from docs/chain-graph.md | `CODATE01` | app/app-vsam-mq/cbl/CODATE01.cbl (COBOL, module VSAMMQ) | not listed |
| program missing from docs/chain-graph.md | `COPAUA0C` | app/app-authorization-ims-db2-mq/cbl/COPAUA0C.cbl (COBOL, module AUTH) | not listed |
| program missing from docs/chain-graph.md | `COPAUS0C` | app/app-authorization-ims-db2-mq/cbl/COPAUS0C.cbl (COBOL, module AUTH) | not listed |
| program missing from docs/chain-graph.md | `COPAUS1C` | app/app-authorization-ims-db2-mq/cbl/COPAUS1C.cbl (COBOL, module AUTH) | not listed |
| program missing from docs/chain-graph.md | `COPAUS2C` | app/app-authorization-ims-db2-mq/cbl/COPAUS2C.cbl (COBOL, module AUTH) | not listed |
| program missing from docs/chain-graph.md | `DBUNLDGS` | app/app-authorization-ims-db2-mq/cbl/DBUNLDGS.CBL (COBOL, module AUTH) | not listed |
| program missing from docs/chain-graph.md | `PAUDBLOD` | app/app-authorization-ims-db2-mq/cbl/PAUDBLOD.CBL (COBOL, module AUTH) | not listed |
| program missing from docs/chain-graph.md | `PAUDBUNL` | app/app-authorization-ims-db2-mq/cbl/PAUDBUNL.CBL (COBOL, module AUTH) | not listed |
| static CALLs differ | `CSUTLDTC` | only in scan: CEEDAYS | only in doc: - |
| job missing from docs/chain-graph.md | `CBPAUP0J` | app/app-authorization-ims-db2-mq/jcl/CBPAUP0J.jcl | not listed |
| job missing from docs/chain-graph.md | `CREASTMT` | app/jcl/CREASTMT.JCL | not listed |
| job missing from docs/chain-graph.md | `DBPAUTP0` | app/app-authorization-ims-db2-mq/jcl/DBPAUTP0.jcl | not listed |
| job missing from docs/chain-graph.md | `FTPJCLS` | app/jcl/FTPJCL.JCL | not listed |
| job missing from docs/chain-graph.md | `INTRDRJ1` | app/jcl/INTRDRJ1.JCL | not listed |
| job missing from docs/chain-graph.md | `INTRDRJ2` | app/jcl/INTRDRJ2.JCL | not listed |
| job missing from docs/chain-graph.md | `LOADPADB` | app/app-authorization-ims-db2-mq/jcl/LOADPADB.JCL | not listed |
| job missing from docs/chain-graph.md | `TXT2PDF1` | app/jcl/TXT2PDF1.JCL | not listed |
| job missing from docs/chain-graph.md | `UNLDGSAM` | app/app-authorization-ims-db2-mq/jcl/UNLDGSAM.JCL | not listed |
| job missing from docs/chain-graph.md | `UNLDPADB` | app/app-authorization-ims-db2-mq/jcl/UNLDPADB.JCL | not listed |
| program run under a launcher differs | `TRANEXTR` | scan resolves: nothing | doc resolves: STEP40=DSNTIAUL, STEP50=DSNTIAUL |

Where both list a program, copybooks, DD names and static CALLs agree except as shown. `docs/chain-graph.md` covers `app/cbl`, the transaction-type sub-application and lower-case `.jcl` members only; the authorization and VSAM-MQ sub-applications and the upper-case `.JCL`/`.CBL` members are outside it.

