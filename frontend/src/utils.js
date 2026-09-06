export function formatSlot(isoLocalDateTime) {
  const [datePart, timePart] = isoLocalDateTime.split("T");
  const date = new Date(`${datePart}T${timePart}`);
  const dateLabel = date.toLocaleDateString(undefined, {
    weekday: "short",
    month: "short",
    day: "numeric",
  });
  const timeLabel = timePart.slice(0, 5);
  return { dateLabel, timeLabel, raw: `${datePart} ${timeLabel}` };
}

export function formatMoney(amount) {
  if (amount === null || amount === undefined) return "—";
  return `${Number(amount).toFixed(2)} ETB`;
}

export function formatAvailability(rules) {
  if (!rules || rules.length === 0) return "No schedule set";
  return rules
    .map((r) => `${r.dayOfWeek.slice(0, 3)} ${r.startTime.slice(0, 5)}–${r.endTime.slice(0, 5)}`)
    .join(" · ");
}

/**
 * Read an image file from the device, downscale it to at most maxSize on its
 * longest edge, and resolve a JPEG data URL. Downscaling keeps the stored
 * photo small — a headshot ends up a few tens of KB, not a few MB.
 */
export function readImageAsDataUrl(file, maxSize = 400) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onerror = () => reject(new Error("Could not read that file."));
    reader.onload = () => {
      const img = new Image();
      img.onerror = () => reject(new Error("That file is not a valid image."));
      img.onload = () => {
        const scale = Math.min(1, maxSize / Math.max(img.width, img.height));
        const canvas = document.createElement("canvas");
        canvas.width = Math.round(img.width * scale);
        canvas.height = Math.round(img.height * scale);
        canvas.getContext("2d").drawImage(img, 0, 0, canvas.width, canvas.height);
        resolve(canvas.toDataURL("image/jpeg", 0.8));
      };
      img.src = reader.result;
    };
    reader.readAsDataURL(file);
  });
}
