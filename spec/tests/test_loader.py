from decimal import Decimal

import pytest

from harness.datasets import INPUTS, OUTPUTS
from harness.records import DecodeError, decode_zoned, encode_zoned


@pytest.mark.parametrize("text,value", [
    ("0000005047G", Decimal("504.77")),
    ("0000000000{", Decimal("0.00")),
    ("0000001234}", Decimal("-123.40")),
    ("0000000061J", Decimal("-6.11")),
    ("00000020200{", Decimal("2020.00")),
])
def test_overpunch_decodes_sign_from_last_byte(text, value):
    assert decode_zoned(text, signed=True, scale=2) == value


@pytest.mark.parametrize("value", ["504.77", "-6.11", "0.00", "-0.01", "999999999.99"])
def test_overpunch_round_trips(value):
    assert decode_zoned(encode_zoned(Decimal(value), 11, True, 2), True, 2) == Decimal(value)


def test_unsigned_field_rejects_sign_byte():
    with pytest.raises(DecodeError):
        decode_zoned("0000A", signed=False, scale=0)


@pytest.mark.parametrize("name,length", [("accounts", 300), ("cardxref", 50), ("tcatbal", 50), ("discgrp", 50), ("dailytran", 350)])
def test_input_layouts_match_the_fd_lengths(name, length):
    assert INPUTS[name].layout.length == length


def test_reject_record_is_daily_record_plus_trailer():
    assert OUTPUTS["rejects"].layout.length == 430
