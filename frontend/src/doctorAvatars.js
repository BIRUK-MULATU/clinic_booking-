/**
 * Illustrated portrait avatars - one male, one female doctor - used as the
 * default picture for a doctor who has no photo uploaded yet. They are
 * inline SVGs (encoded as data URIs) so nothing is fetched over the
 * network. A real photo uploaded via "Add photo" always takes precedence.
 */

const male = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200">
<circle cx="100" cy="100" r="100" fill="#e3f3f1"/>
<path d="M34 200c0-42 26-58 66-58s66 16 66 58Z" fill="#fff"/>
<path d="M92 143h16l-4 57h-8Z" fill="#eef4f6"/>
<path d="M100 142l14 22-14 20-14-20Z" fill="#0b5b55"/>
<rect x="89" y="112" width="22" height="30" rx="9" fill="#e0a983"/>
<circle cx="100" cy="86" r="35" fill="#f0c39c"/>
<circle cx="64" cy="88" r="7" fill="#f0c39c"/><circle cx="136" cy="88" r="7" fill="#f0c39c"/>
<path d="M64 80c0-30 72-30 72 0 0-14-16-24-36-24S64 66 64 80Z" fill="#3b302a"/>
<path d="M64 80c-4-8-2-24 10-30-6 12-4 22-4 30Z" fill="#3b302a"/>
<rect x="79" y="80" width="15" height="4" rx="2" fill="#3b302a"/>
<rect x="106" y="80" width="15" height="4" rx="2" fill="#3b302a"/>
<circle cx="86" cy="91" r="3.4" fill="#2a2320"/><circle cx="114" cy="91" r="3.4" fill="#2a2320"/>
<path d="M100 95l-4 12h8Z" fill="#e0a983"/>
<path d="M89 114q11 9 22 0" stroke="#b5765a" stroke-width="3" fill="none" stroke-linecap="round"/>
<path d="M82 133c-7 22-2 40 18 45 20-5 25-23 18-45" stroke="#0f7a72" stroke-width="4.5" fill="none" stroke-linecap="round"/>
<circle cx="118" cy="176" r="6.5" fill="#0f7a72"/>
</svg>`;

const female = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200">
<circle cx="100" cy="100" r="100" fill="#e3f3f1"/>
<path d="M56 96c-6-46 94-46 88 0 0 34-10 62-10 62l-16 6H82l-16-6s-10-28-10-62Z" fill="#5b3a29"/>
<path d="M34 200c0-42 26-58 66-58s66 16 66 58Z" fill="#fff"/>
<path d="M92 143h16l-4 57h-8Z" fill="#eef4f6"/>
<path d="M100 142l14 22-14 20-14-20Z" fill="#7a1f3d"/>
<rect x="89" y="112" width="22" height="30" rx="9" fill="#f0c39c"/>
<circle cx="100" cy="86" r="35" fill="#f6cda4"/>
<circle cx="64" cy="90" r="7" fill="#f6cda4"/><circle cx="136" cy="90" r="7" fill="#f6cda4"/>
<path d="M62 84c0-34 76-34 76 0 0-16-16-28-38-28S62 68 62 84Z" fill="#5b3a29"/>
<path d="M62 84c-4 20-2 44 6 58-16-8-18-44-6-58Zm76 0c4 20 2 44-6 58 16-8 18-44 6-58Z" fill="#5b3a29"/>
<rect x="80" y="82" width="14" height="4" rx="2" fill="#4a2f22"/>
<rect x="106" y="82" width="14" height="4" rx="2" fill="#4a2f22"/>
<circle cx="87" cy="92" r="3.4" fill="#2a2320"/><circle cx="113" cy="92" r="3.4" fill="#2a2320"/>
<path d="M100 96l-3 11h6Z" fill="#e7b98c"/>
<path d="M90 115q10 8 20 0" stroke="#c97b6a" stroke-width="3" fill="none" stroke-linecap="round"/>
<path d="M82 133c-7 22-2 40 18 45 20-5 25-23 18-45" stroke="#0f7a72" stroke-width="4.5" fill="none" stroke-linecap="round"/>
<circle cx="118" cy="176" r="6.5" fill="#0f7a72"/>
</svg>`;

export const DOCTOR_AVATAR_MALE = `data:image/svg+xml,${encodeURIComponent(male)}`;
export const DOCTOR_AVATAR_FEMALE = `data:image/svg+xml,${encodeURIComponent(female)}`;

// Deterministic per doctor so the same doctor always gets the same default face.
export function defaultDoctorAvatar(id) {
  return Number(id) % 2 === 0 ? DOCTOR_AVATAR_FEMALE : DOCTOR_AVATAR_MALE;
}
