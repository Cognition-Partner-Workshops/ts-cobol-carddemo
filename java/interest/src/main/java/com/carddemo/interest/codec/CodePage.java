package com.carddemo.interest.codec;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * The byte representation of text, digits and zoned signs of one record format: the one place a
 * code page is named (standards S3.7; data-map rules E1-E3, gap G01).
 *
 * <p>Two formats exist. The recorded fixture format (FI-2) is ISO-8859-1 with the digits 0-9 and
 * the overpunch characters { A-I (plus) and } J-R (minus). The host format is an EBCDIC code page
 * with digits X'F0'-X'F9' and sign zones X'C0'-X'C9' (plus), X'D0'-X'D9' (minus). In the host
 * format the zoned bytes are read and written by zone and digit nibble, not through the character
 * table, because the braces sit elsewhere in some EBCDIC code pages.
 */
public final class CodePage {

    /** REL-08 is OPEN: cp037 is the unconfirmed default of the host code page (G01). */
    public static final String DEFAULT_HOST_CHARSET = "IBM037";

    /** Fixture files are ISO-8859-1, one record per line (tools/parity/record.py:51). */
    public static final CodePage FIXTURE = new CodePage("fixture (ISO-8859-1)", StandardCharsets.ISO_8859_1, false);

    private static final String POSITIVE_OVERPUNCH = "{ABCDEFGHI";
    private static final String NEGATIVE_OVERPUNCH = "}JKLMNOPQR";
    private static final int EBCDIC_DIGIT_ZONE = 0xF0;
    private static final int EBCDIC_PLUS_ZONE = 0xC0;
    private static final int EBCDIC_MINUS_ZONE = 0xD0;

    private final String name;
    private final Charset charset;
    private final boolean ebcdic;

    private CodePage(String name, Charset charset, boolean ebcdic) {
        this.name = name;
        this.charset = charset;
        this.ebcdic = ebcdic;
    }

    /** The host format in the default code page. */
    public static CodePage host() {
        return host(DEFAULT_HOST_CHARSET);
    }

    /** The host format in the EBCDIC code page of the given Java charset name, for example IBM037 or IBM1047. */
    public static CodePage host(String charsetName) {
        Charset charset = Charset.forName(charsetName);
        if (!"0".equals(new String(new byte[] {(byte) EBCDIC_DIGIT_ZONE}, charset))) {
            throw new IllegalArgumentException(charsetName + " is not an EBCDIC code page: X'F0' is not the digit 0");
        }
        return new CodePage("host (" + charset.name() + ")", charset, true);
    }

    public String name() {
        return name;
    }

    public Charset charset() {
        return charset;
    }

    /** The space of this code page, the padding of text (E3) and of a new FILLER (E4, G05). */
    public byte space() {
        return ebcdic ? (byte) 0x40 : (byte) 0x20;
    }

    /** E1: the digit a byte stands for, or -1. */
    int digit(byte value) {
        int unsignedByte = value & 0xFF;
        int digit = ebcdic ? unsignedByte - EBCDIC_DIGIT_ZONE : unsignedByte - '0';
        return digit >= 0 && digit <= 9 ? digit : -1;
    }

    byte digitByte(int digit) {
        return (byte) (ebcdic ? EBCDIC_DIGIT_ZONE + digit : '0' + digit);
    }

    /**
     * E2: the last byte of a signed zoned field. Returns the digit, plus 10 when the sign is minus,
     * or -1 when the byte is neither C-, D- nor F-signed (an F-signed digit is plus, G02).
     */
    int signedDigit(byte value) {
        int unsignedByte = value & 0xFF;
        if (digit(value) >= 0) {
            return digit(value);
        }
        if (ebcdic) {
            int digit = unsignedByte & 0x0F;
            int zone = unsignedByte & 0xF0;
            if (digit > 9 || (zone != EBCDIC_PLUS_ZONE && zone != EBCDIC_MINUS_ZONE)) {
                return -1;
            }
            return zone == EBCDIC_MINUS_ZONE ? digit + 10 : digit;
        }
        int positive = POSITIVE_OVERPUNCH.indexOf(unsignedByte);
        int negative = NEGATIVE_OVERPUNCH.indexOf(unsignedByte);
        return positive >= 0 ? positive : negative >= 0 ? negative + 10 : -1;
    }

    /** E2: always C for zero or plus and D for minus. */
    byte signedDigitByte(int digit, boolean minus) {
        if (ebcdic) {
            return (byte) ((minus ? EBCDIC_MINUS_ZONE : EBCDIC_PLUS_ZONE) + digit);
        }
        return (byte) (minus ? NEGATIVE_OVERPUNCH : POSITIVE_OVERPUNCH).charAt(digit);
    }

    /** E3: the text of the bytes, or null when a byte is not a character of the code page (G07). */
    String text(byte[] record, int offset, int length) {
        String text;
        try {
            text = charset.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(record, offset, length)).toString();
        } catch (CharacterCodingException e) {
            return null;
        }
        for (int i = 0; i < text.length(); i++) {
            if (Character.isISOControl(text.charAt(i))) {
                return null;
            }
        }
        return text.length() == length ? text : null;
    }

    /** E3: the bytes of the text, or null when a character is not in the code page. */
    byte[] bytes(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.isISOControl(text.charAt(i))) {
                return null;
            }
        }
        try {
            ByteBuffer encoded = charset.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(text));
            byte[] bytes = new byte[encoded.remaining()];
            encoded.get(bytes);
            return bytes.length == text.length() ? bytes : null;
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
