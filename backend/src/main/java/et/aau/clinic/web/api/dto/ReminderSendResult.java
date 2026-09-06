package et.aau.clinic.web.api.dto;

import et.aau.clinic.core.ReminderDecision;

/**
 * The outcome of reception pressing "send reminder" on one appointment
 * (hospital-expansion Rule F): either sent=true, or sent=false with the
 * ReminderPolicy skip reason (ALREADY_REMINDED, NOT_YET_DUE, ...) so the
 * UI can say why nothing was sent.
 */
public record ReminderSendResult(boolean sent, String reason) {

    public static ReminderSendResult from(ReminderDecision decision) {
        return new ReminderSendResult(
                decision.isDue(),
                decision.isDue() ? null : decision.getReason().name());
    }
}
