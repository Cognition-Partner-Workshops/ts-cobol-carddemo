//MNTTRDYN JOB 'TRAN TYPE ADHOC',CLASS=A,MSGCLASS=0,
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
//* Ad-hoc transaction type maintenance: CBTRNDYN runs the SQL in
//* DYNSQL with PREPARE/EXECUTE, then dynamically CALLs the program
//* named in PARMS (COBTUPDT) to apply the regular update file.
//*
//STEP1   EXEC PGM=IKJEFT01,REGION=0M
//STEPLIB  DD DISP=SHR,DSN=OEM.DB2.DAZ1.SDSNEXIT
//         DD DISP=SHR,DSN=OEMA.DB2.VERSIONA.SDSNLOAD
//         DD DISP=SHR,DSN=AWS.M2.CARDDEMO.LOADLIB
//DBRMLIB  DD DISP=SHR,DSN=AWS.M2.CARDDEMO.DBRMLIB
//SYSTSPRT DD  SYSOUT=*
//DYNSQL   DD  DSN=DYNSQL,DISP=SHR
//INPFILE  DD  DSN=INPFILE,DISP=SHR
//SYSTSIN DD *
  DSN SYSTEM(DAZ1)
  RUN PROGRAM(CBTRNDYN) PLAN(CARDDEMO) PARMS('COBTUPDT')
  END
/*
//SYSPRINT DD  SYSOUT=*
//SYSOUT   DD  SYSOUT=*
//SYSUDUMP DD  SYSOUT=*
//*
