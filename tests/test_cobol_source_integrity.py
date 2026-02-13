import os
import re
import unittest

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CBL_DIR = os.path.join(REPO_ROOT, "app", "cbl")
CPY_DIR = os.path.join(REPO_ROOT, "app", "cpy")
BMS_DIR = os.path.join(REPO_ROOT, "app", "bms")


def read_file(path):
    with open(path, "r", encoding="utf-8", errors="replace") as f:
        return f.read()


def list_files(directory, extension):
    result = []
    for name in sorted(os.listdir(directory)):
        if name.lower().endswith(extension.lower()):
            result.append(os.path.join(directory, name))
    return result


class TestCobolSourcePresence(unittest.TestCase):

    def test_cbl_directory_exists(self):
        self.assertTrue(os.path.isdir(CBL_DIR))

    def test_cpy_directory_exists(self):
        self.assertTrue(os.path.isdir(CPY_DIR))

    def test_cbl_files_not_empty(self):
        files = list_files(CBL_DIR, ".cbl")
        self.assertGreater(len(files), 0, "No .cbl files found")

    def test_cpy_files_not_empty(self):
        files = list_files(CPY_DIR, ".cpy")
        self.assertGreater(len(files), 0, "No .cpy files found")

    def test_expected_online_programs_exist(self):
        expected = [
            "COSGN00C.cbl", "COMEN01C.cbl", "COADM01C.cbl",
            "COTRN00C.cbl", "COTRN01C.cbl", "COTRN02C.cbl",
            "COBIL00C.cbl", "COCRDLIC.cbl", "COCRDSLC.cbl",
            "COCRDUPC.cbl", "COACTUPC.cbl", "COACTVWC.cbl",
            "COUSR00C.cbl", "COUSR01C.cbl", "COUSR02C.cbl",
            "COUSR03C.cbl", "CORPT00C.cbl",
        ]
        actual = set(os.listdir(CBL_DIR))
        for prog in expected:
            self.assertIn(prog, actual, f"Missing online program: {prog}")

    def test_expected_batch_programs_exist(self):
        expected = [
            "CBACT01C.cbl", "CBACT02C.cbl", "CBACT03C.cbl", "CBACT04C.cbl",
            "CBTRN01C.cbl", "CBTRN02C.cbl", "CBTRN03C.cbl",
            "CBCUS01C.cbl", "CBSTM03A.CBL", "CBSTM03B.CBL",
        ]
        actual_lower = {n.lower() for n in os.listdir(CBL_DIR)}
        for prog in expected:
            self.assertIn(prog.lower(), actual_lower, f"Missing batch program: {prog}")

    def test_expected_copybooks_exist(self):
        expected = [
            "COCOM01Y.cpy", "CVACT01Y.cpy", "CSUSR01Y.cpy",
            "COTTL01Y.cpy", "CSDAT01Y.cpy", "CSMSG01Y.cpy",
            "CVTRA05Y.cpy", "CVCRD01Y.cpy", "CVCUS01Y.cpy",
        ]
        actual = set(os.listdir(CPY_DIR))
        for cpybook in expected:
            self.assertIn(cpybook, actual, f"Missing copybook: {cpybook}")


