      ******************************************************************
      * DSNRUN - local stand-in for the TSO batch Db2 attach
      *   //STEP EXEC PGM=IKJEFT01 ... //SYSTSIN DD *
      *      DSN SYSTEM(xxxx)
      *      RUN PROGRAM(pgm) PLAN(plan)
      * On z/OS the DSN command attaches the Db2 subsystem, runs the
      * program and commits at normal end of task.  Here the JCL runner
      * invokes  cobcrun DSNRUN <program>  and this driver connects to
      * PostgreSQL through Open COBOL ESQL (OCDB_* environment), calls
      * the program and commits in an exit procedure, so the application
      * program itself needs no CONNECT or COMMIT.  Not part of the
      * original estate.
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. DSNRUN.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
           EXEC SQL INCLUDE SQLCA END-EXEC.
       01  WS-CMDLINE              PIC X(256).
       01  WS-PGM-NAME             PIC X(8).
       01  WS-DB-NAME              PIC X(64).
       01  WS-DB-USER              PIC X(64).
       01  WS-DB-PASS              PIC X(128).
       01  WS-EXIT-PROC            PROGRAM-POINTER.
       01  WS-INSTALL-FLAG         PIC X VALUE X'00'.
      * DSN RUN ... PARMS('...') as the z/OS parameter area
       01  WS-PARM-AREA.
           05 WS-PARM-LENGTH       PIC S9(04) COMP VALUE 0.
           05 WS-PARM-VALUE        PIC X(100) VALUE SPACES.
       PROCEDURE DIVISION.
           ACCEPT WS-CMDLINE FROM COMMAND-LINE
           UNSTRING WS-CMDLINE DELIMITED BY ALL SPACES
               INTO WS-PGM-NAME
                    WS-PARM-VALUE COUNT IN WS-PARM-LENGTH
           END-UNSTRING
           ACCEPT WS-DB-NAME FROM ENVIRONMENT "OCDB_NAME"
           ACCEPT WS-DB-USER FROM ENVIRONMENT "OCDB_USER"
           ACCEPT WS-DB-PASS FROM ENVIRONMENT "OCDB_PASS"
           EXEC SQL
               CONNECT :WS-DB-USER IDENTIFIED BY :WS-DB-PASS
                   USING :WS-DB-NAME
           END-EXEC
           IF SQLCODE NOT = 0
               DISPLAY 'DSNRUN: DATABASE CONNECT FAILED ' SQLCODE
               MOVE 12 TO RETURN-CODE
               STOP RUN
           END-IF
           DISPLAY 'DSNRUN: DSN ATTACHED, RUNNING ' WS-PGM-NAME
           SET WS-EXIT-PROC TO ENTRY 'DSNEXIT'
           CALL 'CBL_EXIT_PROC' USING WS-INSTALL-FLAG WS-EXIT-PROC
           CALL WS-PGM-NAME USING WS-PARM-AREA
           STOP RUN.
