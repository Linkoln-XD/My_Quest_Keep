export type SessionTokens = {
	accessToken: string;
	refreshToken: string;
};

const KEY = "questkeep.session";

export function readSession(): SessionTokens | null {
	const raw = sessionStorage.getItem(KEY);
	if (!raw) {
		return null;
	}
	try {
		const parsed = JSON.parse(raw) as SessionTokens;
		if (parsed.accessToken && parsed.refreshToken) {
			return parsed;
		}
	} catch {
		/* ignore */
	}
	return null;
}

export function writeSession(tokens: SessionTokens): void {
	sessionStorage.setItem(KEY, JSON.stringify(tokens));
}

export function clearSession(): void {
	sessionStorage.removeItem(KEY);
}

export function decodeAccess(accessToken: string): { email: string; role: "GUEST" | "STAFF"; sub: string } {
	const payload = accessToken.split(".")[1];
	if (!payload) {
		throw new Error("Invalid token");
	}
	const json = JSON.parse(atob(payload.replace(/-/g, "+").replace(/_/g, "/"))) as {
		email?: string;
		role?: string;
		sub?: string;
	};
	if (!json.email || (json.role !== "GUEST" && json.role !== "STAFF") || !json.sub) {
		throw new Error("Invalid token claims");
	}
	return { email: json.email, role: json.role, sub: json.sub };
}
