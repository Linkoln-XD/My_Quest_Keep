import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";

import { getPage, pageQuery, sendJson } from "../api/client";
import { ApiError, type BookingRow, type CopyRow, type GameRow, type TableRow } from "../api/types";
import { useAuth } from "../auth/AuthContext";
import { defaultSlot, formatInstant, fromDatetimeLocal } from "../time";

export function BookPage() {
	const { user } = useAuth();
	const staff = user?.role === "STAFF";
	const slot = useMemo(() => defaultSlot(), []);
	const [tables, setTables] = useState<TableRow[]>([]);
	const [copies, setCopies] = useState<(CopyRow & { title: string })[]>([]);
	const [mine, setMine] = useState<BookingRow[]>([]);
	const [club, setClub] = useState<BookingRow[] | null>(null);
	const [error, setError] = useState<string | null>(null);
	const [ok, setOk] = useState<string | null>(null);
	const [busy, setBusy] = useState(false);
	const [lastKey, setLastKey] = useState<string | null>(null);
	const [reuseKey, setReuseKey] = useState(false);

	const reload = useCallback(async () => {
		const [t, g, my] = await Promise.all([
			getPage<TableRow>(`/api/v1/tables?${pageQuery()}`),
			getPage<GameRow>(`/api/v1/games?${pageQuery()}`),
			getPage<BookingRow>(`/api/v1/bookings/me?${pageQuery()}`),
		]);
		setTables(t.content);
		setMine(my.content);
		const nested = await Promise.all(
			g.content.map(async (game) => {
				const page = await getPage<CopyRow>(`/api/v1/games/${game.id}/copies?${pageQuery()}`);
				return page.content.map((copy) => ({ ...copy, title: game.title }));
			}),
		);
		setCopies(nested.flat());
		if (staff) {
			const all = await getPage<BookingRow>(`/api/v1/bookings?${pageQuery()}`);
			setClub(all.content);
		} else {
			setClub(null);
		}
	}, [staff]);

	useEffect(() => {
		reload().catch((err) => setError(err instanceof ApiError ? err.detail : "Не удалось загрузить брони"));
	}, [reload]);

	function fail(err: unknown) {
		setOk(null);
		setError(err instanceof ApiError ? `${err.status}: ${err.detail}` : "Ошибка запроса");
	}

	async function onBook(event: FormEvent<HTMLFormElement>) {
		event.preventDefault();
		const data = new FormData(event.currentTarget);
		const key = reuseKey && lastKey ? lastKey : crypto.randomUUID();
		setBusy(true);
		try {
			const booking = await sendJson<BookingRow>(
				"/api/v1/bookings",
				"POST",
				{
					tableId: String(data.get("tableId")),
					gameCopyId: String(data.get("gameCopyId")),
					startAt: fromDatetimeLocal(String(data.get("startAt"))),
					endAt: fromDatetimeLocal(String(data.get("endAt"))),
					guestCount: Number(data.get("guestCount")),
				},
				key,
			);
			setLastKey(key);
			setOk(`Бронь ${booking.status}. Ключ идемпотентности сохранён — можно повторить тот же запрос.`);
			setError(null);
			await reload();
		} catch (err) {
			fail(err);
		} finally {
			setBusy(false);
		}
	}

	async function cancel(id: string) {
		setBusy(true);
		try {
			const booking = await sendJson<BookingRow>(`/api/v1/bookings/${id}/cancel`, "POST");
			setOk(`Статус: ${booking.status}`);
			setError(null);
			await reload();
		} catch (err) {
			fail(err);
		} finally {
			setBusy(false);
		}
	}

	return (
		<>
			{error ? <div className="flash">{error}</div> : null}
			{ok ? <div className="flash ok">{ok}</div> : null}

			<section className="panel">
				<h2>Новая бронь</h2>
				<p className="lede">Стол и конкретная копия вместе. 409 — пересечение у другого гостя на тот же стол или ту же копию.</p>
				<form onSubmit={onBook}>
					<div className="row">
						<div>
							<label htmlFor="tableId">Стол</label>
							<select id="tableId" name="tableId" required defaultValue="">
								<option value="" disabled>
									выберите
								</option>
								{tables.map((table) => (
									<option key={table.id} value={table.id}>
										{table.name} ({table.capacity} мест)
									</option>
								))}
							</select>
						</div>
						<div>
							<label htmlFor="gameCopyId">Копия игры</label>
							<select id="gameCopyId" name="gameCopyId" required defaultValue="">
								<option value="" disabled>
									выберите
								</option>
								{copies.map((copy) => (
									<option key={copy.id} value={copy.id}>
										{copy.title} · {copy.id.slice(0, 8)}
									</option>
								))}
							</select>
						</div>
					</div>
					<div className="row">
						<div>
							<label htmlFor="startAt">Начало</label>
							<input id="startAt" name="startAt" type="datetime-local" required defaultValue={slot.startLocal} />
						</div>
						<div>
							<label htmlFor="endAt">Конец (не включая)</label>
							<input id="endAt" name="endAt" type="datetime-local" required defaultValue={slot.endLocal} />
						</div>
						<div>
							<label htmlFor="guestCount">Гостей</label>
							<input id="guestCount" name="guestCount" type="number" min={1} max={8} defaultValue={2} required />
						</div>
					</div>
					<label>
						<input type="checkbox" checked={reuseKey} onChange={(e) => setReuseKey(e.target.checked)} disabled={!lastKey} />{" "}
						Повторить с тем же Idempotency-Key (должна вернуться та же бронь)
					</label>
					{lastKey ? <p className="hint mono">Последний ключ: {lastKey}</p> : null}
					<div className="actions">
						<button className="primary" type="submit" disabled={busy || tables.length === 0 || copies.length === 0}>
							Забронировать
						</button>
					</div>
				</form>
			</section>

			<section className="panel">
				<h2>Мои брони</h2>
				<BookingTable rows={mine} tables={tables} copies={copies} onCancel={cancel} busy={busy} />
			</section>

			{staff && club ? (
				<section className="panel">
					<h2>Все брони клуба</h2>
					<p className="lede">GET /bookings — только STAFF. Гость сюда не попадёт (403).</p>
					<BookingTable rows={club} tables={tables} copies={copies} onCancel={cancel} busy={busy} showUser />
				</section>
			) : null}
		</>
	);
}

