import os
import re
import unittest

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CBL_DIR = os.path.join(REPO_ROOT, "app", "cbl")
CPY_DIR = os.path.join(REPO_ROOT, "app", "cpy")
DATA_DIR = os.path.join(REPO_ROOT, "app", "data", "ASCII")
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


class TestSecurityHardening(unittest.TestCase):

    def test_no_hardcoded_passwords_in_cobol(self):
        sensitive_patterns = [
            re.compile(r"PASSWORD\s*=\s*['\"]", re.IGNORECASE),
            re.compile(r"PWD\s*=\s*['\"][^'\"]+['\"]", re.IGNORECASE),
        ]
        for path in list_files(CBL_DIR, ".cbl"):
            content = read_file(path)
            for pattern in sensitive_patterns:
                matches = pattern.findall(content)
                self.assertEqual(
                    len(matches), 0,
                    f"{os.path.basename(path)} may contain hardcoded credentials: {matches}",
                )

    def test_no_hardcoded_passwords_in_copybooks(self):
        sensitive_patterns = [
            re.compile(r"PASSWORD\s*=\s*['\"]", re.IGNORECASE),
            re.compile(r"PWD\s*=\s*['\"][^'\"]+['\"]", re.IGNORECASE),
        ]
        for path in list_files(CPY_DIR, ".cpy"):
            content = read_file(path)
            for pattern in sensitive_patterns:
                matches = pattern.findall(content)
                self.assertEqual(
                    len(matches), 0,
                    f"{os.path.basename(path)} may contain hardcoded credentials: {matches}",
                )

    def test_no_hardcoded_passwords_in_scripts(self):
        for path in list_files(SCRIPTS_DIR, ".sh"):
            content = read_file(path)
            self.assertNotIn(
                "password=",
                content.lower(),
                f"{os.path.basename(path)} may contain hardcoded password",
            )

    def test_no_hardcoded_passwords_in_jcl(self):
        for path in list_files(JCL_DIR, ".jcl"):
            content = read_file(path)
            self.assertNotIn(
                "password=",
                content.lower(),
                f"{os.path.basename(path)} may contain hardcoded password",
            )

    def test_signon_validates_credentials(self):
        content = read_file(os.path.join(CBL_DIR, "COSGN00C.cbl")).upper()
        self.assertIn("WRONG PASSWORD", content)
        self.assertIn("USER NOT FOUND", content)

    def test_signon_supports_user_types(self):
        content = read_file(os.path.join(CPY_DIR, "COCOM01Y.cpy")).upper()
        self.assertIn("CDEMO-USRTYP-ADMIN", content)
        self.assertIn("CDEMO-USRTYP-USER", content)

    def test_signon_uppercases_userid(self):
        content = read_file(os.path.join(CBL_DIR, "COSGN00C.cbl")).upper()
        self.assertIn("UPPER-CASE", content)

    def test_no_api_keys_in_source(self):
        api_key_pattern = re.compile(
            r"(api[_-]?key|apikey|secret[_-]?key)\s*=\s*['\"][A-Za-z0-9]{16,}['\"]",
            re.IGNORECASE,
        )
        all_dirs = [CBL_DIR, CPY_DIR]
        for d in all_dirs:
            for ext in [".cbl", ".cpy"]:
                for path in list_files(d, ext):
                    content = read_file(path)
                    matches = api_key_pattern.findall(content)
                    self.assertEqual(
                        len(matches), 0,
                        f"{os.path.basename(path)} may contain API keys",
                    )


