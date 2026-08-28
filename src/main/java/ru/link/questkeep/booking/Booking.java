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

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import ru.link.questkeep.catalog.ClubTable;
import ru.link.questkeep.catalog.GameCopy;
import ru.link.questkeep.identity.User;
import ru.link.questkeep.shared.exception.DomainException;

@Entity
@Table(name = "bookings")
public class Booking {

	@Id
	private UUID id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "table_id", nullable = false)
	private ClubTable table;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "game_copy_id", nullable = false)
	private GameCopy gameCopy;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "start_at", nullable = false)
	private Instant startAt;

	@Column(name = "end_at", nullable = false)
	private Instant endAt;

	@Column(name = "guest_count", nullable = false)
	@JdbcTypeCode(SqlTypes.SMALLINT)
	private int guestCount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private BookingStatus status;

	@Column(name = "idempotency_key", length = 128)
	private String idempotencyKey;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by", nullable = false)
	private User createdBy;

	protected Booking() {
	}

	private Booking(
			UUID id,
			ClubTable table,
			GameCopy gameCopy,
			User user,
			Instant startAt,
			Instant endAt,
			int guestCount,
			BookingStatus status,
			String idempotencyKey,
			Instant createdAt,
			User createdBy) {
		this.id = id;
		this.table = table;
		this.gameCopy = gameCopy;
		this.user = user;
		this.startAt = startAt;
		this.endAt = endAt;
		this.guestCount = guestCount;
		this.status = status;
		this.idempotencyKey = idempotencyKey;
		this.createdAt = createdAt;
		this.updatedAt = createdAt;
		this.createdBy = createdBy;
	}

	public static Booking confirmNew(
			ClubTable table,
			GameCopy gameCopy,
			User guest,
			Instant startAt,
			Instant endAt,
			int guestCount,
			String idempotencyKey,
			Instant now) {
		if (table == null || gameCopy == null || guest == null) {
			throw new DomainException("Table, game copy and guest are required");
		}
		if (table.isDeleted()) {
			throw new DomainException("Table is deleted");
		}
		if (gameCopy.isDeleted()) {
			throw new DomainException("Game copy is deleted");
		}
		if (gameCopy.getGame().isDeleted()) {
			throw new DomainException("Game is deleted");
		}
		TimeSlotRules.validateInterval(startAt, endAt, now);
		if (guestCount < 1) {
			throw new DomainException("Guest count must be at least 1");
		}
		if (guestCount > table.getCapacity()) {
			throw new DomainException("Guest count exceeds table capacity");
		}
		String key = normalizeIdempotencyKey(idempotencyKey);
		return new Booking(
				UUID.randomUUID(),
				table,
				gameCopy,
				guest,
				startAt,
				endAt,
				guestCount,
				BookingStatus.CONFIRMED,
				key,
				now,
				guest);
	}

	private static String normalizeIdempotencyKey(String idempotencyKey) {
		if (idempotencyKey == null || idempotencyKey.isBlank()) {
			return null;
		}
		String trimmed = idempotencyKey.trim();
		if (trimmed.length() > 128) {
			throw new DomainException("Idempotency-Key is too long");
		}
		return trimmed;
	}

	public void cancel(Instant now) {
		if (status == BookingStatus.CANCELLED) {
			return;
		}
		if (status == BookingStatus.EXPIRED) {
			throw new DomainException("Cannot cancel an expired booking");
		}
		status = BookingStatus.CANCELLED;
		updatedAt = now;
	}

	/**
	 * Lazy expiry on read: occupancy is [start, end), so the booking is expired when now &gt;= end.
	 */
	public boolean expireIfEnded(Instant now) {
		if (status.occupiesSlot() && !now.isBefore(endAt)) {
			status = BookingStatus.EXPIRED;
			updatedAt = now;
			return true;
		}
		return false;
	}

	public UUID getId() {
		return id;
	}

	public ClubTable getTable() {
		return table;
	}

	public GameCopy getGameCopy() {
		return gameCopy;
	}

	public User getUser() {
		return user;
	}

	public Instant getStartAt() {
		return startAt;
	}

	public Instant getEndAt() {
		return endAt;
	}

	public int getGuestCount() {
		return guestCount;
	}

	public BookingStatus getStatus() {
		return status;
	}

	public String getIdempotencyKey() {
		return idempotencyKey;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public User getCreatedBy() {
		return createdBy;
	}
}
