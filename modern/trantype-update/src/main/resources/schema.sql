CREATE TABLE IF NOT EXISTS transaction_type (
    tr_type CHAR(2) PRIMARY KEY,
    tr_description VARCHAR(50) NOT NULL
);
CREATE TABLE IF NOT EXISTS transaction_type_categor (
    trc_type_code CHAR(2) NOT NULL REFERENCES transaction_type(tr_type) ON DELETE RESTRICT,
    trc_type_category CHAR(4) NOT NULL,
    trc_cat_data VARCHAR(50) NOT NULL,
    PRIMARY KEY (trc_type_code, trc_type_category)
);
CREATE TABLE IF NOT EXISTS maintenance_input (
    record_number BIGINT PRIMARY KEY,
    input_rec_type CHAR(1) NOT NULL,
    input_rec_number CHAR(2) NOT NULL,
    input_rec_desc CHAR(50) NOT NULL
);
