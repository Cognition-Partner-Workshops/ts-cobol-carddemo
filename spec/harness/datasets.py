"""The files the batch path reads and writes, keyed the way the programs key them."""
from dataclasses import dataclass

from .layouts import copybook, reject_layout


@dataclass(frozen=True)
class Dataset:
    name: str
    member: str
    seed: str = ""
    key: tuple = ()
    alt_key: str = ""

    @property
    def layout(self):
        return reject_layout() if self.member == "DALYREJS" else copybook(self.member)


INPUTS = {
    "accounts": Dataset("accounts", "CVACT01Y", "acctdata.txt", ("acct_id",)),
    "cardxref": Dataset("cardxref", "CVACT03Y", "cardxref.txt", ("xref_card_num",), "xref_acct_id"),
    "tcatbal": Dataset("tcatbal", "CVTRA01Y", "tcatbal.txt", ("trancat_acct_id", "trancat_type_cd", "trancat_cd")),
    "discgrp": Dataset("discgrp", "CVTRA02Y", "discgrp.txt", ("dis_acct_group_id", "dis_tran_type_cd", "dis_tran_cat_cd")),
    "dailytran": Dataset("dailytran", "CVTRA06Y", "dailytran.txt", ("dalytran_id",)),
}

OUTPUTS = {
    "posted": Dataset("posted", "CVTRA05Y", key=("tran_id",)),
    "rejects": Dataset("rejects", "DALYREJS", key=("dalytran_id",)),
    "accounts": INPUTS["accounts"],
    "tcatbal": INPUTS["tcatbal"],
    "interest": Dataset("interest", "CVTRA05Y", key=("tran_id",)),
}


def record_key(dataset, rec):
    return "|".join(rec[k] for k in dataset.key)
