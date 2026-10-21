"""The shared loader: fixed-width records with overpunch signs to typed values and back."""
from decimal import Decimal

POSITIVE = "{ABCDEFGHI"
NEGATIVE = "}JKLMNOPQR"


class DecodeError(ValueError):
    pass


def decode_zoned(text, signed, scale):
    """Decode a zoned decimal display field. The sign lives in the last byte."""
    if text.strip() == "":
        text = "0" * len(text)
    last = text[-1]
    negative = False
    if last in POSITIVE:
        digits = text[:-1] + str(POSITIVE.index(last))
    elif last in NEGATIVE:
        digits = text[:-1] + str(NEGATIVE.index(last))
        negative = True
    else:
        digits = text
    digits = digits.replace(" ", "0")
    if not digits.isdigit():
        raise DecodeError(f"not a zoned decimal: {text!r}")
    if not signed and (last in POSITIVE or last in NEGATIVE):
        raise DecodeError(f"sign byte in unsigned field: {text!r}")
    value = Decimal(int(digits)).scaleb(-scale)
    if negative and value != 0:
        value = -value
    return value.quantize(Decimal(1).scaleb(-scale))


def encode_zoned(value, length, signed, scale):
    value = Decimal(value).quantize(Decimal(1).scaleb(-scale))
    digits = str(abs(int(value.scaleb(scale)))).rjust(length, "0")
    if len(digits) > length:
        raise ValueError(f"{value} does not fit in {length} digits")
    if not signed:
        if value < 0:
            raise ValueError("negative value in unsigned field")
        return digits
    table = NEGATIVE if value < 0 else POSITIVE
    return digits[:-1] + table[int(digits[-1])]


def canonical(value, field):
    """The text form every run writes, so records compare byte for byte."""
    if field.numeric:
        value = Decimal(value).quantize(Decimal(1).scaleb(-field.scale))
        if value == 0:
            value = abs(value)
        return f"{value:f}"
    return str(value).rstrip()


def decode_record(text, layout):
    text = text.rstrip("\r\n").ljust(layout.length)[: layout.length]
    rec = {}
    for f in layout.data_fields:
        raw = text[f.offset : f.offset + f.length]
        rec[f.name] = canonical(decode_zoned(raw, f.signed, f.scale), f) if f.numeric else raw.rstrip()
    return rec


def encode_record(rec, layout):
    out = []
    for f in layout.fields:
        if f.name.startswith("filler"):
            out.append(" " * f.length)
        elif f.numeric:
            out.append(encode_zoned(rec.get(f.name, 0), f.length, f.signed, f.scale))
        else:
            out.append(str(rec.get(f.name, "")).ljust(f.length)[: f.length])
    return "".join(out)
