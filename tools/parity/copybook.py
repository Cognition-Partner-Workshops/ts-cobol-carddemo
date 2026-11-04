"""Copybook-driven fixed-record codec for the CardDemo estate.

Parses the subset of COBOL data description used by app/cpy (levels, PIC,
COMP/COMP-3, OCCURS, REDEFINES ignored for layout) and decodes or encodes
fixed-length records.  Supports DISPLAY zoned decimal with EBCDIC-style
overpunch signs (what ``cobc -fsign=EBCDIC`` writes), COMP-3 packed decimal,
binary COMP, and both ASCII and EBCDIC (cp037) text encodings.
"""
from __future__ import annotations

import re
from dataclasses import dataclass
from decimal import Decimal
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[2]
COPY_DIRS = [ROOT / "app" / "cpy", ROOT / "tools" / "parity" / "layouts",
             ROOT / "app" / "app-transaction-type-db2" / "cpy"]

# Overpunch tables: digit -> zone character for positive / negative values.
POSITIVE = "{ABCDEFGHI"
NEGATIVE = "}JKLMNOPQR"
OVERPUNCH = {c: (i, 1) for i, c in enumerate(POSITIVE)}
OVERPUNCH.update({c: (i, -1) for i, c in enumerate(NEGATIVE)})


@dataclass(frozen=True)
class Field:
    name: str
    offset: int
    length: int
    kind: str          # text | display | comp-3 | comp
    scale: int
    signed: bool
    digits: int

    @property
    def is_filler(self) -> bool:
        return self.name.upper().startswith("FILLER")


def find_copybook(name: str) -> Path:
    wanted = name.upper()
    for directory in COPY_DIRS:
        if not directory.exists():
            continue
        for path in directory.iterdir():
            if path.stem.upper() == wanted and path.suffix.lower() in (".cpy", ".cbl", ".dcl"):
                return path
    raise FileNotFoundError(f"copybook not found: {name}")


def _pic_count(piece: str) -> int:
    return sum(int(m.group(2) or 1) for m in re.finditer(r"([XA9SVZ,.+-])(?:\((\d+)\))?", piece.upper()))


def _pic_info(pic: str) -> tuple[str, int, int, bool]:
    pic = pic.upper()
    if "X" in pic or "A" in pic:
        return "text", 0, _pic_count(pic), False
    signed = pic.startswith("S")
    body = pic[1:] if signed else pic
    integer, _, fraction = body.partition("V")
    scale = _pic_count(fraction) if fraction else 0
    return "display", scale, _pic_count(integer) + scale, signed


_ITEM = re.compile(
    r"^\s*(\d{2})\s+([A-Z0-9-]+)"
    r"(?:\s+REDEFINES\s+[A-Z0-9-]+)?"
    r"(?:\s+PIC(?:TURE)?\s+(?:IS\s+)?([SXA9V()0-9]+))?"
    r"(?:\s+(?:USAGE\s+(?:IS\s+)?)?(COMP-3|COMP3|COMP(?:UTATIONAL)?(?:-[45])?|BINARY|PACKED-DECIMAL|DISPLAY))?"
    r"(?:\s+OCCURS\s+(\d+)(?:\s+TIMES)?)?",
    re.IGNORECASE,
)


def _statements(path: Path) -> list[str]:
    """Join the copybook's source lines into period-terminated statements."""
    text = []
    for raw in path.read_text(encoding="utf-8", errors="replace").splitlines():
        line = raw.expandtabs(8)
        if len(line) > 6 and line[6] in "*/":
            continue
        line = line[6:72] if len(line) > 72 else line[6:] if len(line) > 6 else ""
        if line.strip().startswith("*"):
            continue
        text.append(line.strip())
    return [s.strip() for s in " ".join(text).split(".") if s.strip()]


def parse_copybook(name: str) -> list[Field]:
    """Flatten the copybook into elementary fields with byte offsets."""
    fields: list[Field] = []
    offset = 0
    # (level, remaining repeat count, start offset, fields index) for OCCURS groups
    pending_occurs: list[tuple[int, int, int, int]] = []
    for statement in _statements(find_copybook(name)):
        match = _ITEM.match(statement)
        if not match or "REDEFINES" in statement.upper():
            continue
        level, item, pic, usage, occurs = match.groups()
        level = int(level)
        if level in (66, 77, 88):
            continue
        while pending_occurs and level <= pending_occurs[-1][0]:
            lvl, count, start, index = pending_occurs.pop()
            span = offset - start
            block = fields[index:]
            for rep in range(1, count):
                for f in block:
                    fields.append(Field(f"{f.name}[{rep + 1}]", f.offset + span * rep, f.length,
                                        f.kind, f.scale, f.signed, f.digits))
            fields[index:index + len(block)] = [
                Field(f"{f.name}[1]", f.offset, f.length, f.kind, f.scale, f.signed, f.digits)
                for f in block]
            offset = start + span * count
        if pic is None:
            if occurs:
                pending_occurs.append((level, int(occurs), offset, len(fields)))
            continue
        kind, scale, digits, signed = _pic_info(pic)
        usage = (usage or "").upper()
        if kind == "text":
            length = digits
        elif usage in ("COMP-3", "COMP3", "PACKED-DECIMAL"):
            kind, length = "comp-3", digits // 2 + 1
        elif usage in ("COMP", "COMPUTATIONAL", "BINARY", "COMP-4", "COMPUTATIONAL-4", "COMP-5", "COMPUTATIONAL-5"):
            kind, length = "comp", 2 if digits <= 4 else 4 if digits <= 9 else 8
        else:
            length = digits
        count = int(occurs or 1)
        for index in range(count):
            fname = f"FILLER@{offset}" if item.upper() == "FILLER" else (item if count == 1 else f"{item}[{index + 1}]")
            fields.append(Field(fname, offset, length, kind, scale, signed, digits))
            offset += length
    while pending_occurs:
        lvl, count, start, index = pending_occurs.pop()
        span = offset - start
        block = fields[index:]
        for rep in range(1, count):
            for f in block:
                fields.append(Field(f"{f.name}[{rep + 1}]", f.offset + span * rep, f.length,
                                    f.kind, f.scale, f.signed, f.digits))
        offset = start + span * count
    return fields


