      ******************************************************************
      * PARMDRV - local stand-in for the JCL PARM= operand.
      * z/OS passes PARM='...' to a batch program as a halfword length
      * followed by the text.  cobcrun has no such convention, so the
      * JCL runner invokes  cobcrun PARMDRV <program> <parm text>  and
      * this driver rebuilds the z/OS parameter structure before the
      * CALL.  Not part of the original estate.
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. PARMDRV.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  WS-CMDLINE              PIC X(256).
       01  WS-PGM-NAME             PIC X(8).
       01  WS-PARM-TEXT            PIC X(100).
       01  WS-PARM-AREA.
           05  WS-PARM-LENGTH      PIC S9(04) COMP.
           05  WS-PARM-DATA        PIC X(100).
       PROCEDURE DIVISION.
           ACCEPT WS-CMDLINE FROM COMMAND-LINE
           UNSTRING WS-CMDLINE DELIMITED BY ALL SPACES
               INTO WS-PGM-NAME WS-PARM-TEXT
           END-UNSTRING
           MOVE WS-PARM-TEXT TO WS-PARM-DATA
           MOVE FUNCTION LENGTH(FUNCTION TRIM(WS-PARM-TEXT TRAILING))
             TO WS-PARM-LENGTH
           CALL WS-PGM-NAME USING WS-PARM-AREA
           MOVE RETURN-CODE TO RETURN-CODE
           STOP RUN.
