package com.carddemo.interest.codec;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/** Test helper: record bytes written by hand, as fixture text or as hexadecimal. */
final class Bytes {

    private Bytes() {
    }

    static byte[] fixture(String text) {
        return text.getBytes(StandardCharsets.ISO_8859_1);
    }

    static byte[] hex(String hexDigits) {
        return HexFormat.of().parseHex(hexDigits.replace(" ", ""));
    }

    static String hex(byte[] bytes) {
        return HexFormat.of().withUpperCase().formatHex(bytes);
    }
}
