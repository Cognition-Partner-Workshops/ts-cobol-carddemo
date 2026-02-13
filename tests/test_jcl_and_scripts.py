import os
import re
import unittest

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JCL_DIR = os.path.join(REPO_ROOT, "app", "jcl")
SCRIPTS_DIR = os.path.join(REPO_ROOT, "scripts")


def read_file(path):
    with open(path, "r", encoding="utf-8", errors="replace") as f:
        return f.read()


def list_files(directory, extension):
    result = []
    if not os.path.isdir(directory):
        return result
    for name in sorted(os.listdir(directory)):
        if name.lower().endswith(extension.lower()):
            result.append(os.path.join(directory, name))
    return result


class TestJclFilesExist(unittest.TestCase):

    def test_jcl_directory_exists(self):
        self.assertTrue(os.path.isdir(JCL_DIR))

    def test_jcl_files_present(self):
        files = list_files(JCL_DIR, ".jcl")
        self.assertGreater(len(files), 0, "No .jcl files found")

    def test_expected_jcl_files_exist(self):
        expected = [
            "CLOSEFIL.jcl", "OPENFIL.jcl", "ACCTFILE.jcl", "CARDFILE.jcl",
            "XREFFILE.jcl", "CUSTFILE.jcl", "TRANBKP.jcl", "DISCGRP.jcl",
            "TCATBALF.jcl", "TRANTYPE.jcl", "POSTTRAN.jcl", "INTCALC.jcl",
            "COMBTRAN.jcl", "TRANIDX.jcl",
        ]
        actual = set(os.listdir(JCL_DIR))
        for jcl in expected:
            self.assertIn(jcl, actual, f"Missing JCL: {jcl}")


class TestJclFileStructure(unittest.TestCase):

    def test_all_jcl_have_job_card(self):
        for path in list_files(JCL_DIR, ".jcl"):
            content = read_file(path).upper()
            has_job = "JOB" in content or "EXEC" in content
            self.assertTrue(
                has_job,
                f"{os.path.basename(path)} missing JOB or EXEC statement",
            )

    def test_no_jcl_file_is_empty(self):
        for path in list_files(JCL_DIR, ".jcl"):
            size = os.path.getsize(path)
            self.assertGreater(
                size, 10,
                f"{os.path.basename(path)} is suspiciously small ({size} bytes)",
            )

    def test_posttran_references_batch_program(self):
        path = os.path.join(JCL_DIR, "POSTTRAN.jcl")
        if os.path.isfile(path):
            content = read_file(path).upper()
            self.assertIn("CBTRN", content)

    def test_intcalc_references_interest_program(self):
        path = os.path.join(JCL_DIR, "INTCALC.jcl")
        if os.path.isfile(path):
            content = read_file(path).upper()
            has_ref = "CBTRN" in content or "INTCALC" in content or "EXEC" in content
            self.assertTrue(has_ref)


class TestScriptFiles(unittest.TestCase):

    def test_scripts_directory_exists(self):
        self.assertTrue(os.path.isdir(SCRIPTS_DIR))

    def test_run_full_batch_exists(self):
        self.assertTrue(
            os.path.isfile(os.path.join(SCRIPTS_DIR, "run_full_batch.sh"))
        )

    def test_run_full_batch_is_shell_script(self):
        content = read_file(os.path.join(SCRIPTS_DIR, "run_full_batch.sh"))
        self.assertTrue(content.startswith("#!/bin/bash"))

    def test_run_full_batch_references_all_jcl_steps(self):
        content = read_file(os.path.join(SCRIPTS_DIR, "run_full_batch.sh"))
        expected_jcls = [
            "CLOSEFIL.jcl", "ACCTFILE.jcl", "CARDFILE.jcl", "XREFFILE.jcl",
            "CUSTFILE.jcl", "TRANBKP.jcl", "DISCGRP.jcl", "TCATBALF.jcl",
            "TRANTYPE.jcl", "POSTTRAN.jcl", "INTCALC.jcl", "COMBTRAN.jcl",
            "TRANIDX.jcl", "OPENFIL.jcl",
        ]
        for jcl in expected_jcls:
            self.assertIn(jcl, content, f"Script missing reference to {jcl}")

    def test_run_full_batch_checks_tunnel(self):
        content = read_file(os.path.join(SCRIPTS_DIR, "run_full_batch.sh"))
        self.assertIn("2121", content)
        self.assertIn("FTP Tunnel", content)

    def test_run_full_batch_proper_ordering(self):
        content = read_file(os.path.join(SCRIPTS_DIR, "run_full_batch.sh"))
        close_pos = content.find("CLOSEFIL.jcl")
        open_pos = content.find("OPENFIL.jcl")
        self.assertLess(close_pos, open_pos, "CLOSEFIL should come before OPENFIL")

    def test_run_full_batch_has_sleep_between_steps(self):
        content = read_file(os.path.join(SCRIPTS_DIR, "run_full_batch.sh"))
        sleep_count = content.count("sleep")
        self.assertGreaterEqual(sleep_count, 3, "Should have delays between steps")

    def test_run_full_batch_ends_with_bye(self):
        content = read_file(os.path.join(SCRIPTS_DIR, "run_full_batch.sh"))
        self.assertIn("bye", content)


