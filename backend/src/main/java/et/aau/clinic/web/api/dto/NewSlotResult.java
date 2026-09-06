package et.aau.clinic.web.api.dto;

/**
 * The result of adding a slot: the slot itself, plus a warning when the
 * doctor is now over their daily patient limit for that day (the slot
 * is still created - reception is only notified, not blocked).
 */
public record NewSlotResult(SlotResponse slot, String warning) {
}
