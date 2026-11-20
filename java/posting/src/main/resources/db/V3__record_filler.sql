-- FILLER bytes of CVACT01Y (178) and CVTRA01Y (22), kept so REWRITEs preserve them like COBOL does.
alter table account add column filler varchar(178) not null default '';
alter table tran_cat_balance add column filler varchar(22) not null default '';
