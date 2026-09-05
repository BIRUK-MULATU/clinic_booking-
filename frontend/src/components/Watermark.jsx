/**
 * A faint decorative watermark - two doctors in conversation - sitting
 * behind the page content. Rendered only for patients (see App.jsx), not
 * for the reception/admin side, which stays plain for quick scanning.
 * Purely decorative: aria-hidden and pointer-events:none.
 */
export default function Watermark() {
  return (
    <div
      aria-hidden="true"
      style={{
        position: "fixed",
        inset: 0,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        pointerEvents: "none",
        zIndex: 0,
        overflow: "hidden",
      }}
    >
      <svg
        viewBox="0 0 520 340"
        width="min(680px, 90vw)"
        role="img"
        style={{ color: "var(--primary)", opacity: 0.06 }}
      >
        {/* speech bubbles */}
        <g fill="currentColor">
          <rect x="150" y="26" width="120" height="60" rx="18" />
          <path d="M176 84 L176 112 L204 84 Z" />
          <rect x="266" y="8" width="120" height="60" rx="18" />
          <path d="M356 66 L356 94 L328 66 Z" />
        </g>
        <g fill="var(--bg)">
          <circle cx="182" cy="56" r="7" />
          <circle cx="210" cy="56" r="7" />
          <circle cx="238" cy="56" r="7" />
          <circle cx="298" cy="38" r="7" />
          <circle cx="326" cy="38" r="7" />
          <circle cx="354" cy="38" r="7" />
        </g>

        {/* left doctor, facing right */}
        <g fill="currentColor">
          <circle cx="150" cy="150" r="34" />
          <path d="M92 340 L104 214 Q104 176 150 176 Q196 176 196 214 L208 340 Z" />
        </g>
        <g fill="var(--bg)">
          {/* coat opening */}
          <path d="M150 176 L138 214 L150 250 L162 214 Z" />
          {/* medical cross */}
          <path d="M170 232 h14 v10 h-14 v14 h-10 v-14 h-14 v-10 h14 v-14 h10 Z" />
        </g>
        {/* stethoscope */}
        <path
          d="M136 182 C112 210 116 252 150 262"
          fill="none"
          stroke="currentColor"
          strokeWidth="6"
          strokeLinecap="round"
        />
        <circle cx="150" cy="266" r="9" fill="currentColor" />

        {/* right doctor, mirrored */}
        <g transform="translate(520 0) scale(-1 1)">
          <g fill="currentColor">
            <circle cx="150" cy="150" r="34" />
            <path d="M92 340 L104 214 Q104 176 150 176 Q196 176 196 214 L208 340 Z" />
          </g>
          <g fill="var(--bg)">
            <path d="M150 176 L138 214 L150 250 L162 214 Z" />
            <path d="M170 232 h14 v10 h-14 v14 h-10 v-14 h-14 v-10 h14 v-14 h10 Z" />
          </g>
          <path
            d="M136 182 C112 210 116 252 150 262"
            fill="none"
            stroke="currentColor"
            strokeWidth="6"
            strokeLinecap="round"
          />
          <circle cx="150" cy="266" r="9" fill="currentColor" />
        </g>
      </svg>
    </div>
  );
}
