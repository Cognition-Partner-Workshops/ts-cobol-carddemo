      ******************************************************************
      * Program     : CBFEEDRV.CBL
      * Application : CardDemo
      * Type        : BATCH COBOL Program
      * Function    : Read the account master and call the PL/I late-fee
      *               routine CBFEECAL for every account with an overdue
      *               cycle; write one 80-byte fee line per account.
      *               Lab note: CBFEECAL is PL/I and cannot be built
      *               off-host, so FEECALC.jcl is documented but not part
      *               of a runnable chain (docs/LEGACY-LANGUAGES.md).
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
       PROGRAM-ID. CBFEEDRV.
       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT ACCTFILE-FILE ASSIGN TO ACCTFILE
                  ORGANIZATION IS INDEXED
                  ACCESS MODE  IS SEQUENTIAL
                  RECORD KEY   IS FD-ACCT-ID
                  FILE STATUS  IS ACCTFILE-STATUS.
           SELECT FEEOUT-FILE ASSIGN TO FEEOUT
                  ORGANIZATION IS SEQUENTIAL
                  FILE STATUS  IS FEEOUT-STATUS.
       DATA DIVISION.
       FILE SECTION.
       FD  ACCTFILE-FILE.
       01  FD-ACCTFILE-REC.
           05 FD-ACCT-ID                        PIC 9(11).
           05 FD-ACCT-DATA                      PIC X(289).
       FD  FEEOUT-FILE.
       01  FEEOUT-REC                           PIC X(80).
       WORKING-STORAGE SECTION.
       COPY CVACT01Y.
       01  ACCTFILE-STATUS                      PIC X(02).
       01  FEEOUT-STATUS                        PIC X(02).
       01  END-OF-FILE                          PIC X VALUE 'N'.
      * Mirrors the BASED structure FEE_REQ in app/pli/CBFEECAL.pli
       01  FEE-REQ.
           05 FEE-ACCT-ID                       PIC X(11).
           05 FEE-GROUP-ID                      PIC X(10).
           05 FEE-CYCLE-BAL                     PIC S9(10)V99 COMP-3.
           05 FEE-LATE-DAYS                     PIC S9(04) COMP.
           05 FEE-CYCLE-DAYS                    PIC S9(04) COMP.
           05 FEE-AMT                           PIC S9(07)V99 COMP-3.
           05 FEE-AVG-DAILY                     PIC S9(10)V99 COMP-3.
           05 FEE-RC                            PIC S9(04) COMP.
       01  FEE-LINE.
           05 FL-ACCT-ID                        PIC X(11).
           05 FILLER                            PIC X VALUE SPACE.
           05 FL-GROUP-ID                       PIC X(10).
           05 FILLER                            PIC X VALUE SPACE.
           05 FL-CYCLE-BAL                      PIC -(10)9.99.
           05 FILLER                            PIC X VALUE SPACE.
           05 FL-FEE-AMT                        PIC -(7)9.99.
           05 FILLER                            PIC X VALUE SPACE.
           05 FL-RC                             PIC 99.
           05 FILLER                            PIC X(28) VALUE SPACES.
       PROCEDURE DIVISION.
           OPEN INPUT ACCTFILE-FILE
           OPEN OUTPUT FEEOUT-FILE
           PERFORM UNTIL END-OF-FILE = 'Y'
               READ ACCTFILE-FILE INTO ACCOUNT-RECORD
                   AT END MOVE 'Y' TO END-OF-FILE
                   NOT AT END PERFORM 1000-CHARGE-FEE
               END-READ
           END-PERFORM
           CLOSE ACCTFILE-FILE FEEOUT-FILE
           GOBACK.

       1000-CHARGE-FEE.
           MOVE ACCT-ID            TO FEE-ACCT-ID
           MOVE ACCT-GROUP-ID      TO FEE-GROUP-ID
           COMPUTE FEE-CYCLE-BAL = ACCT-CURR-CYC-CREDIT
                                 - ACCT-CURR-CYC-DEBIT
      *    The account master carries no delinquency counter; the lab
      *    treats a balance-due cycle (positive cycle balance) as 10
      *    days late over a 30-day cycle.
           IF FEE-CYCLE-BAL > 0
               MOVE 10 TO FEE-LATE-DAYS
           ELSE
               MOVE 0  TO FEE-LATE-DAYS
           END-IF
           MOVE 30 TO FEE-CYCLE-DAYS
           MOVE 0  TO FEE-AMT FEE-AVG-DAILY FEE-RC
           CALL 'CBFEECAL' USING BY REFERENCE FEE-REQ
           MOVE FEE-ACCT-ID   TO FL-ACCT-ID
           MOVE FEE-GROUP-ID  TO FL-GROUP-ID
           MOVE FEE-CYCLE-BAL TO FL-CYCLE-BAL
           MOVE FEE-AMT       TO FL-FEE-AMT
           MOVE FEE-RC        TO FL-RC
           WRITE FEEOUT-REC FROM FEE-LINE
           .
