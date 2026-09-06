package et.aau.clinic.domain;

/**
 * Who a logged-in account is, for the light role split this app now
 * has: a PATIENT books/cancels their own visits and joins waitlists;
 * an ADMIN (reception/front-desk) confirms appointments and runs the
 * queue. This is not a real authorization system - CLAUDE.md rules out
 * Spring Security - it's a plain field checked the same way login
 * itself is: a string comparison, nothing cryptographic.
 */
public enum Role {
    PATIENT,
    ADMIN
}