def record_length(fields: list[Field]) -> int:
    return max((f.offset + f.length for f in fields), default=0)


def _decode_display(raw: bytes, scale: int, signed: bool, encoding: str) -> Decimal:
    text = raw.decode(encoding)
    sign = 1
    if text.strip() == "":
        return Decimal(0)
    last = text[-1]
    if last in OVERPUNCH:
        digit, sign = OVERPUNCH[last]
        text = text[:-1] + str(digit)
    elif encoding != "ascii" and not last.isdigit():
        # EBCDIC zoned sign nibble arrived as a non-digit after codec translation
        raw_last = raw[-1]
        sign = -1 if (raw_last >> 4) == 0xD else 1
        text = text[:-1] + str(raw_last & 0x0F)
    digits = text.replace(" ", "0")
    if not digits.isdigit():
        # not a valid zoned number (e.g. text in a numeric field): keep the raw text
        return raw.decode(encoding)
    return Decimal(sign * int(digits)).scaleb(-scale) if scale else Decimal(sign * int(digits))


def _decode_packed(raw: bytes, scale: int) -> Decimal:
    nibbles = []
    for byte in raw:
        nibbles.extend((byte >> 4, byte & 0x0F))
    sign_nibble = nibbles.pop()
    sign = -1 if sign_nibble in (0xB, 0xD) else 1
    value = int("".join(str(n) for n in nibbles) or "0")
    return Decimal(sign * value).scaleb(-scale) if scale else Decimal(sign * value)


def _decode_comp(raw: bytes, scale: int, signed: bool) -> Decimal:
    value = int.from_bytes(raw, "big", signed=signed)
    return Decimal(value).scaleb(-scale) if scale else Decimal(value)


def decode_record(copybook: str, data: bytes, encoding: str = "ascii") -> dict[str, Any]:
    codec = "cp037" if encoding.lower() == "ebcdic" else "latin-1"
    values: dict[str, Any] = {}
    for f in parse_copybook(copybook):
        if f.is_filler:
            continue
        raw = data[f.offset:f.offset + f.length]
        if f.kind == "text":
            values[f.name] = raw.decode(codec).rstrip()
        elif f.kind == "comp-3":
            values[f.name] = _decode_packed(raw, f.scale)
        elif f.kind == "comp":
            values[f.name] = _decode_comp(raw, f.scale, f.signed)
        else:
            values[f.name] = _decode_display(raw, f.scale, f.signed, codec if codec != "latin-1" else "ascii")
    return values


def _decimal(value: Any) -> Decimal:
    return value if isinstance(value, Decimal) else Decimal(str(value))


def _encode_display(value: Any, length: int, scale: int, signed: bool) -> bytes:
    dec = _decimal(value)
    units = int((abs(dec) * (Decimal(10) ** scale)).to_integral_value())
    text = f"{units:0{length}d}"[-length:]
    if signed:
        table = NEGATIVE if dec < 0 else POSITIVE
        text = text[:-1] + table[int(text[-1])]
    return text.encode("ascii")


def _encode_packed(value: Any, length: int, scale: int, signed: bool) -> bytes:
    dec = _decimal(value)
    units = int((abs(dec) * (Decimal(10) ** scale)).to_integral_value())
    digits = f"{units:0{length * 2 - 1}d}"[-(length * 2 - 1):]
    nibbles = [int(c) for c in digits] + [0xD if signed and dec < 0 else (0xC if signed else 0xF)]
    return bytes((nibbles[i] << 4) | nibbles[i + 1] for i in range(0, len(nibbles), 2))


def encode_record(copybook: str, values: dict[str, Any]) -> bytes:
    fields = parse_copybook(copybook)
    out = bytearray(b" " * record_length(fields))
    for f in fields:
        if f.is_filler:
            if f.kind != "text":
                out[f.offset:f.offset + f.length] = _encode_display(0, f.length, 0, False)
            continue
        value = values.get(f.name, "" if f.kind == "text" else 0)
        if f.kind == "text":
            enc = str(value).ljust(f.length)[:f.length].encode("ascii")
        elif f.kind == "comp-3":
            enc = _encode_packed(value, f.length, f.scale, f.signed)
        elif f.kind == "comp":
            units = int((_decimal(value) * (Decimal(10) ** f.scale)).to_integral_value())
            enc = units.to_bytes(f.length, "big", signed=f.signed)
        else:
            enc = _encode_display(value, f.length, f.scale, f.signed)
        out[f.offset:f.offset + f.length] = enc
    return bytes(out)


def split_records(data: bytes, lrecl: int) -> list[bytes]:
    return [data[i:i + lrecl] for i in range(0, len(data), lrecl)]
