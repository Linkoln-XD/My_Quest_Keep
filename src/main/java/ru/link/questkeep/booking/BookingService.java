package ru.link.questkeep.booking;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.link.questkeep.shared.exception.BookingConflictException;
import ru.link.questkeep.shared.exception.DomainException;
import ru.link.questkeep.shared.exception.ForbiddenException;
import ru.link.questkeep.shared.exception.ResourceNotFoundException;
import ru.link.questkeep.shared.persistence.PostgresSqlStates;

@Service
public class BookingService {

	private final Clock clock;
	private final BookingRepository bookings;
	private final BookingWriter writer;

	public BookingService(Clock clock, BookingRepository bookings, BookingWriter writer) {
		this.clock = clock;
		this.bookings = bookings;
		this.writer = writer;
	}

	public Booking create(
			UUID userId,
			UUID tableId,
			UUID gameCopyId,
			Instant startAt,
			Instant endAt,
			int guestCount,
			String idempotencyKey) {
		String key = requireIdempotencyKey(idempotencyKey);
		return bookings.findByUserIdAndIdempotencyKey(userId, key)
				.map(existing -> replayOrReject(existing, tableId, gameCopyId, startAt, endAt, guestCount))
				.orElseGet(() -> insertHandlingConflicts(
						userId, tableId, gameCopyId, startAt, endAt, guestCount, key));
	}

	private Booking insertHandlingConflicts(
			UUID userId,
			UUID tableId,
			UUID gameCopyId,
			Instant startAt,
			Instant endAt,
			int guestCount,
			String key) {
		RuntimeException lastDeadlock = null;
		for (int attempt = 1; attempt <= 5; attempt++) {
			try {
				return writer.insertNew(userId, tableId, gameCopyId, startAt, endAt, guestCount, key);
			}
			catch (RuntimeException ex) {
				if (PostgresSqlStates.isExclusionViolation(ex)) {
					throw new BookingConflictException();
				}
				if (PostgresSqlStates.isUniqueViolation(ex)) {
					return bookings.findByUserIdAndIdempotencyKey(userId, key)
							.map(existing -> replayOrReject(existing, tableId, gameCopyId, startAt, endAt, guestCount))
							.orElseThrow(() -> ex);
				}
				if (PostgresSqlStates.isDeadlock(ex)) {
					lastDeadlock = ex;
					if (attempt < 5) {
						pauseBeforeRetry(attempt);
						continue;
					}
					throw new BookingConflictException();
				}
				throw ex;
			}
		}
		throw lastDeadlock;
	}

	private static void pauseBeforeRetry(int attempt) {
		try {
			Thread.sleep(20L * attempt);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while retrying booking insert", ex);
		}
	}

	private Booking replayOrReject(
			Booking existing,
			UUID tableId,
			UUID gameCopyId,
			Instant startAt,
			Instant endAt,
			int guestCount) {
		boolean sameRequest = existing.getTable().getId().equals(tableId)
				&& existing.getGameCopy().getId().equals(gameCopyId)
				&& existing.getStartAt().equals(startAt)
				&& existing.getEndAt().equals(endAt)
				&& existing.getGuestCount() == guestCount;
		if (!sameRequest) {
			throw new DomainException("Idempotency-Key was reused with a different request");
		}
		return applyExpiry(existing);
	}

	@Transactional
	public Booking get(UUID bookingId) {
		Booking booking = bookings.findFetchedById(bookingId)
				.orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
		return applyExpiry(booking);
	}

	@Transactional
	public Page<Booking> listForUser(UUID userId, Pageable pageable) {
		Page<Booking> page = bookings.findByUserId(userId, pageable);
		page.forEach(this::applyExpiry);
		return page;
	}

	@Transactional
	public Page<Booking> listAll(Pageable pageable) {
		Page<Booking> page = bookings.findAllFetched(pageable);
		page.forEach(this::applyExpiry);
		return page;
	}

	@Transactional
	public Booking cancel(UUID bookingId, UUID actorId, boolean staff) {
		Booking booking = get(bookingId);
		if (!staff && !booking.getUser().getId().equals(actorId)) {
			throw new ForbiddenException("Cannot cancel another user's booking");
		}
		booking.cancel(Instant.now(clock));
		return bookings.save(booking);
	}

	private Booking applyExpiry(Booking booking) {
		if (booking.expireIfEnded(Instant.now(clock))) {
			return bookings.save(booking);
		}
		return booking;
	}

	private static String requireIdempotencyKey(String idempotencyKey) {
		if (idempotencyKey == null || idempotencyKey.isBlank()) {
			throw new DomainException("Idempotency-Key is required");
		}
		String trimmed = idempotencyKey.trim();
		if (trimmed.length() > 128) {
			throw new DomainException("Idempotency-Key is too long");
		}
		return trimmed;
	}
}
