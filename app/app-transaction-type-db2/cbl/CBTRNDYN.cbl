      ******************************************************************
      * Program     : CBTRNDYN.CBL
      * Application : CardDemo - transaction type maintenance (Db2)
      * Type        : BATCH COBOL + embedded SQL
      * Function    : Ad-hoc maintenance driver.
      *               1. Reads one SQL statement from DYNSQL and runs
      *                  it with dynamic SQL (PREPARE ... EXECUTE).
      *               2. Dynamically CALLs the program named in the
      *                  JCL PARM (e.g. PARM='COBTUPDT') so the regular
      *                  update file is applied in the same unit of
      *                  work.  The program name is not known at
      *                  compile time: CALL WS-PGM-NAME.
      ******************************************************************
      * Copyright Amazon.com, Inc. or its affiliates.
      * All Rights Reserved.
      *
      * Licensed under the Apache License, Version 2.0 (the "License").
      * You may not use this file except in compliance with the License.
      * You may obtain a copy of the License at
      *
      *    http://www.apache.org/licenses/LICENSE-2.0
      *
      * Unless required by applicable law or agreed to in writing,
      * software distributed under the License is distributed on an
      * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
      * either express or implied. See the License for the specific
      * language governing permissions and limitations under the License
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CBTRNDYN.
       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT DYNSQL-FILE ASSIGN TO DYNSQL
                  ORGANIZATION IS SEQUENTIAL
                  FILE STATUS  IS DYNSQL-STATUS.
       DATA DIVISION.
       FILE SECTION.
       FD  DYNSQL-FILE.
       01  DYNSQL-REC                           PIC X(80).
       WORKING-STORAGE SECTION.
           EXEC SQL INCLUDE SQLCA END-EXEC.
       01  DYNSQL-STATUS                        PIC X(02).
       01  END-OF-FILE                          PIC X VALUE 'N'.
       01  WS-LINE-COUNT                        PIC 9(02) VALUE 0.
       01  WS-PGM-NAME                          PIC X(08) VALUE SPACES.
      * Db2 VARCHAR host structure for the statement text
       01  WS-SQL-STMT.
           49 WS-SQL-STMT-LEN                   PIC S9(04) COMP VALUE 0.
           49 WS-SQL-STMT-ARR                   PIC X(800) VALUE SPACES.
       01  WS-SQL-POS                           PIC 9(04) VALUE 1.
       LINKAGE SECTION.
       01  PARM-AREA.
           05 PARM-LENGTH                       PIC S9(04) COMP.
           05 PARM-VALUE                        PIC X(08).
       PROCEDURE DIVISION USING PARM-AREA.
           PERFORM 1000-READ-STATEMENT
           PERFORM 2000-RUN-DYNAMIC-SQL
           IF PARM-LENGTH > 0
               MOVE PARM-VALUE(1:PARM-LENGTH) TO WS-PGM-NAME
               DISPLAY 'CBTRNDYN: CALLING ' WS-PGM-NAME
               CALL WS-PGM-NAME
           END-IF
           DISPLAY 'CBTRNDYN: END'
           GOBACK.

       1000-READ-STATEMENT.
           OPEN INPUT DYNSQL-FILE
           PERFORM UNTIL END-OF-FILE = 'Y'
               READ DYNSQL-FILE
                   AT END MOVE 'Y' TO END-OF-FILE
                   NOT AT END
                       IF DYNSQL-REC(1:2) NOT = '--'
                           STRING DYNSQL-REC(1:72) DELIMITED BY SIZE
                                  ' ' DELIMITED BY SIZE
                                  INTO WS-SQL-STMT-ARR
                                  WITH POINTER WS-SQL-POS
                           ADD 1 TO WS-LINE-COUNT
                       END-IF
               END-READ
           END-PERFORM
           CLOSE DYNSQL-FILE
           COMPUTE WS-SQL-STMT-LEN = WS-SQL-POS - 1
           DISPLAY 'CBTRNDYN: ' WS-LINE-COUNT ' SQL LINES READ'
           .

       2000-RUN-DYNAMIC-SQL.
           EXEC SQL
               PREPARE DYNSTMT FROM :WS-SQL-STMT
           END-EXEC
           IF SQLCODE NOT = 0
               DISPLAY 'CBTRNDYN: PREPARE FAILED SQLCODE ' SQLCODE
               MOVE 8 TO RETURN-CODE
           ELSE
               EXEC SQL
                   EXECUTE DYNSTMT
               END-EXEC
               DISPLAY 'CBTRNDYN: EXECUTE SQLCODE ' SQLCODE
               IF SQLCODE < 0
                   MOVE 8 TO RETURN-CODE
               END-IF
           END-IF
           .
