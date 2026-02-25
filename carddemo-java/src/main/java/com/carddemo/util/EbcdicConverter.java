package com.carddemo.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;

/**
 * EBCDIC data conversion utility for migrating mainframe data files.
 * Handles COBOL binary data formats:
 * - COMP (binary/fullword, 2 or 4 bytes)
 * - COMP-3 (packed decimal)
 * - Zoned decimal (EBCDIC zone digits)
 * - Signed/unsigned fields
 * - EBCDIC to ASCII character conversion
 *
 * Used to load sample data from app/data/EBCDIC/ into the relational database.
 */
public final class EbcdicConverter {

    private static final Logger log = LoggerFactory.getLogger(EbcdicConverter.class);

    /**
     * IBM EBCDIC code page 037 (US/Canada).
     * Falls back to Cp500 if 037 is not available.
     */
    private static final Charset EBCDIC_CHARSET;

    static {
        Charset charset;
        try {
            charset = Charset.forName("IBM037");
        } catch (Exception e) {
            try {
                charset = Charset.forName("Cp500");
            } catch (Exception e2) {
                log.warn("EBCDIC charset not available, using ISO-8859-1 as fallback");
                charset = Charset.forName("ISO-8859-1");
            }
        }
        EBCDIC_CHARSET = charset;
    }

    private EbcdicConverter() {}

    /**
     * Convert EBCDIC bytes to ASCII string.
     */
    public static String ebcdicToAscii(byte[] ebcdicBytes) {
        return new String(ebcdicBytes, EBCDIC_CHARSET).trim();
    }

    /**
     * Convert EBCDIC bytes at a specific offset and length to ASCII string.
     */
    public static String ebcdicToAscii(byte[] data, int offset, int length) {
        byte[] field = new byte[length];
        System.arraycopy(data, offset, field, 0, length);
        return ebcdicToAscii(field);
    }

    /**
     * Convert COMP-3 (packed decimal) bytes to BigDecimal.
     *
     * Packed decimal format:
     * - Each byte contains two decimal digits (high nibble, low nibble)
     * - Last nibble is the sign (0xC = positive, 0xD = negative, 0xF = unsigned)
     * - Example: value 12345 with 2 decimal places = bytes [0x01, 0x23, 0x45, 0x0C]
     */
    public static BigDecimal comp3ToDecimal(byte[] data, int offset, int length, int decimalPlaces) {
        byte[] field = new byte[length];
        System.arraycopy(data, offset, field, 0, length);
        return comp3ToDecimal(field, decimalPlaces);
    }

    /**
     * Convert COMP-3 (packed decimal) bytes to BigDecimal.
     */
    public static BigDecimal comp3ToDecimal(byte[] packedBytes, int decimalPlaces) {
        StringBuilder digits = new StringBuilder();

        for (int i = 0; i < packedBytes.length; i++) {
            int b = packedBytes[i] & 0xFF;
            int highNibble = (b >> 4) & 0x0F;
            int lowNibble = b & 0x0F;

            if (i < packedBytes.length - 1) {
                // All bytes except last: both nibbles are digits
                digits.append(highNibble);
                digits.append(lowNibble);
            } else {
                // Last byte: high nibble is digit, low nibble is sign
                digits.append(highNibble);
            }
        }

        // Determine sign from last nibble of last byte
        int lastByte = packedBytes[packedBytes.length - 1] & 0x0F;
        boolean negative = (lastByte == 0x0D); // 0xD = negative

        String numStr = digits.toString();
        // Remove leading zeros but keep at least one digit
        numStr = numStr.replaceFirst("^0+(?=.)", "");

        BigDecimal result = new BigDecimal(numStr);
        if (decimalPlaces > 0) {
            result = result.movePointLeft(decimalPlaces);
        }
        if (negative) {
            result = result.negate();
        }

        return result;
    }

    /**
     * Convert COMP (binary) field to long.
     * COMP fields are big-endian binary integers.
     * PIC S9(4) COMP = 2 bytes (halfword)
     * PIC S9(9) COMP = 4 bytes (fullword)
     * PIC S9(18) COMP = 8 bytes (doubleword)
     */
    public static long compToLong(byte[] data, int offset, int length) {
        long result = 0;
        boolean signed = true; // COBOL COMP is typically signed

        for (int i = 0; i < length; i++) {
            result = (result << 8) | (data[offset + i] & 0xFF);
        }

        // Handle sign for 2-byte and 4-byte fields
        if (signed) {
            if (length == 2 && result > 0x7FFF) {
                result = result - 0x10000;
            } else if (length == 4 && result > 0x7FFFFFFFL) {
                result = result - 0x100000000L;
            }
        }

        return result;
    }

    /**
     * Convert zoned decimal (EBCDIC display numeric) to BigDecimal.
     *
     * Zoned decimal format:
     * - Each byte represents one digit
     * - Zone (high nibble) is 0xF for digits 0-9
     * - Sign is embedded in the zone of the last byte:
     *   0xC = positive, 0xD = negative, 0xF = unsigned positive
     */
    public static BigDecimal zonedDecimalToDecimal(byte[] data, int offset, int length, int decimalPlaces) {
        StringBuilder digits = new StringBuilder();
        boolean negative = false;

        for (int i = 0; i < length; i++) {
            int b = data[offset + i] & 0xFF;
            int zone = (b >> 4) & 0x0F;
            int digit = b & 0x0F;

            digits.append(digit);

            // Check sign on last byte
            if (i == length - 1) {
                if (zone == 0x0D) {
                    negative = true;
                }
            }
        }

        String numStr = digits.toString().replaceFirst("^0+(?=.)", "");
        BigDecimal result = new BigDecimal(numStr);
        if (decimalPlaces > 0) {
            result = result.movePointLeft(decimalPlaces);
        }
        if (negative) {
            result = result.negate();
        }

        return result;
    }

    /**
     * Read a fixed-length record from a byte array at the given offset.
     */
    public static byte[] readRecord(byte[] data, int offset, int recordLength) {
        if (offset + recordLength > data.length) {
            throw new IllegalArgumentException(
                    String.format("Record exceeds data boundary: offset=%d, length=%d, dataSize=%d",
                            offset, recordLength, data.length));
        }
        byte[] record = new byte[recordLength];
        System.arraycopy(data, offset, record, 0, recordLength);
        return record;
    }

    /**
     * Count records in a data file based on fixed record length.
     */
    public static int countRecords(byte[] data, int recordLength) {
        return data.length / recordLength;
    }
}
