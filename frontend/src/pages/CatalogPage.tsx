import { FormEvent, useCallback, useEffect, useState } from "react";

import { getPage, pageQuery, sendEmpty, sendJson } from "../api/client";
import { ApiError, type CopyRow, type GameRow, type TableRow } from "../api/types";
import { useAuth } from "../auth/AuthContext";

type CopyWithGame = CopyRow & { title: string };

export function CatalogPage() {
	const { user } = useAuth();
	const staff = user?.role === "STAFF";
	const [tables, setTables] = useState<TableRow[]>([]);
	const [games, setGames] = useState<GameRow[]>([]);
	const [copies, setCopies] = useState<CopyWithGame[]>([]);
	const [error, setError] = useState<string | null>(null);
	const [ok, setOk] = useState<string | null>(null);
	const [busy, setBusy] = useState(false);

	const reload = useCallback(async () => {
		const [t, g] = await Promise.all([
			getPage<TableRow>(`/api/v1/tables?${pageQuery()}`),
			getPage<GameRow>(`/api/v1/games?${pageQuery()}`),
		]);
		setTables(t.content);
		setGames(g.content);
		const nested = await Promise.all(
			g.content.map(async (game) => {
				const page = await getPage<CopyRow>(`/api/v1/games/${game.id}/copies?${pageQuery()}`);
				return page.content.map((copy) => ({ ...copy, title: game.title }));
			}),
		);
		setCopies(nested.flat());
	}, []);

	useEffect(() => {
		reload().catch((err) => setError(err instanceof ApiError ? err.detail : "Не удалось загрузить каталог"));
	}, [reload]);

	function flashOk(message: string) {
		setOk(message);
		setError(null);
	}

	function fail(err: unknown) {
		setOk(null);
		setError(err instanceof ApiError ? `${err.status}: ${err.detail}` : "Ошибка запроса");
	}

	async function addTable(event: FormEvent<HTMLFormElement>) {
		event.preventDefault();
		const form = event.currentTarget;
		const data = new FormData(form);
		setBusy(true);
		try {
			await sendJson("/api/v1/tables", "POST", {
				name: String(data.get("name")),
				capacity: Number(data.get("capacity")),
			});
			form.reset();
			flashOk("Стол создан");
			await reload();
		} catch (err) {
			fail(err);
		} finally {
			setBusy(false);
		}
	}

	async function addGame(event: FormEvent<HTMLFormElement>) {
		event.preventDefault();
		const form = event.currentTarget;
		const data = new FormData(form);
		setBusy(true);
		try {
			await sendJson("/api/v1/games", "POST", { title: String(data.get("title")) });
			form.reset();
			flashOk("Игра создана — добавьте копию");
			await reload();
		} catch (err) {
			fail(err);
		} finally {
			setBusy(false);
		}
	}

	async function addCopy(gameId: string) {
		setBusy(true);
		try {
			await sendJson(`/api/v1/games/${gameId}/copies`, "POST");
			flashOk("Копия добавлена в шкаф");
			await reload();
		} catch (err) {
			fail(err);
		} finally {
			setBusy(false);
		}
	}

	async function seedDemo() {
		setBusy(true);
		try {
			const table = await sendJson<TableRow>("/api/v1/tables", "POST", { name: "Oak", capacity: 4 });
			const game = await sendJson<GameRow>("/api/v1/games", "POST", { title: "Catan" });
			const copy = await sendJson<CopyRow>(`/api/v1/games/${game.id}/copies`, "POST");
			flashOk(`Демо: стол ${table.name}, игра ${game.title}, копия ${copy.id.slice(0, 8)}…`);
			await reload();
		} catch (err) {
			fail(err);
		} finally {
			setBusy(false);
		}
	}

	async function remove(kind: "tables" | "games" | "game-copies", id: string) {
		setBusy(true);
		try {
			await sendEmpty(`/api/v1/${kind}/${id}`, "DELETE");
			flashOk("Мягкое удаление выполнено");
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

			{staff ? (
				<section className="panel">
					<h2>Стойка персонала</h2>
					<p className="lede">Гость этот блок не видит. Удаление запрещено, пока есть активная бронь.</p>
					<div className="actions">
						<button className="primary" type="button" disabled={busy} onClick={seedDemo}>
							Завести Oak + Catan + копию
						</button>
					</div>
					<div className="grid-2">
						<form onSubmit={addTable}>
							<label htmlFor="table-name">Стол</label>
							<input id="table-name" name="name" required placeholder="Oak" />
							<label htmlFor="table-cap">Вместимость 2–8</label>
							<input id="table-cap" name="capacity" type="number" min={2} max={8} defaultValue={4} required />
							<div className="actions">
								<button className="primary" type="submit" disabled={busy}>
									Добавить стол
								</button>
							</div>
						</form>
						<form onSubmit={addGame}>
							<label htmlFor="game-title">Игра</label>
							<input id="game-title" name="title" required placeholder="Catan" />
							<div className="actions">
								<button className="primary" type="submit" disabled={busy}>
									Добавить игру
								</button>
							</div>
						</form>
					</div>
				</section>
			) : (
				<section className="panel">
					<h2>Каталог</h2>
					<p className="lede">Только просмотр. Стол и копию выбираете на странице «Брони».</p>
				</section>
			)}

			<section className="panel">
				<h2>Столы</h2>
				{tables.length === 0 ? (
					<p className="empty">Пока пусто — персонал должен завести мебель.</p>
				) : (
					<table>
						<thead>
							<tr>
								<th>Имя</th>
								<th>Мест</th>
								{staff ? <th></th> : null}
							</tr>
						</thead>
						<tbody>
							{tables.map((table) => (
								<tr key={table.id}>
									<td>{table.name}</td>
									<td>{table.capacity}</td>
									{staff ? (
										<td>
											<button className="danger" type="button" disabled={busy} onClick={() => remove("tables", table.id)}>
												Скрыть
											</button>
										</td>
									) : null}
								</tr>
							))}
						</tbody>
					</table>
				)}
			</section>

			<section className="panel">
				<h2>Игры и копии</h2>
				{games.length === 0 ? (
					<p className="empty">Игр нет.</p>
				) : (
					<table>
						<thead>
							<tr>
								<th>Игра</th>
								<th>Копии</th>
								{staff ? <th></th> : null}
							</tr>
						</thead>
						<tbody>
							{games.map((game) => {
								const gameCopies = copies.filter((c) => c.gameId === game.id);
								return (
									<tr key={game.id}>
										<td>{game.title}</td>
										<td>
											{gameCopies.length === 0
												? "нет"
												: gameCopies.map((c) => (
														<span key={c.id}>
															<code className="mono">{c.id.slice(0, 8)}</code>
															{staff ? (
																<>
																	{" "}
																	<button
																		className="ghost"
																		type="button"
																		disabled={busy}
																		onClick={() => remove("game-copies", c.id)}
																	>
																		убрать
																	</button>
																</>
															) : null}{" "}
														</span>
													))}
										</td>
										{staff ? (
											<td>
												<button className="ghost" type="button" disabled={busy} onClick={() => addCopy(game.id)}>
													+ копия
												</button>
												<button className="danger" type="button" disabled={busy} onClick={() => remove("games", game.id)}>
													Скрыть игру
												</button>
											</td>
										) : null}
									</tr>
								);
							})}
						</tbody>
					</table>
				)}
			</section>
		</>
	);
}
