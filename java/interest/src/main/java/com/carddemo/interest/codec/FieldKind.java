package com.carddemo.interest.codec;

/** How a field is encoded: the encoding rules E1-E5 of the data map. */
public enum FieldKind {
    /** E1: PIC 9(n), unsigned zoned decimal. */
    UNSIGNED_ZONED,
    /** E2: PIC S9(n)V9(m), signed zoned decimal, sign in the last byte. */
    SIGNED_ZONED,
    /** E3: PIC X(n), text in the code page. */
    TEXT,
    /** E4: FILLER, raw bytes. */
    FILLER,
    /** E5: USAGE other than DISPLAY (COMP, COMP-3 ...): refused, never guessed. */
    NOT_DISPLAY
}
