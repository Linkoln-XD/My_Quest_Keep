package ru.link.questkeep.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import ru.link.questkeep.AbstractPostgresTest;
import ru.link.questkeep.catalog.ClubTable;
import ru.link.questkeep.catalog.ClubTableRepository;
import ru.link.questkeep.catalog.Game;
import ru.link.questkeep.catalog.GameCopy;
import ru.link.questkeep.catalog.GameCopyRepository;
import ru.link.questkeep.catalog.GameRepository;
import ru.link.questkeep.identity.User;
import ru.link.questkeep.identity.UserRepository;
import ru.link.questkeep.shared.exception.DomainException;

@Transactional
class BookingPersistenceTest extends AbstractPostgresTest {

	@Autowired
	private UserRepository users;

	@Autowired
	private ClubTableRepository tables;

	@Autowired
	private GameRepository games;

	@Autowired
	private GameCopyRepository copies;

	@Autowired
	private BookingRepository bookings;

	@Test
	void overlappingActiveBookingsOnSameTableAreRejectedByDatabase() {
		Instant now = Instant.parse("2026-08-28T10:00:00Z");
		Instant start = Instant.parse("2026-08-28T12:00:00Z");
		Instant end = Instant.parse("2026-08-28T14:00:00Z");

		User guest = users.save(User.registerGuest("overlap-table@example.com", "hash-placeholder", now));
		ClubTable table = tables.save(ClubTable.create("Oak", 4, now));
		Game game = games.save(Game.create("Catan", now));
		GameCopy copyA = copies.save(GameCopy.create(game, now));
		GameCopy copyB = copies.save(GameCopy.create(game, now));

		bookings.saveAndFlush(Booking.confirmNew(table, copyA, guest, start, end, 2, "key-a", now));

		Booking overlapping = Booking.confirmNew(table, copyB, guest, start.plus(30, ChronoUnit.MINUTES), end.plus(30, ChronoUnit.MINUTES), 2, "key-b", now);

		assertThatThrownBy(() -> bookings.saveAndFlush(overlapping))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void overlappingActiveBookingsOnSameCopyAreRejectedByDatabase() {
		Instant now = Instant.parse("2026-08-28T10:00:00Z");
		Instant start = Instant.parse("2026-08-28T12:00:00Z");
		Instant end = Instant.parse("2026-08-28T14:00:00Z");

		User guest = users.save(User.registerGuest("overlap-copy@example.com", "hash-placeholder", now));
		ClubTable tableA = tables.save(ClubTable.create("Pine", 4, now));
		ClubTable tableB = tables.save(ClubTable.create("Maple", 4, now));
		Game game = games.save(Game.create("Ticket to Ride", now));
		GameCopy copy = copies.save(GameCopy.create(game, now));

		bookings.saveAndFlush(Booking.confirmNew(tableA, copy, guest, start, end, 2, "copy-key-a", now));

		Booking overlapping = Booking.confirmNew(tableB, copy, guest, start, end, 2, "copy-key-b", now);

		assertThatThrownBy(() -> bookings.saveAndFlush(overlapping))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void guestCountCannotExceedTableCapacity() {
		Instant now = Instant.parse("2026-08-28T10:00:00Z");
		ClubTable table = ClubTable.create("Small", 2, now);
		Game game = Game.create("Duo", now);
		GameCopy copy = GameCopy.create(game, now);
		User guest = User.registerGuest("capacity@example.com", "hash-placeholder", now);

		assertThatThrownBy(() -> Booking.confirmNew(
				table,
				copy,
				guest,
				Instant.parse("2026-08-28T12:00:00Z"),
				Instant.parse("2026-08-28T13:00:00Z"),
				3,
				null,
				now))
				.isInstanceOf(DomainException.class)
				.hasMessageContaining("capacity");
	}

	@Test
	void cancelIsIdempotent() {
		Instant now = Instant.parse("2026-08-28T10:00:00Z");
		Booking booking = persistConfirmed(now);
		booking.cancel(now);
		assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
		booking.cancel(now.plusSeconds(1));
		assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
	}

	@Test
	void adjacentHalfOpenSlotsOnSameTableAreAllowed() {
		Instant now = Instant.parse("2026-08-28T10:00:00Z");
		Instant noon = Instant.parse("2026-08-28T12:00:00Z");
		Instant two = Instant.parse("2026-08-28T14:00:00Z");
		Instant four = Instant.parse("2026-08-28T16:00:00Z");

		User guest = users.save(User.registerGuest("adjacent@example.com", "hash-placeholder", now));
		ClubTable table = tables.save(ClubTable.create("Walnut", 4, now));
		Game game = games.save(Game.create("Wingspan", now));
		GameCopy copyA = copies.save(GameCopy.create(game, now));
		GameCopy copyB = copies.save(GameCopy.create(game, now));

		bookings.saveAndFlush(Booking.confirmNew(table, copyA, guest, noon, two, 2, "adj-a", now));
		Booking next = Booking.confirmNew(table, copyB, guest, two, four, 2, "adj-b", now);
		assertThat(bookings.saveAndFlush(next).getId()).isNotNull();
	}

	private Booking persistConfirmed(Instant now) {
		User guest = users.save(User.registerGuest("cancel@example.com", "hash-placeholder", now));
		ClubTable table = tables.save(ClubTable.create("Cedar", 4, now));
		Game game = games.save(Game.create("Azul", now));
		GameCopy copy = copies.save(GameCopy.create(game, now));
		return bookings.saveAndFlush(Booking.confirmNew(
				table,
				copy,
				guest,
				Instant.parse("2026-08-28T12:00:00Z"),
				Instant.parse("2026-08-28T14:00:00Z"),
				2,
				"cancel-key",
				now));
	}
}