class TestCobolSourceStructure(unittest.TestCase):

    def test_all_cbl_have_identification_division(self):
        for path in list_files(CBL_DIR, ".cbl"):
            content = read_file(path).upper()
            self.assertIn(
                "IDENTIFICATION DIVISION",
                content,
                f"{os.path.basename(path)} missing IDENTIFICATION DIVISION",
            )

    def test_all_cbl_have_program_id(self):
        for path in list_files(CBL_DIR, ".cbl"):
            content = read_file(path).upper()
            self.assertIn(
                "PROGRAM-ID",
                content,
                f"{os.path.basename(path)} missing PROGRAM-ID",
            )

    def test_all_cbl_have_procedure_division(self):
        for path in list_files(CBL_DIR, ".cbl"):
            content = read_file(path).upper()
            self.assertIn(
                "PROCEDURE DIVISION",
                content,
                f"{os.path.basename(path)} missing PROCEDURE DIVISION",
            )

    def test_program_id_matches_filename(self):
        for path in list_files(CBL_DIR, ".cbl"):
            content = read_file(path).upper()
            basename = os.path.splitext(os.path.basename(path))[0].upper()
            match = re.search(r"PROGRAM-ID\.\s+(\S+)", content)
            if match:
                prog_id = match.group(1).rstrip(".")
                self.assertEqual(
                    prog_id,
                    basename,
                    f"PROGRAM-ID {prog_id} doesn't match filename {basename}",
                )

    def test_online_programs_have_cics_commands(self):
        non_cics = {"COBSWAIT.CBL", "CSUTLDTC.CBL", "COBDATFT.CBL"}
        for path in list_files(CBL_DIR, ".cbl"):
            basename = os.path.basename(path).upper()
            if basename.startswith("CO") and basename not in non_cics:
                content = read_file(path).upper()
                self.assertIn(
                    "EXEC CICS",
                    content,
                    f"Online program {basename} missing EXEC CICS commands",
                )

    def test_online_programs_have_commarea(self):
        non_cics = {"COBSWAIT.CBL", "CSUTLDTC.CBL", "COBDATFT.CBL"}
        for path in list_files(CBL_DIR, ".cbl"):
            basename = os.path.basename(path).upper()
            if basename.startswith("CO") and basename not in non_cics:
                content = read_file(path).upper()
                self.assertIn(
                    "DFHCOMMAREA",
                    content,
                    f"Online program {basename} missing DFHCOMMAREA",
                )

    def test_batch_programs_have_file_section(self):
        batch_prefixes = ("CB",)
        for path in list_files(CBL_DIR, ".cbl"):
            basename = os.path.basename(path).upper()
            if basename.startswith(batch_prefixes):
                content = read_file(path).upper()
                has_file_section = "FILE SECTION" in content or "FILE-CONTROL" in content
                self.assertTrue(
                    has_file_section,
                    f"Batch program {basename} missing FILE SECTION or FILE-CONTROL",
                )

    def test_no_cbl_file_is_empty(self):
        for path in list_files(CBL_DIR, ".cbl"):
            size = os.path.getsize(path)
            self.assertGreater(
                size, 100,
                f"{os.path.basename(path)} is suspiciously small ({size} bytes)",
            )

    def test_no_cpy_file_is_empty(self):
        for path in list_files(CPY_DIR, ".cpy"):
            size = os.path.getsize(path)
            self.assertGreater(
                size, 10,
                f"{os.path.basename(path)} is suspiciously small ({size} bytes)",
            )


class TestCopybookReferences(unittest.TestCase):

    def _get_copy_references(self, path):
        content = read_file(path).upper()
        refs = re.findall(r"COPY\s+(\S+?)[\.\s]", content)
        return [r for r in refs if r not in ("OF", "IN", "REPLACING")]

    def test_all_copy_references_resolve(self):
        available = {
            os.path.splitext(n)[0].upper()
            for n in os.listdir(CPY_DIR)
            if n.lower().endswith(".cpy")
        }
        system_copies = {"DFHAID", "DFHBMSCA", "DFHATTR"}
        bms_copies = set()
        if os.path.isdir(BMS_DIR):
            for name in os.listdir(BMS_DIR):
                bms_copies.add(os.path.splitext(name)[0].upper())
        cbl_bases = {
            os.path.splitext(n)[0].upper()
            for n in os.listdir(CBL_DIR)
            if n.lower().endswith(".cbl")
        }
        all_known = available | system_copies | bms_copies | cbl_bases

        for path in list_files(CBL_DIR, ".cbl"):
            refs = self._get_copy_references(path)
            for ref in refs:
                ref_upper = ref.upper()
                if ref_upper not in all_known:
                    found_partial = any(
                        ref_upper in k or k in ref_upper for k in all_known
                    )
                    self.assertTrue(
                        found_partial,
                        f"{os.path.basename(path)}: COPY {ref} not found in copybooks",
                    )

    def test_cocom01y_referenced_by_cics_programs(self):
        non_cics = {"COBSWAIT.CBL", "CSUTLDTC.CBL", "COBDATFT.CBL"}
        for path in list_files(CBL_DIR, ".cbl"):
            basename = os.path.basename(path).upper()
            if basename.startswith("CO") and basename not in non_cics:
                content = read_file(path).upper()
                if "EXEC CICS" in content:
                    refs = self._get_copy_references(path)
                    refs_upper = [r.upper() for r in refs]
                    self.assertIn(
                        "COCOM01Y",
                        refs_upper,
                        f"CICS program {basename} should include COCOM01Y",
                    )


