alter table account add column filler bytea not null default decode(repeat('20', 178), 'hex');
alter table tran_cat_balance add column filler bytea not null default decode(repeat('20', 22), 'hex');
