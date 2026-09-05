package et.aau.clinic.web.api.dto;

/** A data URL ("data:image/...;base64,...") for setting or replacing a doctor's photo. */
public record DoctorPhotoRequest(String photo) {
}
