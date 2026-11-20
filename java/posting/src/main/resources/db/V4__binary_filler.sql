alter table account alter column filler drop default;
alter table account alter column filler type bytea using convert_to(rpad(filler, 178, ' '), 'LATIN1');
alter table account alter column filler set default decode(repeat('20', 178), 'hex');

alter table tran_cat_balance alter column filler drop default;
alter table tran_cat_balance alter column filler type bytea using convert_to(rpad(filler, 22, ' '), 'LATIN1');
alter table tran_cat_balance alter column filler set default decode(repeat('20', 22), 'hex');
