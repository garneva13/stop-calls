package com.callguard;

import java.util.Set;

/**
 * Phone number normalisation and matching.
 *
 * Deliberately free of any Android dependency so that it can be compiled and
 * exercised on a plain JVM (see tools/PhoneNumbersSelfTest.java).
 *
 * The canonical form is a plain digit string that always includes the country
 * code when it can be derived, e.g. "+7 (916) 123-45-67" and "89161234567"
 * both become "79161234567".
 */
public final class PhoneNumbers {

    /** Country code assumed for numbers written in national format. */
    public static final String DEFAULT_COUNTRY_CODE = "7";

    private static final int MIN_DIGITS = 3;

    private PhoneNumbers() {
    }

    /** Returns the canonical digit string for {@code raw}, or {@code null}. */
    public static String canonical(String raw) {
        return canonical(raw, DEFAULT_COUNTRY_CODE);
    }

    /** Returns the canonical digit string for {@code raw}, or {@code null}. */
    public static String canonical(String raw, String countryCode) {
        String cleaned = clean(raw);
        if (cleaned == null) {
            return null;
        }
        boolean international = cleaned.charAt(0) == '+';
        String digits = digitsOnly(cleaned);
        if (digits.length() < MIN_DIGITS) {
            return null;
        }
        if (!international && digits.startsWith("00") && digits.length() >= 11) {
            digits = digits.substring(2);
            international = true;
        }
        if (international) {
            return fromInternational(digits);
        }
        String cc = (countryCode == null || countryCode.isEmpty())
                ? DEFAULT_COUNTRY_CODE : countryCode;
        return fromNational(digits, cc);
    }

    /**
     * Compares a canonical incoming number against a set of canonical contact
     * numbers. Numbers of equal length and with the same last ten digits are
     * treated as the same subscriber; this absorbs different country-code
     * spellings and broken formatting.
     */
    public static boolean matches(String canonical, Set<String> exact, Set<String> tails) {
        if (canonical == null || exact == null) {
            return false;
        }
        if (exact.contains(canonical)) {
            return true;
        }
        if (tails != null && canonical.length() >= 11) {
            return tails.contains(tail(canonical));
        }
        return false;
    }

    /** Last ten digits of a canonical number. */
    public static String tail(String canonical) {
        if (canonical == null || canonical.length() <= 10) {
            return canonical;
        }
        return canonical.substring(canonical.length() - 10);
    }

    /** Hundreds-readable representation used in the UI. */
    public static String format(String canonical) {
        if (canonical == null || canonical.isEmpty()) {
            return "";
        }
        if (canonical.length() == 11 && canonical.charAt(0) == '7') {
            return "+7 " + canonical.substring(1, 4) + " " + canonical.substring(4, 7)
                    + "-" + canonical.substring(7, 9) + "-" + canonical.substring(9, 11);
        }
        if (canonical.length() == 11 && canonical.charAt(0) == '1') {
            return "+1 (" + canonical.substring(1, 4) + ") " + canonical.substring(4, 7)
                    + "-" + canonical.substring(7, 11);
        }
        return "+" + canonical;
    }

    private static String fromInternational(String digits) {
        if (digits.length() == 11 && (digits.charAt(0) == '7' || digits.charAt(0) == '8')) {
            return "7" + digits.substring(1);
        }
        return digits;
    }

    private static String fromNational(String digits, String countryCode) {
        if (DEFAULT_COUNTRY_CODE.equals(countryCode)) {
            if (digits.length() == 11
                    && (digits.charAt(0) == '8' || digits.charAt(0) == '7')) {
                return "7" + digits.substring(1);
            }
            if (digits.length() == 10) {
                return "7" + digits;
            }
        }
        return digits;
    }

    /**
     * Trims the input, removes "tel:" prefixes and dial-string suffixes such as
     * ";1234" (extension) or "," (pause).
     */
    private static String clean(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (s.regionMatches(true, 0, "tel:", 0, 4)) {
            s = s.substring(4).trim();
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ';' || c == ',') {
                s = s.substring(0, i).trim();
                break;
            }
        }
        return s.isEmpty() ? null : s;
    }

    /** Keeps only ASCII digits. */
    public static String digitsOnly(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
