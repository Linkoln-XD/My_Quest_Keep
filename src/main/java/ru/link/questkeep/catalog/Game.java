package ru.link.questkeep.catalog;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import ru.link.questkeep.shared.exception.DomainException;

@Entity
@Table(name = "games")
public class Game {

	@Id
	private UUID id;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Game() {
	}

	private Game(UUID id, String title, Instant createdAt) {
		this.id = id;
		this.title = title;
		this.createdAt = createdAt;
		this.updatedAt = createdAt;
	}

	public static Game create(String title, Instant now) {
		if (title == null || title.isBlank()) {
			throw new DomainException("Game title is required");
		}
		if (now == null) {
			throw new DomainException("now is required");
		}
		return new Game(UUID.randomUUID(), title.trim(), now);
	}

	public void markDeleted(Instant now) {
		if (deletedAt == null) {
			deletedAt = now;
			updatedAt = now;
		}
	}

	public boolean isDeleted() {
		return deletedAt != null;
	}

	public UUID getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public Instant getDeletedAt() {
		return deletedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
