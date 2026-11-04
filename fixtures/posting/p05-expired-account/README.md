# p05-expired-account
Account 1004 expired on 2022-01-31, before the transaction's origin date; CBTRN02C
rejects with reason 103 (TRANSACTION RECEIVED AFTER ACCT EXPIRATION), RC 4.
Account 1005 expires on the origin date itself (2022-07-04) and is accepted
(the test is expiry >= origin date).
