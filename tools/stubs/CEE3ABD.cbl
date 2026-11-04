      ******************************************************************
      * CEE3ABD - local stand-in for the Language Environment abend
      * service called by CBTRN02C / CBACT04C on unrecoverable I/O
      * errors.  LE terminates the step with the abend code; here the
      * stand-in displays the code and ends the run with RC 12 so the
      * JCL runner records a failed step.  Not part of the original
      * estate.
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CEE3ABD.
       DATA DIVISION.
       LINKAGE SECTION.
       01  LS-ABEND-CODE           PIC S9(9) BINARY.
       01  LS-TIMING               PIC S9(9) BINARY.
       PROCEDURE DIVISION USING LS-ABEND-CODE LS-TIMING.
           DISPLAY 'CEE3ABD: USER ABEND U' LS-ABEND-CODE
           MOVE 12 TO RETURN-CODE
           STOP RUN.