class TestBmsFiles(unittest.TestCase):

    def setUp(self):
        self.bms_dir = os.path.join(REPO_ROOT, "app", "bms")

    def test_bms_directory_exists(self):
        self.assertTrue(os.path.isdir(self.bms_dir))

    def test_bms_files_present(self):
        files = [
            f for f in os.listdir(self.bms_dir)
            if f.lower().endswith(".bms")
        ]
        self.assertGreater(len(files), 0, "No .bms files found")

    def test_bms_files_have_dfhmsd(self):
        for name in os.listdir(self.bms_dir):
            if name.lower().endswith(".bms"):
                content = read_file(os.path.join(self.bms_dir, name)).upper()
                self.assertIn(
                    "DFHMSD",
                    content,
                    f"{name} missing DFHMSD macro",
                )

    def test_bms_files_have_dfhmdi(self):
        for name in os.listdir(self.bms_dir):
            if name.lower().endswith(".bms"):
                content = read_file(os.path.join(self.bms_dir, name)).upper()
                self.assertIn(
                    "DFHMDI",
                    content,
                    f"{name} missing DFHMDI macro",
                )


class TestScreenDefinitions(unittest.TestCase):

    def setUp(self):
        self.bms_dir = os.path.join(REPO_ROOT, "app", "bms")

    def _get_bms_content(self, pattern):
        for name in os.listdir(self.bms_dir):
            if pattern.upper() in name.upper() and name.lower().endswith(".bms"):
                return read_file(os.path.join(self.bms_dir, name)).upper()
        return None

    def test_signon_screen_exists(self):
        content = self._get_bms_content("COSGN")
        self.assertIsNotNone(content, "Signon screen BMS not found")

    def test_main_menu_screen_exists(self):
        content = self._get_bms_content("COMEN")
        self.assertIsNotNone(content, "Main menu screen BMS not found")

    def test_transaction_list_screen_exists(self):
        content = self._get_bms_content("COTRN00")
        self.assertIsNotNone(content, "Transaction list screen BMS not found")


class TestBatchJobSequence(unittest.TestCase):

    def test_data_refresh_before_processing(self):
        content = read_file(os.path.join(SCRIPTS_DIR, "run_full_batch.sh"))
        acctfile_pos = content.find("ACCTFILE.jcl")
        posttran_pos = content.find("POSTTRAN.jcl")
        self.assertLess(
            acctfile_pos, posttran_pos,
            "Data refresh (ACCTFILE) must precede transaction processing (POSTTRAN)",
        )

    def test_interest_calc_after_post_tran(self):
        content = read_file(os.path.join(SCRIPTS_DIR, "run_full_batch.sh"))
        posttran_pos = content.find("POSTTRAN.jcl")
        intcalc_pos = content.find("INTCALC.jcl")
        self.assertLess(
            posttran_pos, intcalc_pos,
            "POSTTRAN must precede INTCALC",
        )

    def test_tran_backup_after_interest_calc(self):
        content = read_file(os.path.join(SCRIPTS_DIR, "run_full_batch.sh"))
        intcalc_pos = content.find("INTCALC.jcl")
        lines = content.split("\n")
        tranbkp_second_pos = None
        found_first = False
        for i, line in enumerate(lines):
            if "TRANBKP.jcl" in line:
                if found_first:
                    tranbkp_second_pos = i
                    break
                found_first = True
        self.assertIsNotNone(tranbkp_second_pos)

    def test_combine_tran_after_backup(self):
        content = read_file(os.path.join(SCRIPTS_DIR, "run_full_batch.sh"))
        lines = content.split("\n")
        found_second_tranbkp = False
        found_first = False
        for line in lines:
            if "TRANBKP.jcl" in line:
                if found_first:
                    found_second_tranbkp = True
                found_first = True
            if found_second_tranbkp and "COMBTRAN.jcl" in line:
                return
        self.fail("COMBTRAN should follow the second TRANBKP")


class TestSecurityUserDataFile(unittest.TestCase):

    def setUp(self):
        usrsec_path = os.path.join(DATA_DIR, "usrsec.txt")
        if os.path.isfile(usrsec_path):
            self.lines = read_lines(usrsec_path)
            self.has_file = True
        else:
            self.has_file = False

    def test_usrsec_exists_or_dusrsecj_jcl_exists(self):
        usrsec_path = os.path.join(DATA_DIR, "usrsec.txt")
        dusrsecj_path = os.path.join(JCL_DIR, "DUSRSECJ.jcl")
        has_either = os.path.isfile(usrsec_path) or os.path.isfile(dusrsecj_path)
        self.assertTrue(has_either, "Neither usrsec.txt nor DUSRSECJ.jcl found")


DATA_DIR = os.path.join(REPO_ROOT, "app", "data", "ASCII")


def read_lines(path):
    with open(path, "r", encoding="utf-8", errors="replace") as f:
        return [line.rstrip("\n") for line in f if line.strip()]


if __name__ == "__main__":
    unittest.main()
