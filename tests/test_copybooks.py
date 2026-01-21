#!/usr/bin/env python3
"""
Automated tests for CardDemo COBOL copybook data structures.

These tests validate the COBOL copybook definitions used in the CardDemo
mainframe application. They verify record lengths, field definitions,
and data structure consistency which is critical for mainframe modernization.
"""

import os
import re
import unittest
from pathlib import Path


class CopybookParser:
    """Simple parser for COBOL copybook files to extract field definitions."""
    
    PIC_PATTERN = re.compile(r'PIC\s+([X9S()V]+)', re.IGNORECASE)
    FIELD_PATTERN = re.compile(r'^\s*\d+\s+(\S+)\s+PIC\s+([X9S()V.]+)', re.IGNORECASE)
    RECORD_LENGTH_PATTERN = re.compile(r'RECLN\s*[=]?\s*(\d+)', re.IGNORECASE)
    
    @staticmethod
    def calculate_pic_length(pic_clause):
        """Calculate the byte length of a PIC clause."""
        pic = pic_clause.upper().replace(' ', '')
        
        if 'V' in pic:
            parts = pic.split('V')
            integer_part = parts[0]
            decimal_part = parts[1] if len(parts) > 1 else ''
            
            int_len = CopybookParser._count_digits(integer_part)
            dec_len = CopybookParser._count_digits(decimal_part)
            
            if pic.startswith('S'):
                return int_len + dec_len
            return int_len + dec_len
        
        return CopybookParser._count_digits(pic)
    
    @staticmethod
    def _count_digits(pic_part):
        """Count the number of digits/characters in a PIC part."""
        pic_part = pic_part.replace('S', '')
        
        match = re.search(r'[X9]\((\d+)\)', pic_part)
        if match:
            return int(match.group(1))
        
        return len(re.findall(r'[X9]', pic_part))
    
    @staticmethod
    def parse_copybook(filepath):
        """Parse a copybook file and return field definitions."""
        fields = []
        record_length = None
        
        with open(filepath, 'r') as f:
            content = f.read()
        
        length_match = CopybookParser.RECORD_LENGTH_PATTERN.search(content)
        if length_match:
            record_length = int(length_match.group(1))
        
        for line in content.split('\n'):
            if line.strip().startswith('*'):
                continue
            
            match = CopybookParser.FIELD_PATTERN.search(line)
            if match:
                field_name = match.group(1)
                pic_clause = match.group(2)
                length = CopybookParser.calculate_pic_length(pic_clause)
                fields.append({
                    'name': field_name,
                    'pic': pic_clause,
                    'length': length
                })
        
        return {
            'fields': fields,
            'record_length': record_length
        }


