import { useState } from "react";

/**
 * A two-click delete button - first click arms it ("Confirm?"), second
 * click within a few seconds runs onConfirm. Avoids window.confirm(),
 * which blocks the page.
 */
export default function ConfirmButton({ id, onConfirm, label = "Delete", disabled }) {
  const [armed, setArmed] = useState(false);

  if (armed) {
    return (
      <span style={{ display: "inline-flex", gap: 6 }}>
        <button
          className="btn btn-danger btn-sm"
          id={id}
          disabled={disabled}
          onClick={() => {
            setArmed(false);
            onConfirm();
          }}
        >
          Confirm?
        </button>
        <button className="btn btn-secondary btn-sm" onClick={() => setArmed(false)}>
          Cancel
        </button>
      </span>
    );
  }

  return (
    <button
      className="btn btn-danger btn-sm"
      id={id}
      disabled={disabled}
      onClick={() => {
        setArmed(true);
        setTimeout(() => setArmed(false), 4000);
      }}
    >
      {label}
    </button>
  );
}