class TestCopybookDataStructures(unittest.TestCase):

    def test_cocom01y_has_commarea_structure(self):
        content = read_file(os.path.join(CPY_DIR, "COCOM01Y.cpy")).upper()
        self.assertIn("CARDDEMO-COMMAREA", content)
        self.assertIn("CDEMO-FROM-TRANID", content)
        self.assertIn("CDEMO-FROM-PROGRAM", content)
        self.assertIn("CDEMO-TO-PROGRAM", content)
        self.assertIn("CDEMO-USER-ID", content)
        self.assertIn("CDEMO-USER-TYPE", content)

    def test_cocom01y_user_types(self):
        content = read_file(os.path.join(CPY_DIR, "COCOM01Y.cpy")).upper()
        self.assertIn("CDEMO-USRTYP-ADMIN", content)
        self.assertIn("CDEMO-USRTYP-USER", content)

    def test_cvact01y_has_account_record(self):
        content = read_file(os.path.join(CPY_DIR, "CVACT01Y.cpy")).upper()
        self.assertIn("ACCOUNT-RECORD", content)
        self.assertIn("ACCT-ID", content)
        self.assertIn("ACCT-ACTIVE-STATUS", content)
        self.assertIn("ACCT-CURR-BAL", content)
        self.assertIn("ACCT-CREDIT-LIMIT", content)
        self.assertIn("ACCT-CASH-CREDIT-LIMIT", content)
        self.assertIn("ACCT-OPEN-DATE", content)
        self.assertIn("ACCT-EXPIRAION-DATE", content)
        self.assertIn("ACCT-REISSUE-DATE", content)

    def test_csusr01y_has_user_security_structure(self):
        content = read_file(os.path.join(CPY_DIR, "CSUSR01Y.cpy")).upper()
        self.assertIn("SEC-USER-DATA", content)
        self.assertIn("SEC-USR-ID", content)
        self.assertIn("SEC-USR-FNAME", content)
        self.assertIn("SEC-USR-LNAME", content)
        self.assertIn("SEC-USR-PWD", content)
        self.assertIn("SEC-USR-TYPE", content)

    def test_account_record_length_is_300(self):
        content = read_file(os.path.join(CPY_DIR, "CVACT01Y.cpy"))
        self.assertIn("300", content, "Account record should be 300 bytes (RECLN 300)")

    def test_user_security_record_field_sizes(self):
        content = read_file(os.path.join(CPY_DIR, "CSUSR01Y.cpy"))
        self.assertIn("X(08)", content)
        self.assertIn("X(20)", content)
        self.assertIn("X(01)", content)


class TestSignonProgram(unittest.TestCase):

    def setUp(self):
        self.content = read_file(os.path.join(CBL_DIR, "COSGN00C.cbl"))
        self.content_upper = self.content.upper()

    def test_program_id_is_cosgn00c(self):
        self.assertIn("PROGRAM-ID. COSGN00C", self.content_upper)

    def test_reads_usrsec_file(self):
        self.assertIn("USRSEC", self.content_upper)

    def test_handles_enter_key(self):
        self.assertIn("PROCESS-ENTER-KEY", self.content_upper)

    def test_handles_pf3_key(self):
        self.assertIn("DFHPF3", self.content_upper)

    def test_validates_empty_userid(self):
        self.assertIn("PLEASE ENTER USER ID", self.content_upper)

    def test_validates_empty_password(self):
        self.assertIn("PLEASE ENTER PASSWORD", self.content_upper)

    def test_handles_wrong_password(self):
        self.assertIn("WRONG PASSWORD", self.content_upper)

    def test_handles_user_not_found(self):
        self.assertIn("USER NOT FOUND", self.content_upper)

    def test_admin_user_routes_to_admin_menu(self):
        self.assertIn("COADM01C", self.content_upper)

    def test_regular_user_routes_to_main_menu(self):
        self.assertIn("COMEN01C", self.content_upper)

    def test_converts_userid_to_uppercase(self):
        self.assertIn("UPPER-CASE", self.content_upper)

    def test_handles_invalid_key(self):
        self.assertIn("INVALID", self.content_upper)

    def test_sends_signon_screen(self):
        self.assertIn("SEND-SIGNON-SCREEN", self.content_upper)

    def test_populates_header_info(self):
        self.assertIn("POPULATE-HEADER-INFO", self.content_upper)

    def test_uses_current_date(self):
        self.assertIn("CURRENT-DATE", self.content_upper)


