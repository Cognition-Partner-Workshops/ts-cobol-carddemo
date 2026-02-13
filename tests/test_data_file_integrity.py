import os
import re
import unittest

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA_DIR = os.path.join(REPO_ROOT, "app", "data", "ASCII")


def read_lines(path):
    with open(path, "r", encoding="utf-8", errors="replace") as f:
        return [line.rstrip("\n") for line in f if line.strip()]


class TestDataFilesExist(unittest.TestCase):

    def test_data_directory_exists(self):
        self.assertTrue(os.path.isdir(DATA_DIR))

    def test_acctdata_exists(self):
        self.assertTrue(os.path.isfile(os.path.join(DATA_DIR, "acctdata.txt")))

    def test_carddata_exists(self):
        self.assertTrue(os.path.isfile(os.path.join(DATA_DIR, "carddata.txt")))

    def test_cardxref_exists(self):
        self.assertTrue(os.path.isfile(os.path.join(DATA_DIR, "cardxref.txt")))

    def test_custdata_exists(self):
        self.assertTrue(os.path.isfile(os.path.join(DATA_DIR, "custdata.txt")))

    def test_dailytran_exists(self):
        self.assertTrue(os.path.isfile(os.path.join(DATA_DIR, "dailytran.txt")))

    def test_discgrp_exists(self):
        self.assertTrue(os.path.isfile(os.path.join(DATA_DIR, "discgrp.txt")))

    def test_tcatbal_exists(self):
        self.assertTrue(os.path.isfile(os.path.join(DATA_DIR, "tcatbal.txt")))

    def test_trancatg_exists(self):
        self.assertTrue(os.path.isfile(os.path.join(DATA_DIR, "trancatg.txt")))

    def test_trantype_exists(self):
        self.assertTrue(os.path.isfile(os.path.join(DATA_DIR, "trantype.txt")))