function BookingTable({
	rows,
	tables,
	copies,
	onCancel,
	busy,
	showUser = false,
}: {
	rows: BookingRow[];
	tables: TableRow[];
	copies: (CopyRow & { title: string })[];
	onCancel: (id: string) => void;
	busy: boolean;
	showUser?: boolean;
}) {
	if (rows.length === 0) {
		return <p className="empty">Пусто.</p>;
	}
	const tableName = (id: string) => tables.find((t) => t.id === id)?.name ?? id.slice(0, 8);
	const copyName = (id: string) => {
		const copy = copies.find((c) => c.id === id);
		return copy ? `${copy.title} · ${copy.id.slice(0, 8)}` : id.slice(0, 8);
	};
	return (
		<table>
			<thead>
				<tr>
					{showUser ? <th>user</th> : null}
					<th>Стол</th>
					<th>Копия</th>
					<th>Слот</th>
					<th>Гостей</th>
					<th>Статус</th>
					<th></th>
				</tr>
			</thead>
			<tbody>
				{rows.map((row) => (
					<tr key={row.id}>
						{showUser ? <td className="mono">{row.userId.slice(0, 8)}</td> : null}
						<td>{tableName(row.tableId)}</td>
						<td>{copyName(row.gameCopyId)}</td>
						<td>
							{formatInstant(row.startAt)} — {formatInstant(row.endAt)}
						</td>
						<td>{row.guestCount}</td>
						<td>
							<span className={`chip ${row.status}`}>{row.status}</span>
						</td>
						<td>
							<button className="ghost" type="button" disabled={busy} onClick={() => onCancel(row.id)}>
								Отменить
							</button>
						</td>
					</tr>
				))}
			</tbody>
		</table>
	);
}
