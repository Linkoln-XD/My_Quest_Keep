package ru.link.questkeep.catalog;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import ru.link.questkeep.shared.exception.DomainException;

@Entity
@Table(name = "game_copies")
public class GameCopy {

	@Id
	private UUID id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "game_id", nullable = false)
	private Game game;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected GameCopy() {
	}

	private GameCopy(UUID id, Game game, Instant createdAt) {
		this.id = id;
		this.game = game;
		this.createdAt = createdAt;
		this.updatedAt = createdAt;
	}

	public static GameCopy create(Game game, Instant now) {
		if (game == null) {
			throw new DomainException("Game is required");
		}
		if (game.isDeleted()) {
			throw new DomainException("Cannot add a copy of a deleted game");
		}
		if (now == null) {
			throw new DomainException("now is required");
		}
		return new GameCopy(UUID.randomUUID(), game, now);
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

	public Game getGame() {
		return game;
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
