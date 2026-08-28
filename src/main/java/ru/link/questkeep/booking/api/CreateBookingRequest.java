package ru.link.questkeep.booking.api;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateBookingRequest(
		@NotNull UUID tableId,
		@NotNull UUID gameCopyId,
		@NotNull Instant startAt,
		@NotNull Instant endAt,
		@Min(1) @Max(8) int guestCount) {
}
