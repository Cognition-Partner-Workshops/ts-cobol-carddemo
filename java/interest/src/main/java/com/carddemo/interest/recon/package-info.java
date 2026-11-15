/**
 * The data move kit (INT-T10): the reconciliation report of a load and a run (strategy RC1-RC6 with
 * the profile of TL4) and the side-by-side comparison of two output directories (SS2-SS4).
 *
 * <p>The kit reports and does not decide: REL-04 (number of cycles) and REL-05 (the bar for
 * "equal") are open. A figure that cannot be computed is NOT AVAILABLE, never EQUAL. The report
 * reads {@code interest_run} and the tables in one read-only transaction and writes nothing to the
 * database. RC6 needs ACCTDATA unloaded again after the write-back on the mainframe; it cannot be
 * shown off-host and is always NOT AVAILABLE here.
 */
package com.carddemo.interest.recon;
