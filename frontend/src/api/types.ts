export type Role = "GUEST" | "STAFF";

export type TokenResponse = {
	accessToken: string;
	refreshToken: string;
	tokenType: string;
};

export type PageResponse<T> = {
	content: T[];
	page: number;
	size: number;
	totalElements: number;
	totalPages: number;
};

export type TableRow = {
	id: string;
	name: string;
	capacity: number;
	createdAt: string;
	updatedAt: string;
};

export type GameRow = {
	id: string;
	title: string;
	createdAt: string;
	updatedAt: string;
};

export type CopyRow = {
	id: string;
	gameId: string;
	createdAt: string;
};

export type BookingStatus = "PENDING" | "CONFIRMED" | "CANCELLED" | "EXPIRED";

export type BookingRow = {
	id: string;
	tableId: string;
	gameCopyId: string;
	userId: string;
	startAt: string;
	endAt: string;
	guestCount: number;
	status: BookingStatus;
	createdAt: string;
	updatedAt: string;
};

export type WaitlistStatus = "ACTIVE" | "FULFILLED" | "CANCELLED";

export type WaitlistRow = {
	id: string;
	userId: string;
	tableId: string | null;
	gameCopyId: string | null;
	startAt: string;
	endAt: string;
	status: WaitlistStatus;
	createdAt: string;
};

export class ApiError extends Error {
	readonly status: number;
	readonly detail: string;

	constructor(status: number, detail: string) {
		super(detail);
		this.status = status;
		this.detail = detail;
	}
}
