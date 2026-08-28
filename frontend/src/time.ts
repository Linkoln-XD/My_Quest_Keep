export function pad(n: number): string {
	return String(n).padStart(2, "0");
}

export function toDatetimeLocal(iso: string): string {
	const d = new Date(iso);
	return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export function fromDatetimeLocal(value: string): string {
	return new Date(value).toISOString();
}

/** Default demo slot: three days ahead, 12:00–14:00 local, minutes aligned to 30. */
export function defaultSlot(): { startLocal: string; endLocal: string } {
	const start = new Date();
	start.setDate(start.getDate() + 3);
	start.setHours(12, 0, 0, 0);
	const end = new Date(start.getTime() + 2 * 60 * 60 * 1000);
	return { startLocal: toDatetimeLocal(start.toISOString()), endLocal: toDatetimeLocal(end.toISOString()) };
}

export function formatInstant(iso: string): string {
	return new Date(iso).toLocaleString(undefined, {
		dateStyle: "medium",
		timeStyle: "short",
	});
}
