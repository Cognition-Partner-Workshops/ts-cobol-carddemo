"""
Customer data loader - migrated from CUSTFILE.jcl.

This module replaces the JCL job CUSTFILE which:
1. Closes CUSTDAT file in CICS region
2. Deletes existing CUSTDATA VSAM KSDS cluster
3. Defines new CUSTDATA VSAM KSDS cluster (KEYS(9 0), RECORDSIZE(500 500))
4. Copies data from AWS.M2.CARDDEMO.CUSTDATA.PS to VSAM file using REPRO
5. Opens CUSTDAT file in CICS region
"""

from carddemo_batch.loaders.base_loader import BaseLoader
from carddemo_batch.models.customer import Customer


class CustomerLoader(BaseLoader[Customer]):
    """
    Loader for Customer data.
    
    Migrated from CUSTFILE.jcl which loads customer master data
    from a flat file into the CUSTDATA VSAM KSDS file.
    
    Record layout from CVCUS01Y.cpy:
    - Position 0-8: CUST-ID (9 digits)
    - Position 9-33: CUST-FIRST-NAME (25 chars)
    - Position 34-58: CUST-MIDDLE-NAME (25 chars)
    - Position 59-83: CUST-LAST-NAME (25 chars)
    - Position 84-133: CUST-ADDR-LINE-1 (50 chars)
    - Position 134-183: CUST-ADDR-LINE-2 (50 chars)
    - Position 184-233: CUST-ADDR-LINE-3 (50 chars)
    - Position 234-235: CUST-ADDR-STATE-CD (2 chars)
    - Position 236-238: CUST-ADDR-COUNTRY-CD (3 chars)
    - Position 239-248: CUST-ADDR-ZIP (10 chars)
    - Position 249-263: CUST-PHONE-NUM-1 (15 chars)
    - Position 264-278: CUST-PHONE-NUM-2 (15 chars)
    - Position 279-287: CUST-SSN (9 digits)
    - Position 288-307: CUST-GOVT-ISSUED-ID (20 chars)
    - Position 308-317: CUST-DOB-YYYY-MM-DD (10 chars)
    - Position 318-327: CUST-EFT-ACCOUNT-ID (10 chars)
    - Position 328: CUST-PRI-CARD-HOLDER-IND (1 char)
    - Position 329-331: CUST-FICO-CREDIT-SCORE (3 digits)
    """

    @property
    def job_name(self) -> str:
        return "CUSTFILE"

    @property
    def record_length(self) -> int:
        return 500

    @property
    def model_class(self) -> type[Customer]:
        return Customer

    def parse_record(self, line: str) -> Customer | None:
        """Parse a single customer record from the flat file."""
        line = line.ljust(self.record_length)
        
        cust_id_str = line[0:9].strip()
        if not cust_id_str:
            return None
        
        try:
            cust_id = int(cust_id_str)
        except ValueError:
            self.logger.warning(f"Invalid customer ID: {cust_id_str}")
            return None

        ssn_str = line[279:288].strip()
        ssn = int(ssn_str) if ssn_str and ssn_str.isdigit() else None

        fico_str = line[329:332].strip()
        fico = int(fico_str) if fico_str and fico_str.isdigit() else None

        return Customer(
            cust_id=cust_id,
            cust_first_name=line[9:34].strip() or None,
            cust_middle_name=line[34:59].strip() or None,
            cust_last_name=line[59:84].strip() or None,
            cust_addr_line_1=line[84:134].strip() or None,
            cust_addr_line_2=line[134:184].strip() or None,
            cust_addr_line_3=line[184:234].strip() or None,
            cust_addr_state_cd=line[234:236].strip() or None,
            cust_addr_country_cd=line[236:239].strip() or None,
            cust_addr_zip=line[239:249].strip() or None,
            cust_phone_num_1=line[249:264].strip() or None,
            cust_phone_num_2=line[264:279].strip() or None,
            cust_ssn=ssn,
            cust_govt_issued_id=line[288:308].strip() or None,
            cust_dob_yyyy_mm_dd=line[308:318].strip() or None,
            cust_eft_account_id=line[318:328].strip() or None,
            cust_pri_card_holder_ind=line[328:329].strip() or None,
            cust_fico_credit_score=fico,
        )
