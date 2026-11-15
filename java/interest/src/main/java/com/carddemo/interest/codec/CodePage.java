package com.carddemo.interest.codec;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

/**
 * The one place the code page is named (standards S3.7, data-map rules E1-E3, gap G01).
 * <ul>
 * <li>{@link #fixture()}: the recorded files, ISO-8859-1; digits are the characters 0-9 and a signed
 * field ends in <code>{</code>, A-I (plus) or <code>}</code>, J-R (minus) (tools/parity/record.py:51).</li>
 * <li>{@link #host()}: EBCDIC; digits are X'F0'-X'F9' and the sign byte is X'C0'-X'C9' (plus) or
 * X'D0'-X'D9' (minus) whatever the text code page is. The text code page is a parameter with cp037
 * as the unconfirmed default (decision REL-08, gap G01).</li>
 * </ul>
 */
public final class CodePage {

    /** REL-08 is open: cp037 until the mainframe data owner states the CCSID. */
    public static final String UNCONFIRMED_HOST_CHARSET = "IBM037";

    private static final CodePage FIXTURE = new CodePage("fixture", StandardCharsets.ISO_8859_1, '0', '{', 'A', '}', 'J');
    private static final int EBCDIC_ZERO = 0xF0;
    private static final int EBCDIC_PLUS = 0xC0;
    private static final int EBCDIC_MINUS = 0xD0;

    private final String name;
    private final Charset charset;
    private final int zero;
    private final int plusZero;
    private final int plusOne;
    private final int minusZero;
    private final int minusOne;
    private final byte space;

    private CodePage(String name, Charset charset, int zero, int plusZero, int plusOne, int minusZero, int minusOne) {
        if (!charset.canEncode() || charset.newEncoder().maxBytesPerChar() != 1.0f) {
            throw new IllegalArgumentException(charset + " is not a single-byte code page");
        }
        this.name = name;
        this.charset = charset;
        this.zero = zero;
        this.plusZero = plusZero;
        this.plusOne = plusOne;
        this.minusZero = minusZero;
        this.minusOne = minusOne;
        this.space = " ".getBytes(charset)[0];
    }

    public static CodePage fixture() {
        return FIXTURE;
    }

    public static CodePage host() {
        return host(Charset.forName(UNCONFIRMED_HOST_CHARSET));
    }

    public static CodePage host(Charset ebcdicCharset) {
        if ((" ".getBytes(ebcdicCharset)[0] & 0xFF) != 0x40) {
            throw new IllegalArgumentException(ebcdicCharset + " is not an EBCDIC code page");
        }
        return new CodePage("host", ebcdicCharset, EBCDIC_ZERO, EBCDIC_PLUS, EBCDIC_PLUS + 1, EBCDIC_MINUS, EBCDIC_MINUS + 1);
    }

    public String name() {
        return name;
    }

    public Charset charset() {
        return charset;
    }

    public byte space() {
        return space;
    }

    /** One digit and its sign; {@code signed} is false for a plain digit. */
    record SignedDigit(int digit, boolean negative, boolean signed) {
    }

    /** E1: the digit a byte stands for, or -1. */
    int digit(byte b) {
        int digit = (b & 0xFF) - zero;
        return digit >= 0 && digit <= 9 ? digit : -1;
    }

    byte digitByte(int digit) {
        return (byte) (zero + digit);
    }

    /** E2: the last byte of a signed field; a plain digit is accepted as positive (G02). */
    Optional<SignedDigit> signedDigit(byte b) {
        int value = b & 0xFF;
        if (value == plusZero) {
            return Optional.of(new SignedDigit(0, false, true));
        }
        if (value >= plusOne && value < plusOne + 9) {
            return Optional.of(new SignedDigit(value - plusOne + 1, false, true));
        }
        if (value == minusZero) {
            return Optional.of(new SignedDigit(0, true, true));
        }
        if (value >= minusOne && value < minusOne + 9) {
            return Optional.of(new SignedDigit(value - minusOne + 1, true, true));
        }
        int digit = digit(b);
        return digit < 0 ? Optional.empty() : Optional.of(new SignedDigit(digit, false, false));
    }

    /** E2: always C for zero or positive and D for negative. */
    byte signedDigitByte(int digit, boolean negative) {
        if (digit == 0) {
            return (byte) (negative ? minusZero : plusZero);
        }
        return (byte) ((negative ? minusOne : plusOne) + digit - 1);
    }

    /** E3: the characters of the bytes, or empty when a byte is not a character of the code page (G07). */
    Optional<String> text(byte[] bytes, int offset, int length) {
        try {
            String text = charset.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes, offset, length)).toString();
            return isPrintable(text) && text.length() == length ? Optional.of(text) : Optional.empty();
        } catch (CharacterCodingException e) {
            return Optional.empty();
        }
    }

    /** E3: the bytes of the characters, or empty when a character is not in the code page. */
    Optional<byte[]> bytes(String text) {
        if (!isPrintable(text)) {
            return Optional.empty();
        }
        try {
            ByteBuffer encoded = charset.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(text));
            return Optional.of(Arrays.copyOfRange(encoded.array(), encoded.position(), encoded.limit()));
        } catch (CharacterCodingException e) {
            return Optional.empty();
        }
    }

    /** A whole record as text for the run log echo (CBACT04C:193); the bytes are shown as the code page reads them. */
    public String display(byte[] record) {
        return new String(record, charset);
    }

    private static boolean isPrintable(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isISOControl(c) || c == '\uFFFD') {
                return false;
            }
        }
        return true;
    }
}
