      *****************************************************************
      * REJTRAN - local layout of a CBTRN02C reject record (DALYREJS):
      * the 350-byte daily transaction (CVTRA06Y) followed by the 80-byte
      * validation trailer CBTRN02C appends (WS-VALIDATION-TRAILER).
      * Used by tools/parity only; not part of the original estate.
      *****************************************************************
       01  REJECT-RECORD.
           05  DALYTRAN-ID                             PIC X(16).
           05  DALYTRAN-TYPE-CD                        PIC X(02).
           05  DALYTRAN-CAT-CD                         PIC 9(04).
           05  DALYTRAN-SOURCE                         PIC X(10).
           05  DALYTRAN-DESC                           PIC X(100).
           05  DALYTRAN-AMT                            PIC S9(09)V99.
           05  DALYTRAN-MERCHANT-ID                    PIC 9(09).
           05  DALYTRAN-MERCHANT-NAME                  PIC X(50).
           05  DALYTRAN-MERCHANT-CITY                  PIC X(50).
           05  DALYTRAN-MERCHANT-ZIP                   PIC X(10).
           05  DALYTRAN-CARD-NUM                       PIC X(16).
           05  DALYTRAN-ORIG-TS                        PIC X(26).
           05  DALYTRAN-PROC-TS                        PIC X(26).
           05  FILLER                                  PIC X(20).
           05  REJ-VALIDATION-TRAILER.
               10  REJ-FAIL-REASON               PIC 9(04).
               10  REJ-FAIL-REASON-DESC          PIC X(76).
