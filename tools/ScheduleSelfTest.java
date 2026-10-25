package com.callguard;

/**
 * Plain-JVM checks for {@link Schedule}. Not shipped inside the APK.
 *
 *   javac -d out app/src/main/java/com/callguard/Schedule.java tools/ScheduleSelfTest.java
 *   java -cp out com.callguard.ScheduleSelfTest
 */
public final class ScheduleSelfTest {

    private static int failures;
    private static int checks;

    // 0 = Monday ... 6 = Sunday
    private static final int MON = 0;
    private static final int TUE = 1;
    private static final int WED = 2;
    private static final int THU = 3;
    private static final int FRI = 4;
    private static final int SAT = 5;
    private static final int SUN = 6;

    public static void main(String[] args) {
        daytimeWindows();
        midnightWindows();
        masks();

        System.out.println();
        System.out.println(checks + " checks, " + failures + " failures");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void daytimeWindows() {
        section("09:00-18:00 on weekdays");
        int mask = Schedule.WEEKDAYS;
        int start = 9 * 60;
        int end = 18 * 60;

        expect(true, Schedule.isActive(mask, start, end, MON, 9 * 60), "monday 09:00");
        expect(true, Schedule.isActive(mask, start, end, MON, 12 * 60), "monday noon");
        expect(true, Schedule.isActive(mask, start, end, FRI, 17 * 60 + 59), "friday 17:59");
        expect(false, Schedule.isActive(mask, start, end, MON, 18 * 60), "monday 18:00");
        expect(false, Schedule.isActive(mask, start, end, MON, 8 * 60 + 59), "monday 08:59");
        expect(false, Schedule.isActive(mask, start, end, SAT, 12 * 60), "saturday noon");
        expect(false, Schedule.isActive(mask, start, end, SUN, 12 * 60), "sunday noon");
        expect(false, Schedule.isActive(mask, start, start, MON, 9 * 60), "empty window");
        expect(false, Schedule.isActive(0, start, end, MON, 12 * 60), "no days selected");
    }

    private static void midnightWindows() {
        section("22:00-08:00 crossing midnight");
        int mask = Schedule.EVERY_DAY;
        int start = 22 * 60;
        int end = 8 * 60;

        expect(true, Schedule.isActive(mask, start, end, MON, 22 * 60), "monday 22:00");
        expect(true, Schedule.isActive(mask, start, end, MON, 23 * 60 + 30), "monday 23:30");
        expect(true, Schedule.isActive(mask, start, end, TUE, 3 * 60), "tuesday 03:00 (from monday)");
        expect(true, Schedule.isActive(mask, start, end, TUE, 7 * 60 + 59), "tuesday 07:59");
        expect(false, Schedule.isActive(mask, start, end, TUE, 8 * 60), "tuesday 08:00");
        expect(false, Schedule.isActive(mask, start, end, TUE, 21 * 60 + 59), "tuesday 21:59");

        section("22:00-08:00 only on sunday");
        int sundayOnly = 1 << SUN;
        expect(true, Schedule.isActive(sundayOnly, start, end, SUN, 23 * 60), "sunday 23:00");
        expect(true, Schedule.isActive(sundayOnly, start, end, MON, 2 * 60),
                "monday 02:00 (started sunday)");
        expect(false, Schedule.isActive(sundayOnly, start, end, MON, 23 * 60),
                "monday 23:00 (not selected)");
        expect(false, Schedule.isActive(sundayOnly, start, end, SUN, 2 * 60),
                "sunday 02:00 (started saturday)");
    }

    private static void masks() {
        section("day masks");
        expect(true, Schedule.isDaySelected(Schedule.WEEKDAYS, MON), "weekdays include monday");
        expect(false, Schedule.isDaySelected(Schedule.WEEKDAYS, SAT), "weekdays exclude saturday");
        expect(true, Schedule.isDaySelected(Schedule.WEEKEND, SUN), "weekend includes sunday");
        expect(5, Schedule.selectedCount(Schedule.WEEKDAYS), "five weekdays");
        expect(0, Schedule.selectedCount(Schedule.withDay(1 << MON, MON, false)), "toggle off");
        expect(1, Schedule.selectedCount(Schedule.withDay(0, MON, true)), "toggle on");

        section("formatting");
        expectEquals("09:00", Schedule.time(9 * 60), "09:00");
        expectEquals("18:05", Schedule.time(18 * 60 + 5), "18:05");
        expectEquals("00:00", Schedule.time(0), "00:00");
    }

    private static void expect(boolean expected, boolean actual, String label) {
        checks++;
        if (expected != actual) {
            failures++;
            System.out.println("  FAIL: " + label + " — expected " + expected + " but was " + actual);
        }
    }

    private static void expectEquals(String expected, String actual, String label) {
        checks++;
        if (!expected.equals(actual)) {
            failures++;
            System.out.println("  FAIL: " + label + " — expected <" + expected + "> but was <"
                    + actual + ">");
        }
    }

    private static void expect(int expected, int actual, String label) {
        checks++;
        if (expected != actual) {
            failures++;
            System.out.println("  FAIL: " + label + " — expected " + expected + " but was " + actual);
        }
    }

    private static void section(String name) {
        System.out.println("[" + name + "]");
    }
}
