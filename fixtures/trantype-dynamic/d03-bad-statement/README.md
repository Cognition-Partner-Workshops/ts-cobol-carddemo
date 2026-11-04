# d03-bad-statement
The dynamic statement names a column that does not exist.  EXECUTE fails with SQLCODE
-400 through Open COBOL ESQL, CBTRNDYN sets RC 8 and still calls COBTUPDT.  Under
PostgreSQL the failed statement has aborted the whole unit of work, so COBTUPDT's
UPDATE also fails (SQLCODE -400) and the table is unchanged.  Db2 would have rolled
back only the failed statement and applied the update: a genuine semantic gap for the
migration team to decide on (savepoint per statement, or fail the job).