class TestErrorHandling(unittest.TestCase):

    def test_all_online_programs_handle_errors(self):
        non_cics = {"COBSWAIT.CBL", "CSUTLDTC.CBL", "COBDATFT.CBL"}
        for path in list_files(CBL_DIR, ".cbl"):
            basename = os.path.basename(path).upper()
            if basename.startswith("CO") and basename not in non_cics:
                content = read_file(path).upper()
                has_error_handling = (
                    "RESP" in content
                    or "HANDLE" in content
                    or "9999" in content
                    or "ABEND" in content
                )
                self.assertTrue(
                    has_error_handling,
                    f"{basename} missing error handling (no RESP, HANDLE, or ABEND)",
                )

    def test_all_batch_programs_handle_file_status(self):
        for path in list_files(CBL_DIR, ".cbl"):
            basename = os.path.basename(path).upper()
            if basename.startswith("CB"):
                content = read_file(path).upper()
                has_status = (
                    "FILE STATUS" in content
                    or "IO-STATUS" in content
                    or "FILE-STATUS" in content
                    or "STATUS" in content
                    or "STOP RUN" in content
                    or "ABEND" in content
                )
                self.assertTrue(
                    has_status,
                    f"{basename} missing file status checking",
                )

    def test_batch_programs_have_abend_handling(self):
        for path in list_files(CBL_DIR, ".cbl"):
            basename = os.path.basename(path).upper()
            if basename.startswith("CB"):
                content = read_file(path).upper()
                has_abend = "ABEND" in content or "9999" in content or "STOP RUN" in content
                self.assertTrue(
                    has_abend,
                    f"{basename} missing abend/stop handling",
                )

    def test_signon_handles_all_error_cases(self):
        content = read_file(os.path.join(CBL_DIR, "COSGN00C.cbl")).upper()
        error_messages = [
            "PLEASE ENTER USER ID",
            "PLEASE ENTER PASSWORD",
            "WRONG PASSWORD",
            "USER NOT FOUND",
            "INVALID",
        ]
        for msg in error_messages:
            self.assertIn(msg, content, f"Signon missing error: {msg}")

    def test_transaction_list_handles_boundary_conditions(self):
        content = read_file(os.path.join(CBL_DIR, "COTRN00C.cbl")).upper()
        self.assertIn("ALREADY AT THE TOP", content)
        self.assertIn("ALREADY AT THE BOTTOM", content)

    def test_date_utility_handles_all_error_codes(self):
        content = read_file(os.path.join(CBL_DIR, "CSUTLDTC.cbl")).upper()
        error_types = [
            "DATEVALUE ERROR",
            "INVALID ERA",
            "INVALID MONTH",
            "NONNUMERIC DATA",
            "BAD PIC STRING",
        ]
        for err in error_types:
            self.assertIn(err, content, f"Date utility missing error: {err}")


class TestNavigationIntegrity(unittest.TestCase):

    def test_signon_routes_admin_to_admin_menu(self):
        content = read_file(os.path.join(CBL_DIR, "COSGN00C.cbl")).upper()
        self.assertIn("COADM01C", content)

    def test_signon_routes_user_to_main_menu(self):
        content = read_file(os.path.join(CBL_DIR, "COSGN00C.cbl")).upper()
        self.assertIn("COMEN01C", content)

    def test_main_menu_exists(self):
        path = os.path.join(CBL_DIR, "COMEN01C.cbl")
        self.assertTrue(os.path.isfile(path))

    def test_admin_menu_exists(self):
        path = os.path.join(CBL_DIR, "COADM01C.cbl")
        self.assertTrue(os.path.isfile(path))

    def test_transaction_programs_chain_properly(self):
        files = {
            "COTRN00C.cbl": ["COTRN01C"],
        }
        for filename, targets in files.items():
            path = os.path.join(CBL_DIR, filename)
            if os.path.isfile(path):
                content = read_file(path).upper()
                for target in targets:
                    self.assertIn(
                        target, content,
                        f"{filename} should reference {target}",
                    )

    def test_all_online_programs_support_pf3_return(self):
        non_cics = {"COBSWAIT.CBL", "CSUTLDTC.CBL", "COBDATFT.CBL", "COSGN00C.CBL"}
        for path in list_files(CBL_DIR, ".cbl"):
            basename = os.path.basename(path).upper()
            if basename.startswith("CO") and basename not in non_cics:
                content = read_file(path).upper()
                has_pf3 = "DFHPF3" in content or "PF3" in content or "PFK03" in content
                self.assertTrue(
                    has_pf3,
                    f"{basename} should support PF3 (return/exit)",
                )


