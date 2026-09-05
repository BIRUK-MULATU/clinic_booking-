package et.aau.clinic.web.api.dto;

import et.aau.clinic.core.DoctorLoad;

/** A doctor's load for one day: booked patients, room left, and the status band. */
public record DoctorLoadResponse(int dailyLimit, int scheduled, int remaining, String status) {

    public static DoctorLoadResponse from(DoctorLoad load) {
        return new DoctorLoadResponse(load.dailyLimit(), load.scheduled(), load.remaining(), load.status().name());
    }
}