class TestTransactionListProgram(unittest.TestCase):

    def setUp(self):
        self.content = read_file(os.path.join(CBL_DIR, "COTRN00C.cbl"))
        self.content_upper = self.content.upper()

    def test_program_id_is_cotrn00c(self):
        self.assertIn("PROGRAM-ID. COTRN00C", self.content_upper)

    def test_reads_transact_file(self):
        self.assertIn("TRANSACT", self.content_upper)

    def test_supports_page_forward(self):
        self.assertIn("PROCESS-PAGE-FORWARD", self.content_upper)

    def test_supports_page_backward(self):
        self.assertIn("PROCESS-PAGE-BACKWARD", self.content_upper)

    def test_handles_pf7_for_page_up(self):
        self.assertIn("DFHPF7", self.content_upper)

    def test_handles_pf8_for_page_down(self):
        self.assertIn("DFHPF8", self.content_upper)

    def test_validates_tran_id_is_numeric(self):
        self.assertIn("TRAN ID MUST BE NUMERIC", self.content_upper)

    def test_supports_10_transactions_per_page(self):
        self.assertIn("10", self.content)

    def test_handles_selection_flag_s(self):
        self.assertIn("'S'", self.content)

    def test_navigates_to_transaction_detail(self):
        self.assertIn("COTRN01C", self.content_upper)

    def test_returns_to_menu_on_pf3(self):
        self.assertIn("COMEN01C", self.content_upper)

    def test_handles_top_of_page(self):
        self.assertIn("ALREADY AT THE TOP", self.content_upper)

    def test_handles_bottom_of_page(self):
        self.assertIn("ALREADY AT THE BOTTOM", self.content_upper)

    def test_startbr_transact_file(self):
        self.assertIn("STARTBR-TRANSACT-FILE", self.content_upper)

    def test_endbr_transact_file(self):
        self.assertIn("ENDBR-TRANSACT-FILE", self.content_upper)


class TestDateValidationUtility(unittest.TestCase):

    def setUp(self):
        self.content = read_file(os.path.join(CBL_DIR, "CSUTLDTC.cbl"))
        self.content_upper = self.content.upper()

    def test_program_id_is_csutldtc(self):
        self.assertIn("PROGRAM-ID. CSUTLDTC", self.content_upper)

    def test_calls_ceedays_api(self):
        self.assertIn("CEEDAYS", self.content_upper)

    def test_has_linkage_section(self):
        self.assertIn("LINKAGE SECTION", self.content_upper)

    def test_accepts_date_parameter(self):
        self.assertIn("LS-DATE", self.content_upper)

    def test_accepts_date_format_parameter(self):
        self.assertIn("LS-DATE-FORMAT", self.content_upper)

    def test_returns_result(self):
        self.assertIn("LS-RESULT", self.content_upper)

    def test_handles_valid_date(self):
        self.assertIn("DATE IS VALID", self.content_upper)

    def test_handles_bad_date_value(self):
        self.assertIn("DATEVALUE ERROR", self.content_upper)

    def test_handles_invalid_era(self):
        self.assertIn("INVALID ERA", self.content_upper)

    def test_handles_invalid_month(self):
        self.assertIn("INVALID MONTH", self.content_upper)

    def test_handles_non_numeric_data(self):
        self.assertIn("NONNUMERIC DATA", self.content_upper)

    def test_handles_insufficient_data(self):
        self.assertIn("INSUFFICIENT", self.content_upper)

    def test_handles_bad_pic_string(self):
        self.assertIn("BAD PIC STRING", self.content_upper)

    def test_uses_feedback_code(self):
        self.assertIn("FEEDBACK-CODE", self.content_upper)


