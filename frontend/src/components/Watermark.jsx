/**
 * A faint decorative watermark - a male and a female doctor in
 * conversation - sitting behind the page content. Rendered only for
 * patients (see App.jsx), not the reception/admin side, which stays
 * plain for quick scanning. Purely decorative: aria-hidden,
 * pointer-events:none.
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
          <rect x="150" y="24" width="118" height="58" rx="18" />
          <path d="M176 80 L176 106 L202 80 Z" />
          <rect x="264" y="8" width="118" height="58" rx="18" />
          <path d="M354 64 L354 90 L328 64 Z" />
        </g>
        <g fill="var(--bg)">
          <circle cx="182" cy="53" r="7" />
          <circle cx="209" cy="53" r="7" />
          <circle cx="236" cy="53" r="7" />
          <circle cx="296" cy="37" r="7" />
          <circle cx="323" cy="37" r="7" />
          <circle cx="350" cy="37" r="7" />
        </g>

        {/* left - male doctor bust, facing right */}
        <g>
          <path d="M78 340 C78 250 108 224 150 224 C192 224 222 250 222 340 Z" fill="currentColor" />
          <circle cx="150" cy="160" r="42" fill="currentColor" />
          {/* short hair cap */}
          <path d="M108 158 C108 108 192 108 192 158 C192 132 174 116 150 116 C126 116 108 132 108 158 Z" fill="currentColor" />
          {/* coat opening + collar */}
          <path d="M150 224 L134 268 L150 306 L166 268 Z" fill="var(--bg)" />
          <path d="M150 224 L120 244 L132 262 L150 236 Z" fill="var(--bg)" />
          <path d="M150 224 L180 244 L168 262 L150 236 Z" fill="var(--bg)" />
          {/* stethoscope */}
          <path d="M132 230 C104 262 110 312 150 322" fill="none" stroke="var(--bg)" strokeWidth="8" strokeLinecap="round" />
          <circle cx="168" cy="300" r="11" fill="var(--bg)" />
        </g>

        {/* right - female doctor bust, facing left */}
        <g transform="translate(520 0) scale(-1 1)">
          {/* long hair behind */}
          <path d="M96 300 C88 190 212 190 204 300 L184 300 C190 220 110 220 116 300 Z" fill="currentColor" />
          <path d="M78 340 C78 250 108 224 150 224 C192 224 222 250 222 340 Z" fill="currentColor" />
          <circle cx="150" cy="160" r="42" fill="currentColor" />
          {/* hair framing the face */}
          <path d="M104 168 C100 118 200 118 196 168 C196 130 176 112 150 112 C124 112 104 130 104 168 Z" fill="currentColor" />
          <path d="M150 224 L134 268 L150 306 L166 268 Z" fill="var(--bg)" />
          <path d="M150 224 L120 244 L132 262 L150 236 Z" fill="var(--bg)" />
          <path d="M150 224 L180 244 L168 262 L150 236 Z" fill="var(--bg)" />
          <path d="M132 230 C104 262 110 312 150 322" fill="none" stroke="var(--bg)" strokeWidth="8" strokeLinecap="round" />
          <circle cx="168" cy="300" r="11" fill="var(--bg)" />
        </g>
      </svg>
    </div>
  );
}
