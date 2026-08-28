package ru.link.questkeep.catalog.api;

import java.time.Instant;
import java.util.UUID;

import ru.link.questkeep.catalog.Game;

public record GameResponse(UUID id, String title, Instant createdAt, Instant updatedAt) {

	public static GameResponse from(Game game) {
		return new GameResponse(game.getId(), game.getTitle(), game.getCreatedAt(), game.getUpdatedAt());
	}
}
