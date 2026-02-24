package com.carddemo.transform.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for formatting and parsing COBOL-style field values into Java types.
 * Handles PIC S9(n)V99 signed decimals, PIC 9(n) display numerics,
 * PIC X(n) alphanumeric strings, EBCDIC date formats, and COBOL timestamps.
 */
public final class CobolDataFormatter {

    private CobolDataFormatter() {
        // utility class
    }

    /** Pattern for COBOL timestamps: yyyy-MM-dd-HH.mm.ss.SSSSSS */
    private static final Pattern COBOL_TS_PATTERN =
            Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})-(\\d{2})\\.(\\d{2})\\.(\\d{2})\\.(\\d{6})");

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * Parses a COBOL signed decimal string (e.g. "+0000012345.67" or "-0000054321.99")
     * into a {@link BigDecimal}.
     *
     * @param value the COBOL numeric display value with sign and implied decimal
     * @return parsed BigDecimal
     * @throws NumberFormatException if the value cannot be parsed
     */
    public static BigDecimal parseSignedDecimal(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return new BigDecimal(trimmed);
    }

    /**
     * Parses a COBOL unsigned numeric display (PIC 9(n)) into a long.
     * Leading zeros are stripped.
     *
     * @param value the numeric display string (e.g. "00000000042")
     * @return parsed long value
     * @throws NumberFormatException if the value contains non-numeric characters
     */
    public static long parseNumericDisplay(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0L;
        }
        return Long.parseLong(value.trim());
    }

    /**
     * Parses a COBOL unsigned numeric display into an integer.
     *
     * @param value the numeric display string (e.g. "0042")
     * @return parsed int value
     */
    public static int parseNumericDisplayInt(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        return Integer.parseInt(value.trim());
    }

    /**
     * Trims trailing spaces from a COBOL PIC X(n) alphanumeric field.
     * Returns null if the field is entirely spaces.
     *
     * @param value the raw COBOL alphanumeric value
     * @return trimmed string, or null if blank
     */
    public static String trimAlphanumeric(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.stripTrailing();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Parses a COBOL date string in yyyy-MM-dd format into an ISO LocalDate.
     *
     * @param value the date string (PIC X(10))
     * @return parsed LocalDate, or null if blank or invalid
     */
    public static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), ISO_DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Converts a COBOL timestamp (yyyy-MM-dd-HH.mm.ss.SSSSSS) to ISO-8601.
     *
     * @param value the COBOL timestamp string (PIC X(26))
     * @return ISO-8601 string (yyyy-MM-ddTHH:mm:ss.SSSSSS), or null if blank/invalid
     */
    public static String convertTimestampToIso(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        Matcher m = COBOL_TS_PATTERN.matcher(value.trim());
        if (!m.matches()) {
            return null;
        }
        return String.format("%s-%s-%sT%s:%s:%s.%s",
                m.group(1), m.group(2), m.group(3),
                m.group(4), m.group(5), m.group(6), m.group(7));
    }

    /**
     * Parses a COBOL timestamp to a LocalDateTime.
     *
     * @param value the COBOL timestamp string
     * @return parsed LocalDateTime, or null
     */
    public static LocalDateTime parseTimestamp(String value) {
        String iso = convertTimestampToIso(value);
        if (iso == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(iso);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Validates whether a string is a valid COBOL date in yyyy-MM-dd format.
     *
     * @param value the date string
     * @return true if valid
     */
    public static boolean isValidDate(String value) {
        return parseDate(value) != null;
    }

    /**
     * Validates whether a string is a valid COBOL timestamp.
     *
     * @param value the timestamp string
     * @return true if valid
     */
    public static boolean isValidTimestamp(String value) {
        return convertTimestampToIso(value) != null;
    }

    /**
     * Checks if a value represents a COBOL "blank" (all spaces or low-values).
     *
     * @param value the value to test
     * @return true if blank
     */
    public static boolean isCobolBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * Converts the primary card holder indicator (Y/N) to a boolean.
     *
     * @param indicator the COBOL indicator value
     * @return true for "Y", false otherwise
     */
    public static boolean parseCardHolderIndicator(String indicator) {
        return "Y".equalsIgnoreCase(indicator != null ? indicator.trim() : "");
    }
}
