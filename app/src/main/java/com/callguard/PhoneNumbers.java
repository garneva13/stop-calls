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
     * True when two canonical numbers belong to the same subscriber: either they
     * are identical, or they share the last ten digits and differ only in a
     * prefix that is a known spelling of the same country code.
     *
     * The last ten digits alone are not enough. "3809161234567" is a different
     * person in a different country whose number merely ends with the same ten
     * digits as a contact, and so is "19161234567" — and a caller chooses the
     * number they present. Requiring an equal length rules out the first, and
     * requiring an equivalent prefix rules out the second; what is left through
     * is "79161234567" versus "89161234567", which really is one subscriber
     * written with and without the Russian trunk prefix.
     *
     * Numbers shorter than eleven digits — short service codes such as "112" or
     * "900" — only ever match themselves: there is no country code to absorb, and
     * two different short codes are two different destinations.
     */
    public static boolean sameSubscriber(String first, String second) {
        if (first == null || second == null) {
            return false;
        }
        if (first.equals(second)) {
            return true;
        }
        if (first.length() != second.length() || first.length() < 11) {
            return false;
        }
        if (!tail(first).equals(tail(second))) {
            return false;
        }
        return equivalentPrefix(
                first.substring(0, first.length() - 10),
                second.substring(0, second.length() - 10));
    }

    /** True when two country-code prefixes are spellings of the same country. */
    private static boolean equivalentPrefix(String first, String second) {
        if (first.equals(second)) {
            return true;
        }
        // A Russian number reaches the matcher as "7…" when it could be parsed as
        // E.164 and as "8…" when the country of the call was unknown and the trunk
        // prefix survived. Those two are the same subscriber; "1…" is not.
        return isRussianPrefix(first) && isRussianPrefix(second);
    }

    private static boolean isRussianPrefix(String prefix) {
        return DEFAULT_COUNTRY_CODE.equals(prefix) || "8".equals(prefix);
    }

    /**
     * Compares a canonical incoming number against a set of canonical contact
     * numbers, using {@link #sameSubscriber} as the rule.
     *
     * {@code tails} is only a fast negative filter: a tail nobody in the address
     * book has cannot belong to a contact. A tail that is known still has to be
     * confirmed against the full numbers, because a tail on its own says nothing
     * about the length.
     *
     * This means the two arguments are not independent: every entry of
     * {@code tails} has to be the tail of some entry of {@code exact}. A tail with
     * no number behind it can never match, by design — before this method compared
     * tails alone, and that was the hole it is now closing.
     */
    public static boolean matches(String canonical, Set<String> exact, Set<String> tails) {
        if (canonical == null || exact == null) {
            return false;
        }
        if (exact.contains(canonical)) {
            return true;
        }
        if (tails == null || canonical.length() < 11 || !tails.contains(tail(canonical))) {
            return false;
        }
        for (String known : exact) {
            if (sameSubscriber(canonical, known)) {
                return true;
            }
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
