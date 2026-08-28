package ru.link.questkeep.booking;

public enum BookingStatus {
	PENDING,
	CONFIRMED,
	CANCELLED,
	EXPIRED;

	public boolean occupiesSlot() {
		return this == PENDING || this == CONFIRMED;
	}
}