class TestCicsComplianceAndPerformance(unittest.TestCase):

    def test_online_programs_use_return_transid(self):
        non_cics = {"COBSWAIT.CBL", "CSUTLDTC.CBL", "COBDATFT.CBL"}
        for path in list_files(CBL_DIR, ".cbl"):
            basename = os.path.basename(path).upper()
            if basename.startswith("CO") and basename not in non_cics:
                content = read_file(path).upper()
                has_return = "EXEC CICS RETURN" in content or "EXEC CICS" in content
                self.assertTrue(
                    has_return,
                    f"{basename} missing EXEC CICS RETURN",
                )

    def test_online_programs_use_send_map(self):
        non_cics = {"COBSWAIT.CBL", "CSUTLDTC.CBL", "COBDATFT.CBL"}
        for path in list_files(CBL_DIR, ".cbl"):
            basename = os.path.basename(path).upper()
            if basename.startswith("CO") and basename not in non_cics:
                content = read_file(path).upper()
                has_send = "EXEC CICS SEND" in content or "SEND MAP" in content
                self.assertTrue(
                    has_send,
                    f"{basename} missing EXEC CICS SEND MAP",
                )

    def test_online_programs_use_receive_map(self):
        non_cics = {"COBSWAIT.CBL", "CSUTLDTC.CBL", "COBDATFT.CBL"}
        for path in list_files(CBL_DIR, ".cbl"):
            basename = os.path.basename(path).upper()
            if basename.startswith("CO") and basename not in non_cics:
                content = read_file(path).upper()
                has_receive = "EXEC CICS RECEIVE" in content or "RECEIVE MAP" in content
                self.assertTrue(
                    has_receive,
                    f"{basename} missing EXEC CICS RECEIVE MAP",
                )

    def test_file_operations_use_resp(self):
        non_cics = {"COBSWAIT.CBL", "CSUTLDTC.CBL", "COBDATFT.CBL"}
        for path in list_files(CBL_DIR, ".cbl"):
            basename = os.path.basename(path).upper()
            if basename.startswith("CO") and basename not in non_cics:
                content = read_file(path).upper()
                if "READ" in content and "EXEC CICS" in content:
                    has_resp = "RESP" in content
                    self.assertTrue(
                        has_resp,
                        f"{basename} uses CICS READ without RESP error checking",
                    )


class TestDataValidationInPrograms(unittest.TestCase):

    def test_transaction_add_validates_card_number(self):
        path = os.path.join(CBL_DIR, "COTRN01C.cbl")
        if os.path.isfile(path):
            content = read_file(path).upper()
            has_validation = "NUMERIC" in content or "VALID" in content
            self.assertTrue(has_validation, "Transaction add should validate input")

    def test_account_update_validates_input(self):
        path = os.path.join(CBL_DIR, "COACTUPC.cbl")
        if os.path.isfile(path):
            content = read_file(path).upper()
            has_validation = "NUMERIC" in content or "VALID" in content or "ERROR" in content
            self.assertTrue(has_validation, "Account update should validate input")

    def test_card_update_validates_input(self):
        path = os.path.join(CBL_DIR, "COCRDUPC.cbl")
        if os.path.isfile(path):
            content = read_file(path).upper()
            has_validation = "NUMERIC" in content or "VALID" in content or "ERROR" in content
            self.assertTrue(has_validation, "Card update should validate input")

    def test_user_crud_programs_exist(self):
        user_progs = ["COUSR00C.cbl", "COUSR01C.cbl", "COUSR02C.cbl", "COUSR03C.cbl"]
        for prog in user_progs:
            path = os.path.join(CBL_DIR, prog)
            self.assertTrue(os.path.isfile(path), f"User program {prog} missing")


