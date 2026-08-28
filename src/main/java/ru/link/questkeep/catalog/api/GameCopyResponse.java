package ru.link.questkeep.catalog.api;

import java.time.Instant;
import java.util.UUID;

import ru.link.questkeep.catalog.GameCopy;

public record GameCopyResponse(UUID id, UUID gameId, Instant createdAt) {

	public static GameCopyResponse from(GameCopy copy) {
		return new GameCopyResponse(copy.getId(), copy.getGame().getId(), copy.getCreatedAt());
	}
}