class TestBatchAccountProgram(unittest.TestCase):

    def setUp(self):
        self.content = read_file(os.path.join(CBL_DIR, "CBACT01C.cbl"))
        self.content_upper = self.content.upper()

    def test_program_id_is_cbact01c(self):
        self.assertIn("PROGRAM-ID", self.content_upper)
        self.assertIn("CBACT01C", self.content_upper)

    def test_reads_acctfile(self):
        self.assertIn("ACCTFILE", self.content_upper)

    def test_writes_output_file(self):
        self.assertIn("OUT-FILE", self.content_upper)

    def test_writes_array_file(self):
        self.assertIn("ARRY-FILE", self.content_upper)

    def test_writes_variable_record_file(self):
        self.assertIn("VBRC-FILE", self.content_upper)

    def test_handles_end_of_file(self):
        self.assertIn("END-OF-FILE", self.content_upper)

    def test_handles_io_errors(self):
        self.assertIn("9910-DISPLAY-IO-STATUS", self.content_upper)

    def test_handles_abend(self):
        self.assertIn("9999-ABEND-PROGRAM", self.content_upper)

    def test_opens_acctfile(self):
        self.assertIn("0000-ACCTFILE-OPEN", self.content_upper)

    def test_closes_acctfile(self):
        self.assertIn("9000-ACCTFILE-CLOSE", self.content_upper)

    def test_calls_date_formatter(self):
        self.assertIn("COBDATFT", self.content_upper)

    def test_uses_indexed_file_access(self):
        self.assertIn("INDEXED", self.content_upper)


class TestStatementPrintProgram(unittest.TestCase):

    def setUp(self):
        path = os.path.join(CBL_DIR, "CBSTM03A.CBL")
        self.content = read_file(path)
        self.content_upper = self.content.upper()

    def test_program_id_is_cbstm03a(self):
        self.assertIn("PROGRAM-ID", self.content_upper)
        self.assertIn("CBSTM03A", self.content_upper)

    def test_generates_plain_text_statements(self):
        self.assertIn("STMT-FILE", self.content_upper)

    def test_generates_html_statements(self):
        self.assertIn("HTML-FILE", self.content_upper)

    def test_has_html_output_formatting(self):
        self.assertIn("<!DOCTYPE HTML>", self.content_upper)

    def test_uses_2d_array(self):
        self.assertIn("OCCURS", self.content_upper)

    def test_uses_comp_variables(self):
        self.assertIn("COMP", self.content_upper)

    def test_uses_comp3_variables(self):
        self.assertIn("COMP-3", self.content_upper)

    def test_uses_alter_statement(self):
        self.assertIn("ALTER", self.content_upper)

    def test_reads_transaction_file(self):
        self.assertIn("TRNXFILE", self.content_upper)

    def test_reads_xref_file(self):
        self.assertIn("XREFFILE", self.content_upper)

    def test_reads_customer_file(self):
        self.assertIn("CUSTFILE", self.content_upper)

    def test_reads_account_file(self):
        self.assertIn("ACCTFILE", self.content_upper)

    def test_creates_statement(self):
        self.assertIn("CREATE-STATEMENT", self.content_upper)

    def test_statement_has_start_marker(self):
        self.assertIn("START OF STATEMENT", self.content_upper)

    def test_statement_has_end_marker(self):
        self.assertIn("END OF STATEMENT", self.content_upper)

    def test_accesses_mainframe_control_blocks(self):
        self.assertIn("PSA-BLOCK", self.content_upper)
        self.assertIn("TCB-BLOCK", self.content_upper)
        self.assertIn("TIOT-BLOCK", self.content_upper)


if __name__ == "__main__":
    unittest.main()
