package ru.link.questkeep.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.link.questkeep.AbstractPostgresTest;
import ru.link.questkeep.booking.BookingService;
import ru.link.questkeep.identity.User;
import ru.link.questkeep.identity.UserRepository;
import ru.link.questkeep.shared.exception.DomainException;

class CatalogServiceTest extends AbstractPostgresTest {

	private static final Instant START = Instant.parse("2026-08-28T12:00:00Z");
	private static final Instant END = Instant.parse("2026-08-28T14:00:00Z");

	@Autowired
	private CatalogService catalog;

	@Autowired
	private BookingService bookings;

	@Autowired
	private UserRepository users;

	@Autowired
	private ClubTableRepository tables;

	@Autowired
	private GameRepository games;

	@Autowired
	private GameCopyRepository copies;

	@Test
	void cannotSoftDeleteTableWithActiveBooking() {
		User guest = users.save(User.registerGuest("del-table-" + UUID.randomUUID() + "@example.com", "hash-placeholder", TEST_NOW));
		ClubTable table = tables.save(ClubTable.create("Busy " + UUID.randomUUID(), 4, TEST_NOW));
		Game game = games.save(Game.create("Busy game " + UUID.randomUUID(), TEST_NOW));
		GameCopy copy = copies.save(GameCopy.create(game, TEST_NOW));
		bookings.create(guest.getId(), table.getId(), copy.getId(), START, END, 2, "del-table");

		assertThatThrownBy(() -> catalog.deleteTable(table.getId()))
				.isInstanceOf(DomainException.class)
				.hasMessageContaining("active booking");
	}

	@Test
	void canSoftDeleteTableAfterCancel() {
		User guest = users.save(User.registerGuest("del-ok-" + UUID.randomUUID() + "@example.com", "hash-placeholder", TEST_NOW));
		ClubTable table = tables.save(ClubTable.create("Free " + UUID.randomUUID(), 4, TEST_NOW));
		Game game = games.save(Game.create("Free game " + UUID.randomUUID(), TEST_NOW));
		GameCopy copy = copies.save(GameCopy.create(game, TEST_NOW));
		var booking = bookings.create(guest.getId(), table.getId(), copy.getId(), START, END, 2, "del-ok");
		bookings.cancel(booking.getId(), guest.getId(), false);

		ClubTable deleted = catalog.deleteTable(table.getId());
		assertThat(deleted.isDeleted()).isTrue();
	}

	@Test
	void cannotSoftDeleteCopyWithActiveBooking() {
		User guest = users.save(User.registerGuest("del-copy-" + UUID.randomUUID() + "@example.com", "hash-placeholder", TEST_NOW));
		ClubTable table = tables.save(ClubTable.create("Copy table " + UUID.randomUUID(), 4, TEST_NOW));
		Game game = games.save(Game.create("Copy game " + UUID.randomUUID(), TEST_NOW));
		GameCopy copy = copies.save(GameCopy.create(game, TEST_NOW));
		bookings.create(guest.getId(), table.getId(), copy.getId(), START, END, 2, "del-copy");

		assertThatThrownBy(() -> catalog.deleteCopy(copy.getId()))
				.isInstanceOf(DomainException.class)
				.hasMessageContaining("active booking");
	}
}