class TestAccountData(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.lines = read_lines(os.path.join(DATA_DIR, "acctdata.txt"))

    def test_has_records(self):
        self.assertGreater(len(self.lines), 0)

    def test_exactly_50_records(self):
        self.assertEqual(len(self.lines), 50)

    def test_all_records_same_length(self):
        lengths = {len(line) for line in self.lines}
        self.assertEqual(len(lengths), 1, f"Inconsistent record lengths: {lengths}")

    def test_record_length_is_300(self):
        self.assertEqual(len(self.lines[0]), 300)

    def test_account_ids_are_sequential(self):
        for i, line in enumerate(self.lines, start=1):
            acct_id = int(line[:11])
            self.assertEqual(acct_id, i, f"Account ID {acct_id} != expected {i}")

    def test_active_status_is_y(self):
        for line in self.lines:
            status = line[11]
            self.assertIn(status, ("Y", "N"), f"Invalid active status: {status}")

    def test_all_accounts_are_active(self):
        for line in self.lines:
            self.assertEqual(line[11], "Y")

    def test_dates_in_valid_format(self):
        date_pattern = re.compile(r"\d{4}-\d{2}-\d{2}")
        for line in self.lines:
            dates = date_pattern.findall(line)
            self.assertGreaterEqual(
                len(dates), 3,
                f"Expected at least 3 dates in account record",
            )

    def test_open_dates_are_valid_range(self):
        for line in self.lines:
            dates = re.findall(r"\d{4}-\d{2}-\d{2}", line)
            if dates:
                year = int(dates[0][:4])
                self.assertGreaterEqual(year, 2000)
                self.assertLessEqual(year, 2030)

    def test_account_ids_are_unique(self):
        ids = [line[:11] for line in self.lines]
        self.assertEqual(len(ids), len(set(ids)))

    def test_account_group_code_present(self):
        for line in self.lines:
            group_code = line[-10:]
            self.assertEqual(len(group_code), 10)


class TestCardData(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.lines = read_lines(os.path.join(DATA_DIR, "carddata.txt"))

    def test_has_records(self):
        self.assertGreater(len(self.lines), 0)

    def test_exactly_50_records(self):
        self.assertEqual(len(self.lines), 50)

    def test_all_records_same_length(self):
        lengths = {len(line) for line in self.lines}
        self.assertEqual(len(lengths), 1, f"Inconsistent record lengths: {lengths}")

    def test_card_numbers_are_16_digits(self):
        for line in self.lines:
            card_num = line[:16]
            self.assertTrue(card_num.isdigit(), f"Card number not numeric: {card_num}")
            self.assertEqual(len(card_num), 16)

    def test_card_numbers_are_unique(self):
        card_nums = [line[:16] for line in self.lines]
        self.assertEqual(len(card_nums), len(set(card_nums)))

    def test_account_ids_are_11_digits(self):
        for line in self.lines:
            acct_id = line[16:27]
            self.assertTrue(acct_id.isdigit(), f"Account ID not numeric: {acct_id}")

    def test_cardholder_name_present(self):
        for line in self.lines:
            name = line[27:77].strip()
            self.assertGreater(len(name), 0, "Cardholder name is empty")

    def test_expiration_date_valid(self):
        date_pattern = re.compile(r"\d{4}-\d{2}-\d{2}")
        for line in self.lines:
            dates = date_pattern.findall(line)
            self.assertGreaterEqual(len(dates), 1, "No date found in card record")

    def test_active_flag_present(self):
        for line in self.lines:
            active = line[90]
            self.assertIn(active, ("Y", "N"))


class TestCustomerData(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.lines = read_lines(os.path.join(DATA_DIR, "custdata.txt"))

    def test_has_records(self):
        self.assertGreater(len(self.lines), 0)

    def test_exactly_50_records(self):
        self.assertEqual(len(self.lines), 50)

    def test_all_records_same_length(self):
        lengths = {len(line) for line in self.lines}
        self.assertEqual(len(lengths), 1, f"Inconsistent record lengths: {lengths}")

    def test_customer_ids_are_sequential(self):
        for i, line in enumerate(self.lines, start=1):
            cust_id = int(line[:9])
            self.assertEqual(cust_id, i, f"Customer ID {cust_id} != expected {i}")

    def test_customer_ids_are_unique(self):
        ids = [line[:9] for line in self.lines]
        self.assertEqual(len(ids), len(set(ids)))

    def test_first_name_present(self):
        for line in self.lines:
            first_name = line[9:34].strip()
            self.assertGreater(len(first_name), 0, "First name is empty")

    def test_last_name_present(self):
        for line in self.lines:
            last_name = line[59:84].strip()
            self.assertGreater(len(last_name), 0, "Last name is empty")

    def test_state_code_is_two_chars(self):
        for line in self.lines:
            state = line[234:236]
            self.assertTrue(state.isalpha(), f"State code not alpha: {state}")
            self.assertEqual(len(state), 2)

    def test_country_code_is_usa(self):
        for line in self.lines:
            country = line[236:239]
            self.assertEqual(country, "USA")

    def test_phone_numbers_present(self):
        phone_pattern = re.compile(r"\(\d{3}\)\d{3}-\d{4}")
        for line in self.lines:
            phones = phone_pattern.findall(line)
            self.assertGreaterEqual(len(phones), 1, "No phone found in record")

    def test_dob_in_valid_range(self):
        for line in self.lines:
            dates = re.findall(r"\d{4}-\d{2}-\d{2}", line)
            if dates:
                year = int(dates[0][:4])
                self.assertGreaterEqual(year, 1920)
                self.assertLessEqual(year, 2010)

    def test_fico_score_in_range(self):
        for line in self.lines:
            fico = line[289:292].strip()
            if fico.isdigit():
                score = int(fico)
                self.assertGreaterEqual(score, 0)
                self.assertLessEqual(score, 999)

    def test_ssn_present(self):
        for line in self.lines:
            ssn_area = line[279:288].strip()
            self.assertTrue(ssn_area.isdigit(), f"SSN not numeric: {ssn_area}")


class TestTransactionTypeData(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.lines = read_lines(os.path.join(DATA_DIR, "trantype.txt"))

    def test_has_records(self):
        self.assertGreater(len(self.lines), 0)

    def test_all_records_same_length(self):
        lengths = {len(line) for line in self.lines}
        self.assertEqual(len(lengths), 1, f"Inconsistent record lengths: {lengths}")

    def test_type_codes_are_non_empty(self):
        for line in self.lines:
            code = line[:2].strip()
            self.assertGreater(len(code), 0)


class TestTransactionCategoryData(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.lines = read_lines(os.path.join(DATA_DIR, "trancatg.txt"))

    def test_has_records(self):
        self.assertGreater(len(self.lines), 0)

    def test_all_records_same_length(self):
        lengths = {len(line) for line in self.lines}
        self.assertEqual(len(lengths), 1, f"Inconsistent record lengths: {lengths}")


class TestDisclosureGroupData(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.lines = read_lines(os.path.join(DATA_DIR, "discgrp.txt"))

    def test_has_records(self):
        self.assertGreater(len(self.lines), 0)

    def test_all_records_same_length(self):
        lengths = {len(line) for line in self.lines}
        self.assertEqual(len(lengths), 1, f"Inconsistent record lengths: {lengths}")


class TestCardXrefData(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.lines = read_lines(os.path.join(DATA_DIR, "cardxref.txt"))

    def test_has_records(self):
        self.assertGreater(len(self.lines), 0)

    def test_all_records_same_length(self):
        lengths = {len(line) for line in self.lines}
        self.assertEqual(len(lengths), 1, f"Inconsistent record lengths: {lengths}")

    def test_exactly_50_records(self):
        self.assertEqual(len(self.lines), 50)

    def test_card_numbers_are_16_digits(self):
        for line in self.lines:
            card_num = line[:16]
            self.assertTrue(card_num.isdigit(), f"Card number not numeric: {card_num}")


class TestCategoryBalanceData(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.lines = read_lines(os.path.join(DATA_DIR, "tcatbal.txt"))

    def test_has_records(self):
        self.assertGreater(len(self.lines), 0)

    def test_all_records_same_length(self):
        lengths = {len(line) for line in self.lines}
        self.assertEqual(len(lengths), 1, f"Inconsistent record lengths: {lengths}")


class TestDailyTransactionData(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.lines = read_lines(os.path.join(DATA_DIR, "dailytran.txt"))

    def test_has_records(self):
        self.assertGreater(len(self.lines), 0)

    def test_all_records_same_length(self):
        lengths = {len(line) for line in self.lines}
        self.assertEqual(len(lengths), 1, f"Inconsistent record lengths: {lengths}")


class TestReferentialIntegrity(unittest.TestCase):

    def test_card_accounts_exist_in_acctdata(self):
        acct_lines = read_lines(os.path.join(DATA_DIR, "acctdata.txt"))
        card_lines = read_lines(os.path.join(DATA_DIR, "carddata.txt"))
        acct_ids = {int(line[:11]) for line in acct_lines}
        for line in card_lines:
            card_acct = int(line[16:27])
            self.assertIn(
                card_acct, acct_ids,
                f"Card references account {card_acct} not in acctdata",
            )

    def test_xref_card_numbers_exist_in_carddata(self):
        card_lines = read_lines(os.path.join(DATA_DIR, "carddata.txt"))
        xref_lines = read_lines(os.path.join(DATA_DIR, "cardxref.txt"))
        card_nums = {line[:16] for line in card_lines}
        for line in xref_lines:
            xref_card = line[:16]
            self.assertIn(
                xref_card, card_nums,
                f"Xref references card {xref_card} not in carddata",
            )

    def test_customer_count_matches_account_count(self):
        acct_lines = read_lines(os.path.join(DATA_DIR, "acctdata.txt"))
        cust_lines = read_lines(os.path.join(DATA_DIR, "custdata.txt"))
        self.assertEqual(len(acct_lines), len(cust_lines))

    def test_card_count_matches_xref_count(self):
        card_lines = read_lines(os.path.join(DATA_DIR, "carddata.txt"))
        xref_lines = read_lines(os.path.join(DATA_DIR, "cardxref.txt"))
        self.assertEqual(len(card_lines), len(xref_lines))


if __name__ == "__main__":
    unittest.main()
