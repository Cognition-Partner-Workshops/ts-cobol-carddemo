# p04-overlimit-reject
Account 1003 has a 1000.00 credit limit and 950.00 already in the current cycle.
The first purchase (50.00) lands exactly on the limit and is accepted (CBTRN02C
accepts when limit >= cycle credit - cycle debit + amount); the second (100.00)
is rejected with reason 102 (OVERLIMIT TRANSACTION), RC 4.
