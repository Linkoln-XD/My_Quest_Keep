import { Navigate, NavLink, Outlet } from "react-router-dom";

import { useAuth } from "../auth/AuthContext";

export function ClubLayout() {
	const { user, logout } = useAuth();
	if (!user) {
		return <Navigate to="/" replace />;
	}

	return (
		<div className="app-shell">
			<header className="masthead">
				<div>
					<h1 className="brand">
						Quest<span>Keep</span>
					</h1>
					<p>Стол и копия игры — одной бронью. Интервал [start, end).</p>
				</div>
				<div className="who">
					<strong>{user.email}</strong>
					{user.role === "STAFF" ? "персонал клуба" : "гость"}
				</div>
			</header>
			<nav className="nav">
				<NavLink to="/catalog">Каталог</NavLink>
				<NavLink to="/book">Брони</NavLink>
				<NavLink to="/waitlist">Лист ожидания</NavLink>
				<button type="button" className="linkish" onClick={logout}>
					Сменить человека
				</button>
			</nav>
			<Outlet />
		</div>
	);
}