class TestCopybookStructures(unittest.TestCase):
    """Test cases for COBOL copybook data structures."""
    
    @classmethod
    def setUpClass(cls):
        """Set up test fixtures."""
        cls.cpy_dir = Path(__file__).parent.parent / 'app' / 'cpy'
    
    def test_account_record_structure(self):
        """Test CVACT01Y.cpy - Account record structure."""
        filepath = self.cpy_dir / 'CVACT01Y.cpy'
        self.assertTrue(filepath.exists(), f"Copybook not found: {filepath}")
        
        result = CopybookParser.parse_copybook(filepath)
        
        self.assertEqual(result['record_length'], 300, "Account record should be 300 bytes")
        
        field_names = [f['name'] for f in result['fields']]
        expected_fields = ['ACCT-ID', 'ACCT-ACTIVE-STATUS', 'ACCT-CURR-BAL', 
                          'ACCT-CREDIT-LIMIT', 'ACCT-OPEN-DATE']
        for field in expected_fields:
            self.assertIn(field, field_names, f"Missing field: {field}")
    
    def test_customer_record_structure(self):
        """Test CVCUS01Y.cpy - Customer record structure."""
        filepath = self.cpy_dir / 'CVCUS01Y.cpy'
        self.assertTrue(filepath.exists(), f"Copybook not found: {filepath}")
        
        result = CopybookParser.parse_copybook(filepath)
        
        self.assertEqual(result['record_length'], 500, "Customer record should be 500 bytes")
        
        field_names = [f['name'] for f in result['fields']]
        expected_fields = ['CUST-ID', 'CUST-FIRST-NAME', 'CUST-LAST-NAME', 
                          'CUST-SSN', 'CUST-FICO-CREDIT-SCORE']
        for field in expected_fields:
            self.assertIn(field, field_names, f"Missing field: {field}")
    
    def test_transaction_record_structure(self):
        """Test CVTRA05Y.cpy - Transaction record structure."""
        filepath = self.cpy_dir / 'CVTRA05Y.cpy'
        self.assertTrue(filepath.exists(), f"Copybook not found: {filepath}")
        
        result = CopybookParser.parse_copybook(filepath)
        
        self.assertEqual(result['record_length'], 350, "Transaction record should be 350 bytes")
        
        field_names = [f['name'] for f in result['fields']]
        expected_fields = ['TRAN-ID', 'TRAN-TYPE-CD', 'TRAN-AMT', 
                          'TRAN-CARD-NUM', 'TRAN-MERCHANT-NAME']
        for field in expected_fields:
            self.assertIn(field, field_names, f"Missing field: {field}")
    
    def test_all_copybooks_exist(self):
        """Test that all expected copybook files exist."""
        expected_copybooks = [
            'CVACT01Y.cpy',
            'CVACT02Y.cpy',
            'CVACT03Y.cpy',
            'CVCUS01Y.cpy',
            'CVTRA05Y.cpy',
            'CVTRA06Y.cpy',
            'CVEXPORT.cpy'
        ]
        
        for copybook in expected_copybooks:
            filepath = self.cpy_dir / copybook
            self.assertTrue(filepath.exists(), f"Missing copybook: {copybook}")
    
    def test_copybook_has_valid_pic_clauses(self):
        """Test that copybooks contain valid PIC clauses."""
        copybooks = list(self.cpy_dir.glob('*.cpy')) + list(self.cpy_dir.glob('*.CPY'))
        
        self.assertGreater(len(copybooks), 0, "No copybooks found")
        
        for copybook in copybooks:
            with open(copybook, 'r') as f:
                content = f.read()
            
            if 'PIC' in content.upper():
                pic_matches = re.findall(r'PIC\s+[X9S()V.]+', content, re.IGNORECASE)
                self.assertGreater(len(pic_matches), 0, 
                    f"Copybook {copybook.name} has PIC keyword but no valid PIC clauses")


class TestCOBOLPrograms(unittest.TestCase):
    """Test cases for COBOL program files."""
    
    @classmethod
    def setUpClass(cls):
        """Set up test fixtures."""
        cls.cbl_dir = Path(__file__).parent.parent / 'app' / 'cbl'
    
    def test_all_cobol_programs_exist(self):
        """Test that all expected COBOL programs exist."""
        expected_programs = [
            'COSGN00C.cbl',
            'COMEN01C.cbl',
            'COACTVWC.cbl',
            'COACTUPC.cbl',
            'COTRN00C.cbl',
            'COTRN01C.cbl',
            'COTRN02C.cbl',
            'CBTRN02C.cbl',
            'CBEXPORT.cbl',
            'CBIMPORT.cbl'
        ]
        
        for program in expected_programs:
            filepath = self.cbl_dir / program
            self.assertTrue(filepath.exists(), f"Missing COBOL program: {program}")
    
    def test_cobol_programs_have_procedure_division(self):
        """Test that COBOL programs contain PROCEDURE DIVISION."""
        programs = list(self.cbl_dir.glob('*.cbl')) + list(self.cbl_dir.glob('*.CBL'))
        
        self.assertGreater(len(programs), 0, "No COBOL programs found")
        
        for program in programs:
            with open(program, 'r') as f:
                content = f.read().upper()
            
            self.assertIn('PROCEDURE DIVISION', content, 
                f"Program {program.name} missing PROCEDURE DIVISION")
    
    def test_cobol_programs_have_identification_division(self):
        """Test that COBOL programs contain IDENTIFICATION DIVISION."""
        programs = list(self.cbl_dir.glob('*.cbl')) + list(self.cbl_dir.glob('*.CBL'))
        
        for program in programs:
            with open(program, 'r') as f:
                content = f.read().upper()
            
            self.assertIn('IDENTIFICATION DIVISION', content, 
                f"Program {program.name} missing IDENTIFICATION DIVISION")
    
    def test_online_programs_have_cics_commands(self):
        """Test that online CICS programs contain EXEC CICS commands."""
        online_programs = [
            'COSGN00C.cbl',
            'COMEN01C.cbl',
            'COACTVWC.cbl',
            'COTRN00C.cbl'
        ]
        
        for program_name in online_programs:
            filepath = self.cbl_dir / program_name
            if filepath.exists():
                with open(filepath, 'r') as f:
                    content = f.read().upper()
                
                self.assertIn('EXEC CICS', content, 
                    f"Online program {program_name} should contain EXEC CICS commands")


