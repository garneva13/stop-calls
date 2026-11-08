package com.callguard;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Plain-JVM checks for {@link PhoneNumbers}. Not shipped inside the APK.
 *
 *   javac -d out app/src/main/java/com/callguard/PhoneNumbers.java tools/PhoneNumbersSelfTest.java
 *   java -cp out com.callguard.PhoneNumbersSelfTest
 */
public final class PhoneNumbersSelfTest {

    private static int failures;
    private static int checks;

    public static void main(String[] args) {
        canonicalCases();
        shortNumberCases();
        matchCases();
        formatCases();

        System.out.println();
        System.out.println(checks + " checks, " + failures + " failures");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void canonicalCases() {
        section("canonical");
        expect("79161234567", PhoneNumbers.canonical("+7 (916) 123-45-67"));
        expect("79161234567", PhoneNumbers.canonical("89161234567"));
        expect("79161234567", PhoneNumbers.canonical("8 916 123 45 67"));
        expect("79161234567", PhoneNumbers.canonical("8-916-123-45-67"));
        expect("79161234567", PhoneNumbers.canonical("9161234567"));
        expect("79161234567", PhoneNumbers.canonical(" tel:+79161234567 "));
        expect("79161234567", PhoneNumbers.canonical("+7 916 123 45 67;123"));
        expect("79161234567", PhoneNumbers.canonical("+7 (916) 123-45-67 доб."));
        expect("14155550100", PhoneNumbers.canonical("+1 415 555 0100"));
        expect("14155550100", PhoneNumbers.canonical("0014155550100"));
        expect("493012345678", PhoneNumbers.canonical("+49 30 12345678"));
        expect("112", PhoneNumbers.canonical("112"));
        expect(null, PhoneNumbers.canonical(null));
        expect(null, PhoneNumbers.canonical(""));
        expect(null, PhoneNumbers.canonical("   "));
        expect(null, PhoneNumbers.canonical("+"));
        expect(null, PhoneNumbers.canonical("abc"));
        expect(null, PhoneNumbers.canonical("12"));

        section("fallback for non-russian countries (empty country code)");
        // Without the Russian heuristics a national number is kept as written,
        // and international numbers keep their country code.
        expect("03012345678", PhoneNumbers.canonical("030 12345678", ""));
        expect("493012345678", PhoneNumbers.canonical("+49 30 12345678", ""));
        expect("4712345678", PhoneNumbers.canonical("+47 123 45 678", ""));
        expect("79161234567", PhoneNumbers.canonical("+7 916 123-45-67", ""));
    }

    private static void matchCases() {
        section("matching against contacts");
        Set<String> exact = new HashSet<>(Arrays.asList(
                "79161234567",   // +7 916 123-45-67
                "79001234567",   // +7 900 123-45-67
                "14155550100"    // +1 415 555 0100
        ));
        Set<String> tails = new HashSet<>();
        for (String value : exact) {
            if (value.length() >= 11) {
                tails.add(PhoneNumbers.tail(value));
            }
        }

        expectTrue(PhoneNumbers.matches("79161234567", exact, tails), "same number");
        expectFalse(PhoneNumbers.matches("79161234568", exact, tails), "last digit differs");
        expectFalse(PhoneNumbers.matches("7916123456", exact, tails), "too short");
        expectTrue(PhoneNumbers.matches("14155550100", exact, tails), "foreign number");
        expectFalse(PhoneNumbers.matches("14155550101", exact, tails), "foreign number differs");
        expectFalse(PhoneNumbers.matches(null, exact, tails), "null number");
        expectFalse(PhoneNumbers.matches("79161234567", new HashSet<String>(),
                new HashSet<String>()), "empty contacts");
        // A tail on its own is not enough: it only pre-filters, and the decision is
        // taken on the full numbers, so a tail without a matching contact means no.
        expectFalse(PhoneNumbers.matches("79161234567", new HashSet<String>(),
                new HashSet<>(Arrays.asList("9161234567"))), "tail without a contact");
        expectFalse(PhoneNumbers.matches("79161234567", new HashSet<String>(),
                new HashSet<>(Arrays.asList("9161234568"))), "tail differs");

        section("same last ten digits is not enough (spoofed caller id)");
        // The trunk-prefix spelling of the very same subscriber: same length, allowed.
        expectTrue(PhoneNumbers.matches("89161234567", exact, tails), "8 instead of 7");
        // Longer numbers that merely end with a contact's ten digits: a different
        // subscriber in a different country, must not pass as a contact.
        expectFalse(PhoneNumbers.matches("3809161234567", exact, tails), "ukrainian lookalike");
        expectFalse(PhoneNumbers.matches("9989161234567", exact, tails), "uzbek lookalike");
        expectFalse(PhoneNumbers.matches("999999999979161234567", exact, tails),
                "padded lookalike");
        expectFalse(PhoneNumbers.matches("19161234567", exact, tails),
                "same length, different country, no such contact");

        section("sameSubscriber");
        expectTrue(PhoneNumbers.sameSubscriber("79161234567", "79161234567"), "identical");
        expectTrue(PhoneNumbers.sameSubscriber("89161234567", "79161234567"), "trunk prefix");
        expectFalse(PhoneNumbers.sameSubscriber("3809161234567", "79161234567"), "longer");
        expectFalse(PhoneNumbers.sameSubscriber("79161234567", "3809161234567"), "shorter");
        expectFalse(PhoneNumbers.sameSubscriber("9161234567", "9161234567".substring(0, 9)),
                "different length short numbers");
        expectTrue(PhoneNumbers.sameSubscriber("112", "112"), "short code, identical");
        expectFalse(PhoneNumbers.sameSubscriber("112", "113"), "short code, different");
        expectFalse(PhoneNumbers.sameSubscriber(null, "79161234567"), "null");
    }

    /**
     * Short service codes (112, 102, 900) deliberately get a canonical form and are
     * screened like any other number: spoofing them is a common fraud, so blocking
     * an unknown one is the intended behaviour, and the whitelist is how a user lets
     * a particular one through. These checks pin that down so a future change to the
     * canonicalisation cannot alter the policy unnoticed.
     */
    private static void shortNumberCases() {
        section("short service codes keep a canonical form (blocking policy)");
        expect("112", PhoneNumbers.canonical("112"));
        expect("102", PhoneNumbers.canonical("102"));
        expect("900", PhoneNumbers.canonical("900"));
        expect("112", PhoneNumbers.canonical("*112#"));
        expect("112", PhoneNumbers.canonical("tel:112"));
        expect(null, PhoneNumbers.canonical("12"));
    }

    private static void formatCases() {
        section("formatting");
        expect("+7 916 123-45-67", PhoneNumbers.format("79161234567"));
        expect("+1 (415) 555-0100", PhoneNumbers.format("14155550100"));
        expect("+493012345678", PhoneNumbers.format("493012345678"));
        expect("", PhoneNumbers.format(null));
    }

    private static void expect(String expected, String actual) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            failures++;
            System.out.println("  FAIL: expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void expectTrue(boolean value, String label) {
        checks++;
        if (!value) {
            failures++;
            System.out.println("  FAIL: expected true: " + label);
        }
    }

    private static void expectFalse(boolean value, String label) {
        checks++;
        if (value) {
            failures++;
            System.out.println("  FAIL: expected false: " + label);
        }
    }

    private static void section(String name) {
        System.out.println("[" + name + "]");
    }
}
