      *****************************************************************
      * Stub for the Language Environment abend service CEE3ABD.
      * GnuCOBOL has no Language Environment. The stub prints the abend
      * code and ends the run unit with return code 12, which is how the
      * harness sees an abend. Listed as an assumption in the spec.
      *****************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CEE3ABD.
       DATA DIVISION.
       LINKAGE SECTION.
       01  LK-ABCODE                PIC S9(9) BINARY.
       01  LK-TIMING                PIC S9(9) BINARY.
       PROCEDURE DIVISION USING LK-ABCODE LK-TIMING.
           DISPLAY 'CEE3ABD STUB: ABEND CODE ' LK-ABCODE
           STOP RUN RETURNING 12.
