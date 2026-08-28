import { ApiError, type PageResponse, type TokenResponse } from "./types";
import { clearSession, readSession, writeSession } from "../auth/session";

type Json = Record<string, unknown> | undefined;

let refreshInFlight: Promise<boolean> | null = null;

function problemDetail(body: unknown, fallback: string): string {
	if (body && typeof body === "object" && "detail" in body && typeof body.detail === "string") {
		return body.detail;
	}
	if (body && typeof body === "object" && "title" in body && typeof body.title === "string") {
		return body.title;
	}
	return fallback;
}

async function parseBody(response: Response): Promise<unknown> {
	if (response.status === 204) {
		return undefined;
	}
	const text = await response.text();
	if (!text) {
		return undefined;
	}
	try {
		return JSON.parse(text);
	} catch {
		return text;
	}
}

async function rawFetch(
	path: string,
	init: RequestInit,
	accessToken?: string,
	idempotencyKey?: string,
): Promise<Response> {
	const headers = new Headers(init.headers);
	if (init.body && !headers.has("Content-Type")) {
		headers.set("Content-Type", "application/json");
	}
	if (accessToken) {
		headers.set("Authorization", `Bearer ${accessToken}`);
	}
	if (idempotencyKey) {
		headers.set("Idempotency-Key", idempotencyKey);
	}
	return fetch(path, { ...init, headers });
}

async function tryRefresh(): Promise<boolean> {
	if (refreshInFlight) {
		return refreshInFlight;
	}
	refreshInFlight = (async () => {
		const session = readSession();
		if (!session) {
			return false;
		}
		const response = await rawFetch("/api/v1/auth/refresh", {
			method: "POST",
			body: JSON.stringify({ refreshToken: session.refreshToken }),
		});
		const body = await parseBody(response);
		if (!response.ok) {
			clearSession();
			return false;
		}
		const tokens = body as TokenResponse;
		writeSession({ accessToken: tokens.accessToken, refreshToken: tokens.refreshToken });
		return true;
	})();
	try {
		return await refreshInFlight;
	} finally {
		refreshInFlight = null;
	}
}

export async function api<T>(
	path: string,
	init: RequestInit = {},
	options: { auth?: boolean; idempotencyKey?: string } = {},
): Promise<T> {
	const auth = options.auth !== false;
	let session = auth ? readSession() : null;
	let response = await rawFetch(path, init, session?.accessToken, options.idempotencyKey);
	if (response.status === 401 && auth && session) {
		const ok = await tryRefresh();
		if (ok) {
			session = readSession();
			response = await rawFetch(path, init, session?.accessToken, options.idempotencyKey);
		}
	}
	const body = await parseBody(response);
	if (!response.ok) {
		throw new ApiError(response.status, problemDetail(body, `HTTP ${response.status}`));
	}
	return body as T;
}

export async function login(email: string, password: string): Promise<TokenResponse> {
	return api<TokenResponse>(
		"/api/v1/auth/login",
		{ method: "POST", body: JSON.stringify({ email, password }) },
		{ auth: false },
	);
}

export async function register(email: string, password: string): Promise<TokenResponse> {
	return api<TokenResponse>(
		"/api/v1/auth/register",
		{ method: "POST", body: JSON.stringify({ email, password }) },
		{ auth: false },
	);
}

export function pageQuery(page = 0, size = 50): string {
	return `page=${page}&size=${size}`;
}

export async function getPage<T>(path: string): Promise<PageResponse<T>> {
	return api<PageResponse<T>>(path);
}

export async function sendJson<T>(path: string, method: string, json?: Json, idempotencyKey?: string): Promise<T> {
	return api<T>(
		path,
		{ method, body: json === undefined ? undefined : JSON.stringify(json) },
		{ idempotencyKey },
	);
}

export async function sendEmpty(path: string, method: string): Promise<void> {
	await api<undefined>(path, { method });
}
