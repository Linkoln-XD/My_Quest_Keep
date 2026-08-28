package ru.link.questkeep.booking.api;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record JoinWaitlistRequest(
		UUID tableId,
		UUID gameCopyId,
		@NotNull Instant startAt,
		@NotNull Instant endAt) {
}
