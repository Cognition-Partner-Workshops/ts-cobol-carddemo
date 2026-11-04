      ******************************************************************
      * DSNEXIT - end-of-task exit installed by DSNRUN (CBL_EXIT_PROC).
      * Commits the unit of work and releases the PostgreSQL connection
      * when the application program ends with STOP RUN, mirroring the
      * implicit commit the TSO DSN command performs at normal end of
      * task.  Not part of the original estate.
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. DSNEXIT.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
           EXEC SQL INCLUDE SQLCA END-EXEC.
       PROCEDURE DIVISION.
           EXEC SQL COMMIT END-EXEC
           IF SQLCODE NOT = 0
               DISPLAY 'DSNRUN: COMMIT FAILED ' SQLCODE
           ELSE
               DISPLAY 'DSNRUN: COMMIT AT END OF TASK'
           END-IF
           EXEC SQL DISCONNECT ALL END-EXEC
           GOBACK.
