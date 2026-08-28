import { FormEvent, useState } from "react";
import { Navigate } from "react-router-dom";

import { ApiError } from "../api/types";
import { useAuth } from "../auth/AuthContext";

export function GatePage() {
	const { user, login, register } = useAuth();
	const [error, setError] = useState<string | null>(null);
	const [busy, setBusy] = useState(false);

	if (user) {
		return <Navigate to="/catalog" replace />;
	}

	async function onLogin(event: FormEvent<HTMLFormElement>) {
		event.preventDefault();
		const data = new FormData(event.currentTarget);
		setBusy(true);
		setError(null);
		try {
			await login(String(data.get("email")), String(data.get("password")));
		} catch (err) {
			setError(err instanceof ApiError ? `${err.status}: ${err.detail}` : "Не удалось войти");
		} finally {
			setBusy(false);
		}
	}

	async function onRegister(event: FormEvent<HTMLFormElement>) {
		event.preventDefault();
		const data = new FormData(event.currentTarget);
		setBusy(true);
		setError(null);
		try {
			await register(String(data.get("email")), String(data.get("password")));
		} catch (err) {
			setError(err instanceof ApiError ? `${err.status}: ${err.detail}` : "Не удалось зарегистрироваться");
		} finally {
			setBusy(false);
		}
	}

	return (
		<div className="app-shell">
			<header className="masthead">
				<div>
					<h1 className="brand">
						Quest<span>Keep</span>
					</h1>
					<p>Демо клуба настолок. Сессия живёт во вкладке: два гостя — две вкладки.</p>
				</div>
			</header>

			<section className="panel">
				<h2>Как показать сценарий</h2>
				<ol className="steps">
					<li>Войдите как персонал и заведите стол, игру и копию.</li>
					<li>В этой вкладке зарегистрируйте гостя и бронь.</li>
					<li>Откройте вторую вкладку, зарегистрируйте другого гостя — пересечение даст 409.</li>
					<li>Второй гость встаёт в лист ожидания; персонал видит очередь.</li>
					<li>Первый гость отменяет бронь — второй может занять слот.</li>
				</ol>
				<p className="hint">
					Access JWT 15 минут; приложение само обновляет его refresh-токеном. Слот: кратно 30 минутам, 1–4 часа,
					только будущее. Заголовок Idempotency-Key ставится сам.
				</p>
			</section>

			{error ? <div className="flash">{error}</div> : null}

			<div className="grid-2">
				<section className="panel">
					<h2>Вход</h2>
					<p className="lede">STAFF уже посеян при старте API.</p>
					<form onSubmit={onLogin}>
						<label htmlFor="login-email">Email</label>
						<input id="login-email" name="email" type="email" required defaultValue="staff@questkeep.local" />
						<label htmlFor="login-password">Пароль</label>
						<input
							id="login-password"
							name="password"
							type="password"
							required
							minLength={8}
							defaultValue="ChangeMe_Staff_Demo_1"
						/>
						<div className="actions">
							<button className="primary" type="submit" disabled={busy}>
								Войти
							</button>
						</div>
					</form>
				</section>

				<section className="panel">
					<h2>Регистрация гостя</h2>
					<p className="lede">Публичный GUEST. Пароль минимум 8 символов.</p>
					<form onSubmit={onRegister}>
						<label htmlFor="reg-email">Email</label>
						<input id="reg-email" name="email" type="email" required placeholder="guest1@example.com" />
						<label htmlFor="reg-password">Пароль</label>
						<input id="reg-password" name="password" type="password" required minLength={8} defaultValue="password1" />
						<div className="actions">
							<button className="primary" type="submit" disabled={busy}>
								Создать гостя
							</button>
						</div>
					</form>
				</section>
			</div>
		</div>
	);
}
