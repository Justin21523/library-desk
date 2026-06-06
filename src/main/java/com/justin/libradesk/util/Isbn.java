package com.justin.libradesk.util;

/**
 * ISBN-10 / ISBN-13 validation and normalisation.
 */
public final class Isbn {

    private Isbn() {
    }

    /** Strips spaces/hyphens and upper-cases a trailing check 'x'. Returns null for null. */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        return raw.replaceAll("[\\s-]", "").toUpperCase();
    }

    public static boolean isValid(String raw) {
        String isbn = normalize(raw);
        if (isbn == null) {
            return false;
        }
        return switch (isbn.length()) {
            case 10 -> isValid10(isbn);
            case 13 -> isValid13(isbn);
            default -> false;
        };
    }

    /** Converts a valid ISBN-10 to ISBN-13; returns the input unchanged if it is already 13 digits. */
    public static String to13(String raw) {
        String isbn = normalize(raw);
        if (isbn != null && isbn.length() == 13) {
            return isbn;
        }
        if (!isValid10(isbn)) {
            throw new IllegalArgumentException("Not a valid ISBN-10: " + raw);
        }
        String body = "978" + isbn.substring(0, 9);
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = body.charAt(i) - '0';
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        int check = (10 - (sum % 10)) % 10;
        return body + check;
    }

    private static boolean isValid10(String isbn) {
        if (isbn == null || isbn.length() != 10) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            char ch = isbn.charAt(i);
            int value;
            if (ch == 'X' && i == 9) {
                value = 10;
            } else if (Character.isDigit(ch)) {
                value = ch - '0';
            } else {
                return false;
            }
            sum += value * (10 - i);
        }
        return sum % 11 == 0;
    }

    private static boolean isValid13(String isbn) {
        if (isbn == null || isbn.length() != 13) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 13; i++) {
            char ch = isbn.charAt(i);
            if (!Character.isDigit(ch)) {
                return false;
            }
            int digit = ch - '0';
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        return sum % 10 == 0;
    }
}
