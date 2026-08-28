package ru.link.questkeep.booking.api;

import java.time.Instant;
import java.util.UUID;

import ru.link.questkeep.booking.WaitlistEntry;
import ru.link.questkeep.booking.WaitlistStatus;

public record WaitlistResponse(
		UUID id,
		UUID userId,
		UUID tableId,
		UUID gameCopyId,
		Instant startAt,
		Instant endAt,
		WaitlistStatus status,
		Instant createdAt) {

	public static WaitlistResponse from(WaitlistEntry entry) {
		return new WaitlistResponse(
				entry.getId(),
				entry.getUser().getId(),
				entry.getTable() == null ? null : entry.getTable().getId(),
				entry.getGameCopy() == null ? null : entry.getGameCopy().getId(),
				entry.getStartAt(),
				entry.getEndAt(),
				entry.getStatus(),
				entry.getCreatedAt());
	}
}
