package com.justin.libradesk.util;

import com.justin.libradesk.domain.enumtype.ClassificationScheme;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Produces a comparable shelf-order key from a call number so copies can be
 * sorted into shelving order.
 *
 * <p>This is a pragmatic approximation, not a full LC/Dewey shelflisting
 * algorithm: it pads the class number so numeric parts compare correctly and
 * upper-cases the rest. Good enough to order a shelf list; precise Cutter
 * collation is out of scope.
 */
public final class CallNumber {

    private static final Pattern DDC = Pattern.compile("^(\\d+)(\\.\\d+)?(.*)$");
    private static final Pattern LCC = Pattern.compile("^([A-Z]+)\\s*(\\d+)?(\\.\\d+)?(.*)$");

    private CallNumber() {
    }

    public static String shelfKey(ClassificationScheme scheme, String callNumber) {
        if (callNumber == null || callNumber.isBlank()) {
            return "";
        }
        String normalized = callNumber.trim().toUpperCase().replaceAll("\\s+", " ");
        return scheme == ClassificationScheme.LCC ? lccKey(normalized) : ddcKey(normalized);
    }

    private static String ddcKey(String value) {
        Matcher m = DDC.matcher(value);
        if (!m.matches()) {
            return value;
        }
        String integer = padLeft(m.group(1), 3);
        String fraction = m.group(2) == null ? "" : m.group(2);
        return integer + fraction + " " + m.group(3).trim();
    }

    private static String lccKey(String value) {
        Matcher m = LCC.matcher(value);
        if (!m.matches()) {
            return value;
        }
        String letters = padRight(m.group(1), 3);
        String number = m.group(2) == null ? "     " : padLeft(m.group(2), 5);
        String fraction = m.group(3) == null ? "" : m.group(3);
        return letters + " " + number + fraction + " " + m.group(4).trim();
    }

    private static String padLeft(String value, int width) {
        return value.length() >= width ? value : "0".repeat(width - value.length()) + value;
    }

    private static String padRight(String value, int width) {
        return value.length() >= width ? value : value + " ".repeat(width - value.length());
    }
}
