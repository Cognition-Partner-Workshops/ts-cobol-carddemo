//FEECALC JOB 'LATE FEES',CLASS=A,MSGCLASS=0,
// NOTIFY=&SYSUID
//*
//* Copyright Amazon.com, Inc. or its affiliates.
//* All Rights Reserved.
//*
//* Licensed under the Apache License, Version 2.0 (the "License").
//* You may not use this file except in compliance with the License.
//* You may obtain a copy of the License at
//*
//*    http://www.apache.org/licenses/LICENSE-2.0
//*
//* Unless required by applicable law or agreed to in writing,
//* software distributed under the License is distributed on an
//* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
//* either express or implied. See the License for the specific
//* language governing permissions and limitations under the License
//*
//* Month-end late-fee calculation: COBOL driver CBFEEDRV calls the
//* PL/I routine CBFEECAL (app/pli) for every account on the master.
//* Run after INTCALC.  Not executable off-host (no PL/I compiler),
//* see docs/LEGACY-LANGUAGES.md.
//*
//STEP05  EXEC PGM=CBFEEDRV
//STEPLIB  DD DISP=SHR,DSN=AWS.M2.CARDDEMO.LOADLIB
//         DD DISP=SHR,DSN=CEE.SCEERUN
//ACCTFILE DD DISP=SHR,DSN=AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS
//FEEOUT   DD DSN=AWS.M2.CARDDEMO.LATEFEES(+1),
//            DISP=(NEW,CATLG,DELETE),
//            DCB=(RECFM=FB,LRECL=80,BLKSIZE=0),
//            SPACE=(TRK,(1,1),RLSE)
//SYSPRINT DD SYSOUT=*
//SYSOUT   DD SYSOUT=*
//SYSUDUMP DD SYSOUT=*
//*
