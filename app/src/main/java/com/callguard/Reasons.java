package com.callguard;

import android.telecom.Connection;

/**
 * Reason codes stored for every screened call, plus their UI labels.
 *
 * A call is either blocked or allowed; the reason explains why.
 */
public final class Reasons {

    // --- blocked ---------------------------------------------------------
    /** Number is not in the address book and carries no verification marker. */
    public static final String BLOCK_UNKNOWN = "unknown";
    /** Number is not in the address book but the carrier verified it. */
    public static final String BLOCK_UNKNOWN_VERIFIED = "unknown_verified";
    /** Number belongs to a contact but the carrier did not verify it. */
    public static final String BLOCK_CONTACT_UNVERIFIED = "contact_unverified";

    // --- allowed ---------------------------------------------------------
    public static final String ALLOW_CONTACT = "contact";
    public static final String ALLOW_WHITELIST = "whitelist";
    public static final String ALLOW_FILTER_OFF = "filter_off";
    public static final String ALLOW_PAUSED = "paused";
    public static final String ALLOW_SCHEDULE = "schedule";
    public static final String ALLOW_NEXT = "allow_next";
    public static final String ALLOW_NO_NUMBER = "no_number";
    public static final String ALLOW_INDEX_ERROR = "index_error";

    private Reasons() {
    }

    public static boolean isBlocked(String reason) {
        return BLOCK_UNKNOWN.equals(reason)
                || BLOCK_UNKNOWN_VERIFIED.equals(reason)
                || BLOCK_CONTACT_UNVERIFIED.equals(reason);
    }

    public static int labelRes(String reason) {
        if (BLOCK_UNKNOWN_VERIFIED.equals(reason)) {
            return R.string.reason_unknown_verified;
        }
        if (BLOCK_CONTACT_UNVERIFIED.equals(reason)) {
            return R.string.reason_contact_unverified;
        }
        if (ALLOW_CONTACT.equals(reason)) {
            return R.string.reason_contact;
        }
        if (ALLOW_WHITELIST.equals(reason)) {
            return R.string.reason_whitelist;
        }
        if (ALLOW_FILTER_OFF.equals(reason)) {
            return R.string.reason_filter_off;
        }
        if (ALLOW_PAUSED.equals(reason)) {
            return R.string.reason_paused;
        }
        if (ALLOW_SCHEDULE.equals(reason)) {
            return R.string.reason_schedule;
        }
        if (ALLOW_NEXT.equals(reason)) {
            return R.string.reason_allow_next;
        }
        if (ALLOW_NO_NUMBER.equals(reason)) {
            return R.string.reason_no_number;
        }
        if (ALLOW_INDEX_ERROR.equals(reason)) {
            return R.string.reason_index_error;
        }
        return R.string.reason_unknown;
    }

    public static int markerRes(int verificationStatus) {
        if (verificationStatus == Connection.VERIFICATION_STATUS_PASSED) {
            return R.string.marker_passed;
        }
        if (verificationStatus == Connection.VERIFICATION_STATUS_FAILED) {
            return R.string.marker_failed;
        }
        return R.string.marker_none;
    }
}
