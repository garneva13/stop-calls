package com.callguard;

import android.net.Uri;
import android.os.Build;
import android.telecom.Call;
import android.telecom.CallScreeningService;
import android.telecom.Connection;

/**
 * System bound service that sees every incoming call before the phone starts
 * ringing and decides whether the call is allowed through.
 *
 * Telecom binds this service on demand, so the app does not need a foreground
 * service and does not consume battery while idle. Every screened call is
 * recorded, including allowed ones, so the diagnostics screen can show whether
 * the service is being invoked at all.
 */
public final class CallGuardService extends CallScreeningService {

    @Override
    public void onScreenCall(Call.Details callDetails) {
        if (callDetails == null) {
            return;
        }
        // Outgoing calls are answered automatically by the framework.
        if (callDetails.getCallDirection() != Call.Details.DIRECTION_INCOMING) {
            return;
        }

        String raw = rawNumber(callDetails);
        int verification = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                // The carrier verification marker exists from Android 11 only.
                verification = callDetails.getCallerNumberVerificationStatus();
            } catch (Throwable ignored) {
                // Older or unusual builds may not provide it; 0 means "not verified".
            }
        }

        Decision decision;
        try {
            decision = decide(raw, verification);
        } catch (Throwable ignored) {
            decision = Decision.allow(Reasons.ALLOW_INDEX_ERROR, raw,
                    Numbers.canonical(this, raw), verification, System.currentTimeMillis());
        }

        // The call has to be answered within five seconds, otherwise it rings.
        try {
            respondToCall(callDetails, decision.response);
        } catch (Throwable ignored) {
            // Nothing else can be done here; the platform falls back to "allow".
        }

        try {
            record(decision);
        } catch (Throwable ignored) {
            // A logging failure must never affect the call itself.
        }
    }

    private Decision decide(String raw, int verification) {
        Settings settings = Settings.get(this);
        long now = System.currentTimeMillis();
        String canonical = Numbers.canonical(this, raw);
        boolean verified = verification == Connection.VERIFICATION_STATUS_PASSED;

        if (!settings.isEnabled()) {
            return Decision.allow(Reasons.ALLOW_FILTER_OFF, raw, canonical, verification, now);
        }
        if (settings.isPaused()) {
            return Decision.allow(Reasons.ALLOW_PAUSED, raw, canonical, verification, now);
        }
        if (settings.isScheduleActiveNow()) {
            return Decision.allow(Reasons.ALLOW_SCHEDULE, raw, canonical, verification, now);
        }
        if (canonical == null) {
            // No usable number: never block blindly.
            return Decision.allow(Reasons.ALLOW_NO_NUMBER, raw, null, verification, now);
        }

        DbHelper db = DbHelper.get(this);
        if (db.isWhitelisted(canonical)) {
            // An explicitly allowed number always wins.
            return Decision.allow(Reasons.ALLOW_WHITELIST, raw, canonical, verification, now);
        }

        ContactIndex.Match match = ContactIndex.lookup(this, canonical);
        if (!match.available) {
            // Contacts are unreadable: letting the call through is safer than
            // blocking a caller we simply could not verify.
            return Decision.allow(Reasons.ALLOW_INDEX_ERROR, raw, canonical, verification, now);
        }
        if (match.name != null) {
            if (settings.isBlockUnverified() && !verified) {
                return Decision.block(Reasons.BLOCK_CONTACT_UNVERIFIED, raw, canonical,
                        verification, now);
            }
            return Decision.allow(Reasons.ALLOW_CONTACT, raw, canonical, verification, now);
        }

        if (settings.hasAllowNext()) {
            settings.consumeAllowNext();
            return Decision.allow(Reasons.ALLOW_NEXT, raw, canonical, verification, now);
        }

        return Decision.block(verified ? Reasons.BLOCK_UNKNOWN_VERIFIED : Reasons.BLOCK_UNKNOWN,
                raw, canonical, verification, now);
    }

    private void record(Decision decision) {
        DbHelper.get(this).addEvent(decision.time, decision.raw, decision.canonical,
                decision.reason, decision.verification);
    }

    private static String rawNumber(Call.Details details) {
        Uri handle = details.getHandle();
        if (handle == null) {
            return null;
        }
        String part = handle.getSchemeSpecificPart();
        return part != null ? Uri.decode(part) : null;
    }

    /** Immutable outcome of the screening decision. */
    private static final class Decision {

        final CallResponse response;
        final boolean blocked;
        final String reason;
        final String raw;
        final String canonical;
        final int verification;
        final long time;

        private Decision(CallResponse response, boolean blocked, String reason, String raw,
                         String canonical, int verification, long time) {
            this.response = response;
            this.blocked = blocked;
            this.reason = reason;
            this.raw = raw;
            this.canonical = canonical;
            this.verification = verification;
            this.time = time;
        }

        static Decision allow(String reason, String raw, String canonical,
                              int verification, long time) {
            CallResponse response = new CallResponse.Builder()
                    .setDisallowCall(false)
                    .build();
            return new Decision(response, false, reason, raw, canonical, verification, time);
        }

        static Decision block(String reason, String raw, String canonical,
                              int verification, long time) {
            CallResponse response = new CallResponse.Builder()
                    .setDisallowCall(true)
                    .setRejectCall(true)
                    .setSilenceCall(true)
                    .build();
            return new Decision(response, true, reason, raw, canonical, verification, time);
        }
    }
}
