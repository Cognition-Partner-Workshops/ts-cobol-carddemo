/**
 * exportDatasetsStep (INT-T07, component interest-export of the architecture): writes a calculated run
 * from the tables as the SYSTRAN generation (INT-T03, INT-T04, INT-T05, INT-R06), the ACCTDATA image
 * and rc.json, in the recorded fixture format (FI-2) or as fixed-length host files (strategy CO2).
 *
 * <p>Open decisions, implemented as proposed: REL-03 (the files are the hand-over; the mainframe
 * catalogs SYSTRAN(+1) and writes the accounts back) and REL-08 (host files are cp037, zoned signs
 * C/D, fixed-length records without a separator). Transfer and write-back JCL are not part of this.
 */
package com.carddemo.interest.export;
