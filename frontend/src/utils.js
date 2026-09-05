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
