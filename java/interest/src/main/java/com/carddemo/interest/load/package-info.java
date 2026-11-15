/**
 * interest-loader (architecture, section Components): loadExtractsStep decodes the four unloaded
 * files and replaces the four replica tables, then records the counts and sums of reconciliation
 * RC1 and RC2 in interest_run (strategy items TL3, TL4, RC1-RC4).
 *
 * <p>Three release decisions are OPEN; the loader implements the proposed option of each:
 * REL-01 (a file bridge at the dataset boundary: the inputs arrive as unloaded files), REL-02 (the
 * mainframe file is the system of record: the tables are replaced, never merged, before every run)
 * and REL-08 (host files are cp037 with C/D signs: {@code CodePage.host()}).
 */
package com.carddemo.interest.load;
