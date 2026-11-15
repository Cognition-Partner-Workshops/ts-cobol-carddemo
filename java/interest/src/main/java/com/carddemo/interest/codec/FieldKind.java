package com.carddemo.interest.codec;

/** The encoding rule of a copybook field (modernization/design/data-map-interest.md, E1-E5). */
public enum FieldKind {
    /** E1: PIC 9(n), unsigned zoned decimal. */
    UNSIGNED_ZONED,
    /** E2: PIC S9(n)V9(m), signed zoned decimal, sign in the last byte. */
    SIGNED_ZONED,
    /** E3: PIC X(n), text in the code page. */
    TEXT,
    /** E4: FILLER, raw bytes, never translated. */
    FILLER,
    /** E5: a field this codec refuses: USAGE other than DISPLAY, or a clause it does not implement. */
    REFUSED
}
