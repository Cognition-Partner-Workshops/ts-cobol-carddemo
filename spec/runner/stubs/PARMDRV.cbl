      *****************************************************************
      * Stands in for the JCL EXEC statement's PARM= operand. z/OS hands
      * the program a halfword length and the PARM text; this driver
      * reads the text from the environment variable JCL_PARM and calls
      * the program named in JCL_PGM the same way.
      *****************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. PARMDRV.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  WS-PGM                   PIC X(8).
       01  WS-PARM.
           05  WS-PARM-LENGTH       PIC S9(04) COMP.
           05  WS-PARM-TEXT         PIC X(100).
       PROCEDURE DIVISION.
           ACCEPT WS-PGM FROM ENVIRONMENT 'JCL_PGM'
           ACCEPT WS-PARM-TEXT FROM ENVIRONMENT 'JCL_PARM'
           MOVE FUNCTION LENGTH(FUNCTION TRIM(WS-PARM-TEXT TRAILING))
             TO WS-PARM-LENGTH
           CALL WS-PGM USING WS-PARM
           STOP RUN RETURNING RETURN-CODE.
