# t05-delete-missing
Deleting type 09, which does not exist.  Db2 returns SQLCODE +100; COBTUPDT only tests
for negative SQLCODE, so it still prints RECORD DELETED SUCCESSFULLY and the table is
unchanged.  This legacy quirk is captured in the golden SYSOUT.
