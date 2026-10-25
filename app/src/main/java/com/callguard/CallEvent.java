package com.callguard;

/** One screened call: what came in and what the filter decided. */
public final class CallEvent {

    public final long time;
    /** Number exactly as delivered by the system, kept for diagnostics. */
    public final String raw;
    /** Normalised number used for matching. */
    public final String canonical;
    public final String reason;
    public final int verification;

    public CallEvent(long time, String raw, String canonical, String reason, int verification) {
        this.time = time;
        this.raw = raw;
        this.canonical = canonical;
        this.reason = reason;
        this.verification = verification;
    }

    public boolean isBlocked() {
        return Reasons.isBlocked(reason);
    }

    /** Best available human-readable number. */
    public String displayNumber() {
        if (canonical != null) {
            return PhoneNumbers.format(canonical);
        }
        return raw == null ? "" : raw;
    }
}