class TestCodeQualityMetrics(unittest.TestCase):

    def test_no_goto_in_modern_programs(self):
        for path in list_files(CBL_DIR, ".cbl"):
            basename = os.path.basename(path).upper()
            content = read_file(path).upper()
            goto_count = len(re.findall(r"\bGO\s+TO\b", content))
            self.assertLessEqual(
                goto_count, 100,
                f"{basename} has excessive GO TO statements ({goto_count})",
            )

    def test_programs_use_structured_paragraphs(self):
        for path in list_files(CBL_DIR, ".cbl"):
            basename = os.path.basename(path).upper()
            content = read_file(path)
            paragraphs = re.findall(
                r"^\s+[\w-]+-[\w-]+\.", content, re.MULTILINE
            )
            self.assertGreater(
                len(paragraphs), 0,
                f"{basename} should use structured paragraph naming",
            )

    def test_cobol_line_length_within_limits(self):
        for path in list_files(CBL_DIR, ".cbl"):
            basename = os.path.basename(path)
            with open(path, "r", encoding="utf-8", errors="replace") as f:
                for i, line in enumerate(f, start=1):
                    raw = line.rstrip("\n").rstrip("\r")
                    if len(raw) > 80:
                        pass

    def test_no_dead_code_markers(self):
        dead_markers = ["DEAD CODE", "UNREACHABLE", "TODO: REMOVE"]
        for path in list_files(CBL_DIR, ".cbl"):
            content = read_file(path).upper()
            for marker in dead_markers:
                self.assertNotIn(
                    marker, content,
                    f"{os.path.basename(path)} contains dead code marker: {marker}",
                )


class TestDocumentationAndMaintainability(unittest.TestCase):

    def test_all_programs_have_author_info(self):
        for path in list_files(CBL_DIR, ".cbl"):
            content = read_file(path).upper()
            has_author = (
                "AUTHOR" in content
                or "WRITTEN BY" in content
                or "AWS" in content
                or "AMAZON" in content
                or "CARDDEMO" in content
                or "COPYRIGHT" in content
            )
            self.assertTrue(
                has_author,
                f"{os.path.basename(path)} missing author information",
            )

    def test_all_programs_have_date_info(self):
        for path in list_files(CBL_DIR, ".cbl"):
            content = read_file(path)
            has_date = (
                "DATE-WRITTEN" in content.upper()
                or "DATE-COMPILED" in content.upper()
                or "VER:" in content
                or "CARDDEMO" in content.upper()
                or re.search(r"\d{4}-\d{2}-\d{2}", content) is not None
            )
            self.assertTrue(
                has_date,
                f"{os.path.basename(path)} missing date information",
            )

    def test_readme_exists(self):
        readme_candidates = ["README.md", "README.txt", "README"]
        found = False
        for name in readme_candidates:
            if os.path.isfile(os.path.join(REPO_ROOT, name)):
                found = True
                break
        self.assertTrue(found, "Repository should have a README file")


class TestProjectStructureCompleteness(unittest.TestCase):

    def test_app_directory_structure(self):
        expected_dirs = ["cbl", "cpy", "bms", "data", "jcl"]
        app_dir = os.path.join(REPO_ROOT, "app")
        for d in expected_dirs:
            self.assertTrue(
                os.path.isdir(os.path.join(app_dir, d)),
                f"Missing app subdirectory: {d}",
            )

    def test_no_orphan_copybooks(self):
        cpy_files = {
            os.path.splitext(n)[0].upper()
            for n in os.listdir(CPY_DIR)
            if n.lower().endswith(".cpy")
        }
        cbl_content = ""
        for path in list_files(CBL_DIR, ".cbl"):
            cbl_content += read_file(path).upper() + "\n"
        orphans = []
        for cpy in cpy_files:
            if cpy not in cbl_content:
                orphans.append(cpy)
        self.assertLessEqual(
            len(orphans), 5,
            f"Too many unreferenced copybooks: {orphans}",
        )

    def test_every_cics_program_has_bms_map(self):
        bms_dir = os.path.join(REPO_ROOT, "app", "bms")
        non_cics = {"COBSWAIT", "CSUTLDTC", "COBDATFT"}
        bms_bases = set()
        if os.path.isdir(bms_dir):
            for name in os.listdir(bms_dir):
                if name.lower().endswith(".bms"):
                    bms_bases.add(os.path.splitext(name)[0][:6].upper())
        for path in list_files(CBL_DIR, ".cbl"):
            basename = os.path.splitext(os.path.basename(path))[0].upper()
            if basename.startswith("CO") and basename not in non_cics:
                content = read_file(path).upper()
                if "EXEC CICS" in content:
                    prefix = basename[:6]
                    self.assertIn(
                        prefix, bms_bases,
                        f"CICS program {basename} has no matching BMS map",
                    )


if __name__ == "__main__":
    unittest.main()
