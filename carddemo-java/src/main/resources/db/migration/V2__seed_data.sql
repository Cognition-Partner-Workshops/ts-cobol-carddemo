-- CardDemo Seed Data - Default users and reference data
-- Migrated from USRSEC VSAM file and reference data files

-- Default users (from USRSEC VSAM file)
INSERT INTO users (usr_id, usr_fname, usr_lname, usr_pwd, usr_type) VALUES ('ADMIN001', 'ADMIN', 'USER', 'PASSWORD', 'A');
INSERT INTO users (usr_id, usr_fname, usr_lname, usr_pwd, usr_type) VALUES ('USER0001', 'REGULAR', 'USER', 'PASSWORD', 'R');

-- Transaction types (from TRANTYPE.PS)
INSERT INTO transaction_types (tran_type, tran_type_desc) VALUES ('01', 'Purchase');
INSERT INTO transaction_types (tran_type, tran_type_desc) VALUES ('02', 'Cash Advance');
INSERT INTO transaction_types (tran_type, tran_type_desc) VALUES ('03', 'Balance Transfer');
INSERT INTO transaction_types (tran_type, tran_type_desc) VALUES ('04', 'Payment');
INSERT INTO transaction_types (tran_type, tran_type_desc) VALUES ('05', 'Fee');

-- Transaction categories (from TRANCATG.PS)
INSERT INTO transaction_categories (tran_type_cd, tran_cat_cd, tran_cat_type_desc) VALUES ('01', 1, 'Retail Purchase');
INSERT INTO transaction_categories (tran_type_cd, tran_cat_cd, tran_cat_type_desc) VALUES ('01', 2, 'Online Purchase');
INSERT INTO transaction_categories (tran_type_cd, tran_cat_cd, tran_cat_type_desc) VALUES ('01', 3, 'Travel Purchase');
INSERT INTO transaction_categories (tran_type_cd, tran_cat_cd, tran_cat_type_desc) VALUES ('01', 4, 'Recurring Payment');
INSERT INTO transaction_categories (tran_type_cd, tran_cat_cd, tran_cat_type_desc) VALUES ('01', 5, 'Interest Charge');
INSERT INTO transaction_categories (tran_type_cd, tran_cat_cd, tran_cat_type_desc) VALUES ('02', 1, 'ATM Cash Advance');
INSERT INTO transaction_categories (tran_type_cd, tran_cat_cd, tran_cat_type_desc) VALUES ('02', 2, 'Counter Cash Advance');
INSERT INTO transaction_categories (tran_type_cd, tran_cat_cd, tran_cat_type_desc) VALUES ('03', 1, 'Standard Balance Transfer');
INSERT INTO transaction_categories (tran_type_cd, tran_cat_cd, tran_cat_type_desc) VALUES ('04', 1, 'Online Payment');
INSERT INTO transaction_categories (tran_type_cd, tran_cat_cd, tran_cat_type_desc) VALUES ('04', 2, 'Check Payment');
INSERT INTO transaction_categories (tran_type_cd, tran_cat_cd, tran_cat_type_desc) VALUES ('05', 1, 'Annual Fee');
INSERT INTO transaction_categories (tran_type_cd, tran_cat_cd, tran_cat_type_desc) VALUES ('05', 2, 'Late Fee');

-- DB2 Transaction Type (optional module)
INSERT INTO transaction_type (tr_type, tr_description) VALUES ('01', 'Purchase');
INSERT INTO transaction_type (tr_type, tr_description) VALUES ('02', 'Cash Advance');
INSERT INTO transaction_type (tr_type, tr_description) VALUES ('03', 'Balance Transfer');
INSERT INTO transaction_type (tr_type, tr_description) VALUES ('04', 'Payment');
INSERT INTO transaction_type (tr_type, tr_description) VALUES ('05', 'Fee');

-- Disclosure groups with default interest rates
INSERT INTO disclosure_groups (dis_acct_group_id, dis_tran_type_cd, dis_tran_cat_cd, dis_int_rate) VALUES ('DEFAULT', '01', 1, 19.99);
INSERT INTO disclosure_groups (dis_acct_group_id, dis_tran_type_cd, dis_tran_cat_cd, dis_int_rate) VALUES ('DEFAULT', '01', 2, 19.99);
INSERT INTO disclosure_groups (dis_acct_group_id, dis_tran_type_cd, dis_tran_cat_cd, dis_int_rate) VALUES ('DEFAULT', '01', 3, 19.99);
INSERT INTO disclosure_groups (dis_acct_group_id, dis_tran_type_cd, dis_tran_cat_cd, dis_int_rate) VALUES ('DEFAULT', '01', 4, 19.99);
INSERT INTO disclosure_groups (dis_acct_group_id, dis_tran_type_cd, dis_tran_cat_cd, dis_int_rate) VALUES ('DEFAULT', '01', 5, 0.00);
INSERT INTO disclosure_groups (dis_acct_group_id, dis_tran_type_cd, dis_tran_cat_cd, dis_int_rate) VALUES ('DEFAULT', '02', 1, 24.99);
INSERT INTO disclosure_groups (dis_acct_group_id, dis_tran_type_cd, dis_tran_cat_cd, dis_int_rate) VALUES ('DEFAULT', '02', 2, 24.99);
INSERT INTO disclosure_groups (dis_acct_group_id, dis_tran_type_cd, dis_tran_cat_cd, dis_int_rate) VALUES ('DEFAULT', '03', 1, 14.99);
INSERT INTO disclosure_groups (dis_acct_group_id, dis_tran_type_cd, dis_tran_cat_cd, dis_int_rate) VALUES ('DEFAULT', '04', 1, 0.00);
INSERT INTO disclosure_groups (dis_acct_group_id, dis_tran_type_cd, dis_tran_cat_cd, dis_int_rate) VALUES ('DEFAULT', '04', 2, 0.00);
INSERT INTO disclosure_groups (dis_acct_group_id, dis_tran_type_cd, dis_tran_cat_cd, dis_int_rate) VALUES ('DEFAULT', '05', 1, 0.00);
INSERT INTO disclosure_groups (dis_acct_group_id, dis_tran_type_cd, dis_tran_cat_cd, dis_int_rate) VALUES ('DEFAULT', '05', 2, 0.00);
