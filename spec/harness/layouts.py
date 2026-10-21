"""Record layouts read straight from the copybooks in app/cpy."""
import re
from dataclasses import dataclass
from functools import cache

from . import APP_DIR

PIC_RE = re.compile(
    r"^\s*(\d{2})\s+([A-Z0-9-]+)\s+PIC\s+(S?)([X9])\((\d+)\)(?:V9\((\d+)\)|V(9+))?",
    re.IGNORECASE,
)


@dataclass(frozen=True)
class Field:
    name: str
    offset: int
    length: int
    kind: str  # "X" alphanumeric, "9" zoned decimal
    signed: bool = False
    scale: int = 0

    @property
    def numeric(self):
        return self.kind == "9"


@dataclass(frozen=True)
class Layout:
    name: str
    fields: tuple

    @property
    def length(self):
        return sum(f.length for f in self.fields)

    def field(self, name):
        for f in self.fields:
            if f.name == name:
                return f
        raise KeyError(name)

    @property
    def data_fields(self):
        return tuple(f for f in self.fields if not f.name.startswith("filler"))


def _column_text(line):
    line = line.expandtabs(8).rstrip("\r\n")
    if len(line) > 6 and line[6] in "*/":
        return ""
    return line[7:72]


def parse_copybook_text(name, text):
    fields, offset, fillers = [], 0, 0
    for raw in text.splitlines():
        line = _column_text(raw)
        m = PIC_RE.match(line)
        if not m:
            continue
        _, cname, sign, kind, digits, vdigits, vnines = m.groups()
        int_len = int(digits)
        scale = int(vdigits) if vdigits else (len(vnines) if vnines else 0)
        length = int_len + scale
        fname = cname.lower().replace("-", "_")
        if fname == "filler":
            fillers += 1
            fname = f"filler_{fillers}"
        fields.append(Field(fname, offset, length, kind.upper(), bool(sign), scale))
        offset += length
    return Layout(name, tuple(fields))


@cache
def copybook(member):
    path = APP_DIR / "cpy" / f"{member}.cpy"
    return parse_copybook_text(member, path.read_text())


@cache
def reject_layout():
    """DALYREJS: the daily record followed by the 80-byte validation trailer (CBTRN02C)."""
    daily = copybook("CVTRA06Y")
    trailer = [
        Field("validation_fail_reason", daily.length, 4, "9"),
        Field("validation_fail_reason_desc", daily.length + 4, 76, "X"),
    ]
    return Layout("DALYREJS", daily.fields + tuple(trailer))
