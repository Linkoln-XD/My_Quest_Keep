import { createContext, useContext, useMemo, useState, type ReactNode } from "react";

import { login as apiLogin, register as apiRegister } from "../api/client";
import type { Role } from "../api/types";
import { clearSession, decodeAccess, readSession, writeSession } from "./session";

export type AuthUser = {
	email: string;
	role: Role;
	id: string;
};

type AuthContextValue = {
	user: AuthUser | null;
	login: (email: string, password: string) => Promise<void>;
	register: (email: string, password: string) => Promise<void>;
	logout: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

function userFromAccess(accessToken: string): AuthUser {
	const claims = decodeAccess(accessToken);
	return { email: claims.email, role: claims.role, id: claims.sub };
}

export function AuthProvider({ children }: { children: ReactNode }) {
	const [user, setUser] = useState<AuthUser | null>(() => {
		const session = readSession();
		if (!session) {
			return null;
		}
		try {
			return userFromAccess(session.accessToken);
		} catch {
			clearSession();
			return null;
		}
	});

	const value = useMemo<AuthContextValue>(
		() => ({
			user,
			async login(email, password) {
				const tokens = await apiLogin(email, password);
				writeSession({ accessToken: tokens.accessToken, refreshToken: tokens.refreshToken });
				setUser(userFromAccess(tokens.accessToken));
			},
			async register(email, password) {
				const tokens = await apiRegister(email, password);
				writeSession({ accessToken: tokens.accessToken, refreshToken: tokens.refreshToken });
				setUser(userFromAccess(tokens.accessToken));
			},
			logout() {
				clearSession();
				setUser(null);
			},
		}),
		[user],
	);

	return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
	const ctx = useContext(AuthContext);
	if (!ctx) {
		throw new Error("useAuth outside AuthProvider");
	}
	return ctx;
}