class TestBMSMaps(unittest.TestCase):
    """Test cases for BMS map files."""
    
    @classmethod
    def setUpClass(cls):
        """Set up test fixtures."""
        cls.bms_dir = Path(__file__).parent.parent / 'app' / 'bms'
    
    def test_bms_directory_exists(self):
        """Test that BMS directory exists."""
        self.assertTrue(self.bms_dir.exists(), "BMS directory not found")
    
    def test_bms_maps_exist(self):
        """Test that BMS map files exist."""
        bms_files = list(self.bms_dir.glob('*.bms')) + list(self.bms_dir.glob('*.BMS'))
        self.assertGreater(len(bms_files), 0, "No BMS map files found")
    
    def test_bms_maps_have_dfhmsd(self):
        """Test that BMS maps contain DFHMSD macro."""
        bms_files = list(self.bms_dir.glob('*.bms')) + list(self.bms_dir.glob('*.BMS'))
        
        for bms_file in bms_files:
            with open(bms_file, 'r') as f:
                content = f.read().upper()
            
            self.assertIn('DFHMSD', content, 
                f"BMS map {bms_file.name} should contain DFHMSD macro")


class TestJCLJobs(unittest.TestCase):
    """Test cases for JCL job files."""
    
    @classmethod
    def setUpClass(cls):
        """Set up test fixtures."""
        cls.jcl_dir = Path(__file__).parent.parent / 'app' / 'jcl'
    
    def test_jcl_directory_exists(self):
        """Test that JCL directory exists."""
        self.assertTrue(self.jcl_dir.exists(), "JCL directory not found")
    
    def test_jcl_jobs_exist(self):
        """Test that JCL job files exist."""
        jcl_files = list(self.jcl_dir.glob('*.jcl')) + list(self.jcl_dir.glob('*.JCL'))
        self.assertGreater(len(jcl_files), 0, "No JCL job files found")
    
    def test_jcl_jobs_have_job_card(self):
        """Test that JCL jobs contain JOB card."""
        jcl_files = list(self.jcl_dir.glob('*.jcl')) + list(self.jcl_dir.glob('*.JCL'))
        
        for jcl_file in jcl_files:
            with open(jcl_file, 'r') as f:
                content = f.read().upper()
            
            has_job = 'JOB' in content
            self.assertTrue(has_job, f"JCL file {jcl_file.name} should contain JOB card")


class TestDataFiles(unittest.TestCase):
    """Test cases for data files."""
    
    @classmethod
    def setUpClass(cls):
        """Set up test fixtures."""
        cls.data_dir = Path(__file__).parent.parent / 'app' / 'data'
    
    def test_data_directory_exists(self):
        """Test that data directory exists."""
        self.assertTrue(self.data_dir.exists(), "Data directory not found")
    
    def test_sample_data_files_exist(self):
        """Test that sample data files exist."""
        data_files = list(self.data_dir.rglob('*'))
        data_files = [f for f in data_files if f.is_file()]
        self.assertGreater(len(data_files), 0, "No data files found")


if __name__ == '__main__':
    unittest.main(verbosity=2)
