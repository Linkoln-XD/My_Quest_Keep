package ru.link.questkeep.booking;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import ru.link.questkeep.catalog.ClubTable;
import ru.link.questkeep.catalog.GameCopy;
import ru.link.questkeep.identity.User;
import ru.link.questkeep.shared.exception.DomainException;

@Entity
@Table(name = "waitlist_entries")
public class WaitlistEntry {

	@Id
	private UUID id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "table_id")
	private ClubTable table;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "game_copy_id")
	private GameCopy gameCopy;

	@Column(name = "start_at", nullable = false)
	private Instant startAt;

	@Column(name = "end_at", nullable = false)
	private Instant endAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private WaitlistStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected WaitlistEntry() {
	}

	private WaitlistEntry(
			UUID id,
			User user,
			ClubTable table,
			GameCopy gameCopy,
			Instant startAt,
			Instant endAt,
			Instant createdAt) {
		this.id = id;
		this.user = user;
		this.table = table;
		this.gameCopy = gameCopy;
		this.startAt = startAt;
		this.endAt = endAt;
		this.status = WaitlistStatus.ACTIVE;
		this.createdAt = createdAt;
	}

	public static WaitlistEntry join(
			User user,
			ClubTable table,
			GameCopy gameCopy,
			Instant startAt,
			Instant endAt,
			Instant now) {
		if (user == null) {
			throw new DomainException("User is required");
		}
		if (table == null && gameCopy == null) {
			throw new DomainException("Waitlist entry must target a table and/or a game copy");
		}
		TimeSlotRules.validateInterval(startAt, endAt, now);
		return new WaitlistEntry(UUID.randomUUID(), user, table, gameCopy, startAt, endAt, now);
	}

	public UUID getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public ClubTable getTable() {
		return table;
	}

	public GameCopy getGameCopy() {
		return gameCopy;
	}

	public Instant getStartAt() {
		return startAt;
	}

	public Instant getEndAt() {
		return endAt;
	}

	public WaitlistStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
