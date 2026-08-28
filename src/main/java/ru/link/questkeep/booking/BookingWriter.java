package ru.link.questkeep.booking;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ru.link.questkeep.catalog.ClubTable;
import ru.link.questkeep.catalog.ClubTableRepository;
import ru.link.questkeep.catalog.GameCopy;
import ru.link.questkeep.catalog.GameCopyRepository;
import ru.link.questkeep.identity.User;
import ru.link.questkeep.identity.UserRepository;
import ru.link.questkeep.shared.exception.DomainException;

@Component
class BookingWriter {

	private final Clock clock;
	private final UserRepository users;
	private final ClubTableRepository tables;
	private final GameCopyRepository copies;
	private final BookingRepository bookings;

	BookingWriter(
			Clock clock,
			UserRepository users,
			ClubTableRepository tables,
			GameCopyRepository copies,
			BookingRepository bookings) {
		this.clock = clock;
		this.users = users;
		this.tables = tables;
		this.copies = copies;
		this.bookings = bookings;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Booking insertNew(
			UUID userId,
			UUID tableId,
			UUID gameCopyId,
			Instant startAt,
			Instant endAt,
			int guestCount,
			String idempotencyKey) {
		Instant now = Instant.now(clock);
		User guest = users.findById(userId)
				.orElseThrow(() -> new DomainException("User not found"));
		ClubTable table = tables.findById(tableId)
				.orElseThrow(() -> new DomainException("Table not found"));
		GameCopy copy = copies.findById(gameCopyId)
				.orElseThrow(() -> new DomainException("Game copy not found"));
		copy.getGame().getTitle();
		Booking booking = Booking.confirmNew(
				table, copy, guest, startAt, endAt, guestCount, idempotencyKey, now);
		return bookings.saveAndFlush(booking);
	}
}
