package ru.link.questkeep.booking.api;

import java.time.Instant;
import java.util.UUID;

import ru.link.questkeep.booking.Booking;
import ru.link.questkeep.booking.BookingStatus;

public record BookingResponse(
		UUID id,
		UUID tableId,
		UUID gameCopyId,
		UUID userId,
		Instant startAt,
		Instant endAt,
		int guestCount,
		BookingStatus status,
		Instant createdAt,
		Instant updatedAt) {

	public static BookingResponse from(Booking booking) {
		return new BookingResponse(
				booking.getId(),
				booking.getTable().getId(),
				booking.getGameCopy().getId(),
				booking.getUser().getId(),
				booking.getStartAt(),
				booking.getEndAt(),
				booking.getGuestCount(),
				booking.getStatus(),
				booking.getCreatedAt(),
				booking.getUpdatedAt());
	}
}
