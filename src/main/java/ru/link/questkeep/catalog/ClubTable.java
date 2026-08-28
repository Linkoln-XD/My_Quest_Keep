package ru.link.questkeep.catalog;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import ru.link.questkeep.shared.exception.DomainException;

@Entity
@Table(name = "club_tables")
public class ClubTable {

	@Id
	private UUID id;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(nullable = false)
	@JdbcTypeCode(SqlTypes.SMALLINT)
	private int capacity;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected ClubTable() {
	}

	private ClubTable(UUID id, String name, int capacity, Instant createdAt) {
		this.id = id;
		this.name = name;
		this.capacity = capacity;
		this.createdAt = createdAt;
		this.updatedAt = createdAt;
	}

	public static ClubTable create(String name, int capacity, Instant now) {
		if (name == null || name.isBlank()) {
			throw new DomainException("Table name is required");
		}
		if (capacity < 2 || capacity > 8) {
			throw new DomainException("Table capacity must be between 2 and 8");
		}
		if (now == null) {
			throw new DomainException("now is required");
		}
		return new ClubTable(UUID.randomUUID(), name.trim(), capacity, now);
	}

	public void update(String name, int capacity, Instant now) {
		if (isDeleted()) {
			throw new DomainException("Table is deleted");
		}
		if (name == null || name.isBlank()) {
			throw new DomainException("Table name is required");
		}
		if (capacity < 2 || capacity > 8) {
			throw new DomainException("Table capacity must be between 2 and 8");
		}
		this.name = name.trim();
		this.capacity = capacity;
		this.updatedAt = now;
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

	public String getName() {
		return name;
	}

	public int getCapacity() {
		return capacity;
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
