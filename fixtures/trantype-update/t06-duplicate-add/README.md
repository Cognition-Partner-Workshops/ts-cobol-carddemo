# t06-duplicate-add
Inserting type 01 again violates the primary key.  Db2 would return SQLCODE -803; via
Open COBOL ESQL PostgreSQL returns a negative SQLCODE as well.  COBTUPDT reports the
error and the table is unchanged.
