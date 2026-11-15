package com.carddemo.interest.codec;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.util.Optional;

/**
 * The bytes of one code page: digits, zoned sign bytes, text and the space (data-map rules E1-E3,
 * standards S3.7). Two kinds exist: the fixture code page (ISO-8859-1, where the sign bytes appear
 * as the characters { A-I and } J-R) and a host EBCDIC code page, where digits are X'F0'-X'F9' and
 * the sign bytes X'C0'-X'C9' and X'D0'-X'D9' whatever characters the page gives them.
 * The host page is a parameter; cp037 is the unconfirmed default (gap G01, decision REL-08).
 */
public final class CodePage {

    /** REL-08, option A (proposed, unconfirmed). */
    public static final String DEFAULT_HOST_CHARSET = "IBM037";

    private static final String FIXTURE_POSITIVE = "{ABCDEFGHI";
    private static final String FIXTURE_NEGATIVE = "}JKLMNOPQR";

    private final String name;
    private final Charset charset;
    private final byte[] digits = new byte[10];
    private final byte[] positive = new byte[10];
    private final byte[] negative = new byte[10];
    private final byte space;

    /** A digit and the sign carried with it in the last byte of a signed zoned field (E2). */
    public record SignedDigit(int digit, boolean negative) {
    }

    private CodePage(String name, Charset charset, boolean ebcdicZones) {
        if (charset.newEncoder().maxBytesPerChar() != 1.0f) {
            throw new IllegalArgumentException("code page " + charset + " is not a single-byte code page");
        }
        this.name = name;
        this.charset = charset;
        for (int d = 0; d < 10; d++) {
            digits[d] = (byte) (ebcdicZones ? 0xF0 | d : '0' + d);
            positive[d] = (byte) (ebcdicZones ? 0xC0 | d : FIXTURE_POSITIVE.charAt(d));
            negative[d] = (byte) (ebcdicZones ? 0xD0 | d : FIXTURE_NEGATIVE.charAt(d));
        }
        this.space = bytes(" ").orElseThrow()[0];
    }

    /** The code page of the recorded fixture files. */
    public static CodePage fixture() {
        return new CodePage("fixture", FixtureCodePage.CHARSET, false);
    }

    /** The host code page with the unconfirmed default cp037. */
    public static CodePage host() {
        return host(Charset.forName(DEFAULT_HOST_CHARSET));
    }

    /** A host EBCDIC code page stated by the data owner (REL-08, option B). */
    public static CodePage host(Charset ebcdicCharset) {
        return new CodePage("host " + ebcdicCharset.name(), ebcdicCharset, true);
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

    /** E1: the digit of a byte, or -1 when the byte is not a digit of this code page. */
    public int digitOf(byte b) {
        return indexOf(digits, b);
    }

    public byte digit(int digit) {
        return digits[digit];
    }

    /** E2: C zone is positive, D zone negative, a plain (F zone) digit is accepted as positive (G02). */
    public Optional<SignedDigit> signedDigitOf(byte b) {
        int digit = indexOf(positive, b);
        if (digit >= 0) {
            return Optional.of(new SignedDigit(digit, false));
        }
        digit = indexOf(negative, b);
        if (digit >= 0) {
            return Optional.of(new SignedDigit(digit, true));
        }
        digit = indexOf(digits, b);
        return digit >= 0 ? Optional.of(new SignedDigit(digit, false)) : Optional.empty();
    }

    /** E2: always C for positive and D for negative. */
    public byte signedDigit(int digit, boolean isNegative) {
        return isNegative ? negative[digit] : positive[digit];
    }

    /** E3: the characters of the bytes; empty when a byte is not a character of the code page (G07). */
    public Optional<String> text(byte[] record, int offset, int length) {
        try {
            String text = charset.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(record, offset, length)).toString();
            return text.chars().anyMatch(Character::isISOControl) ? Optional.empty() : Optional.of(text);
        } catch (CharacterCodingException e) {
            return Optional.empty();
        }
    }

    /** E3: the bytes of the characters; empty when a character does not exist in the code page. */
    public Optional<byte[]> bytes(String text) {
        if (text.chars().anyMatch(Character::isISOControl)) {
            return Optional.empty();
        }
        try {
            ByteBuffer encoded = charset.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(text));
            byte[] bytes = new byte[encoded.remaining()];
            encoded.get(bytes);
            return Optional.of(bytes);
        } catch (CharacterCodingException e) {
            return Optional.empty();
        }
    }

    private static int indexOf(byte[] table, byte b) {
        for (int i = 0; i < table.length; i++) {
            if (table[i] == b) {
                return i;
            }
        }
        return -1;
    }
}
