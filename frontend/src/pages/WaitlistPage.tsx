import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";

import { getPage, pageQuery, sendJson } from "../api/client";
import { ApiError, type CopyRow, type GameRow, type TableRow, type WaitlistRow } from "../api/types";
import { useAuth } from "../auth/AuthContext";
import { defaultSlot, formatInstant, fromDatetimeLocal } from "../time";

export function WaitlistPage() {
	const { user } = useAuth();
	const staff = user?.role === "STAFF";
	const slot = useMemo(() => defaultSlot(), []);
	const [tables, setTables] = useState<TableRow[]>([]);
	const [copies, setCopies] = useState<(CopyRow & { title: string })[]>([]);
	const [mine, setMine] = useState<WaitlistRow[]>([]);
	const [club, setClub] = useState<WaitlistRow[] | null>(null);
	const [error, setError] = useState<string | null>(null);
	const [ok, setOk] = useState<string | null>(null);
	const [busy, setBusy] = useState(false);

	const reload = useCallback(async () => {
		const [t, g, my] = await Promise.all([
			getPage<TableRow>(`/api/v1/tables?${pageQuery()}`),
			getPage<GameRow>(`/api/v1/games?${pageQuery()}`),
			getPage<WaitlistRow>(`/api/v1/waitlist/me?${pageQuery()}`),
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
			const all = await getPage<WaitlistRow>(`/api/v1/waitlist?${pageQuery()}`);
			setClub(all.content);
		} else {
			setClub(null);
		}
	}, [staff]);

	useEffect(() => {
		reload().catch((err) => setError(err instanceof ApiError ? err.detail : "Не удалось загрузить лист"));
	}, [reload]);

	function fail(err: unknown) {
		setOk(null);
		setError(err instanceof ApiError ? `${err.status}: ${err.detail}` : "Ошибка запроса");
	}

	async function onJoin(event: FormEvent<HTMLFormElement>) {
		event.preventDefault();
		const data = new FormData(event.currentTarget);
		const tableId = String(data.get("tableId") || "");
		const gameCopyId = String(data.get("gameCopyId") || "");
		if (!tableId && !gameCopyId) {
			setError("Укажите стол и/или копию");
			return;
		}
		setBusy(true);
		try {
			const row = await sendJson<WaitlistRow>("/api/v1/waitlist", "POST", {
				tableId: tableId || null,
				gameCopyId: gameCopyId || null,
				startAt: fromDatetimeLocal(String(data.get("startAt"))),
				endAt: fromDatetimeLocal(String(data.get("endAt"))),
			});
			setOk(`Запись ${row.status}. Повтор с тем же слотом вернёт тот же id.`);
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
			const row = await sendJson<WaitlistRow>(`/api/v1/waitlist/${id}/cancel`, "POST");
			setOk(`Статус: ${row.status}`);
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
				<h2>Встать в лист</h2>
				<p className="lede">Нужен стол и/или копия. Писем нет: персонал смотрит очередь сам. Гость не видит чужой список (403).</p>
				<form onSubmit={onJoin}>
					<div className="row">
						<div>
							<label htmlFor="wl-table">Стол (необязательно)</label>
							<select id="wl-table" name="tableId" defaultValue="">
								<option value="">—</option>
								{tables.map((table) => (
									<option key={table.id} value={table.id}>
										{table.name}
									</option>
								))}
							</select>
						</div>
						<div>
							<label htmlFor="wl-copy">Копия (необязательно)</label>
							<select id="wl-copy" name="gameCopyId" defaultValue="">
								<option value="">—</option>
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
							<label htmlFor="wl-start">Начало</label>
							<input id="wl-start" name="startAt" type="datetime-local" required defaultValue={slot.startLocal} />
						</div>
						<div>
							<label htmlFor="wl-end">Конец</label>
							<input id="wl-end" name="endAt" type="datetime-local" required defaultValue={slot.endLocal} />
						</div>
					</div>
					<div className="actions">
						<button className="primary" type="submit" disabled={busy}>
							В лист
						</button>
					</div>
				</form>
			</section>

			<section className="panel">
				<h2>Мои записи</h2>
				<WaitTable rows={mine} tables={tables} copies={copies} onCancel={cancel} busy={busy} />
			</section>

			{staff && club ? (
				<section className="panel">
					<h2>Активная очередь клуба</h2>
					<p className="lede">Только ACTIVE, сначала старые.</p>
					<WaitTable rows={club} tables={tables} copies={copies} onCancel={cancel} busy={busy} showUser />
				</section>
			) : null}
		</>
	);
}

function WaitTable({
	rows,
	tables,
	copies,
	onCancel,
	busy,
	showUser = false,
}: {
	rows: WaitlistRow[];
	tables: TableRow[];
	copies: (CopyRow & { title: string })[];
	onCancel: (id: string) => void;
	busy: boolean;
	showUser?: boolean;
}) {
	if (rows.length === 0) {
		return <p className="empty">Пусто.</p>;
	}
	return (
		<table>
			<thead>
				<tr>
					{showUser ? <th>user</th> : null}
					<th>Стол</th>
					<th>Копия</th>
					<th>Слот</th>
					<th>Статус</th>
					<th></th>
				</tr>
			</thead>
			<tbody>
				{rows.map((row) => (
					<tr key={row.id}>
						{showUser ? <td className="mono">{row.userId.slice(0, 8)}</td> : null}
						<td>{row.tableId ? tables.find((t) => t.id === row.tableId)?.name ?? row.tableId.slice(0, 8) : "—"}</td>
						<td>
							{row.gameCopyId
								? copies.find((c) => c.id === row.gameCopyId)?.title ?? row.gameCopyId.slice(0, 8)
								: "—"}
						</td>
						<td>
							{formatInstant(row.startAt)} — {formatInstant(row.endAt)}
						</td>
						<td>
							<span className={`chip ${row.status}`}>{row.status}</span>
						</td>
						<td>
							<button className="ghost" type="button" disabled={busy} onClick={() => onCancel(row.id)}>
								Снять
							</button>
						</td>
					</tr>
				))}
			</tbody>
		</table>
	);
}
