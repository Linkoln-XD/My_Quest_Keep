package ru.link.questkeep.catalog.api;

import java.time.Instant;
import java.util.UUID;

import ru.link.questkeep.catalog.ClubTable;

public record TableResponse(UUID id, String name, int capacity, Instant createdAt, Instant updatedAt) {

	public static TableResponse from(ClubTable table) {
		return new TableResponse(table.getId(), table.getName(), table.getCapacity(), table.getCreatedAt(), table.getUpdatedAt());
	}
}
