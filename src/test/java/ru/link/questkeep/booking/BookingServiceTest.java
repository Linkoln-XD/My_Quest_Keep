package ru.link.questkeep.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.link.questkeep.AbstractPostgresTest;
import ru.link.questkeep.catalog.ClubTable;
import ru.link.questkeep.catalog.ClubTableRepository;
import ru.link.questkeep.catalog.Game;
import ru.link.questkeep.catalog.GameCopy;
import ru.link.questkeep.catalog.GameCopyRepository;
import ru.link.questkeep.catalog.GameRepository;
import ru.link.questkeep.identity.User;
import ru.link.questkeep.identity.UserRepository;
import ru.link.questkeep.shared.exception.BookingConflictException;
import ru.link.questkeep.shared.exception.DomainException;

class BookingServiceTest extends AbstractPostgresTest {

	private static final Instant START = Instant.parse("2026-08-28T12:00:00Z");
	private static final Instant END = Instant.parse("2026-08-28T14:00:00Z");

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

	@Autowired
	private org.springframework.jdbc.core.JdbcTemplate jdbc;

	@Autowired
	private jakarta.persistence.EntityManager entityManager;

	@Test
	void sameIdempotencyKeyDifferentPayloadIsRejected() {
		CatalogFixture fixture = catalog("idem-diff");
		bookings.create(fixture.guest.getId(), fixture.table.getId(), fixture.copy.getId(), START, END, 2, "diff-key");
		GameCopy otherCopy = copies.save(GameCopy.create(fixture.game, TEST_NOW));
		assertThatThrownBy(() -> bookings.create(
				fixture.guest.getId(), fixture.table.getId(), otherCopy.getId(), START, END, 2, "diff-key"))
				.isInstanceOf(DomainException.class)
				.hasMessageContaining("Idempotency-Key");
	}

	@Test
	void getExpiresBookingWhenIntervalHasEnded() {
		CatalogFixture fixture = catalog("expire-read");
		Booking created = bookings.create(
				fixture.guest.getId(), fixture.table.getId(), fixture.copy.getId(), START, END, 2, "expire-key");
		jdbc.update(
				"update bookings set start_at = ?, end_at = ? where id = ?",
				java.sql.Timestamp.from(TEST_NOW.minusSeconds(7200)),
				java.sql.Timestamp.from(TEST_NOW),
				created.getId());
		entityManager.clear();
		assertThat(bookings.get(created.getId()).getStatus()).isEqualTo(BookingStatus.EXPIRED);
	}

	@Test
	void secondBookingOnSameTableAndOverlappingIntervalConflicts() {
		CatalogFixture fixture = catalog("table-conflict");
		User other = guest("other-table");
		GameCopy otherCopy = copies.save(GameCopy.create(fixture.game, TEST_NOW));

		bookings.create(fixture.guest.getId(), fixture.table.getId(), fixture.copy.getId(), START, END, 2, "table-1");

		assertThatThrownBy(() -> bookings.create(
				other.getId(), fixture.table.getId(), otherCopy.getId(), START, END, 2, "table-2"))
				.isInstanceOf(BookingConflictException.class);
	}

	@Test
	void secondBookingOnSameCopyAndOverlappingIntervalConflicts() {
		CatalogFixture fixture = catalog("copy-conflict");
		User other = guest("other-copy");
		ClubTable otherTable = tables.save(ClubTable.create("Other " + UUID.randomUUID(), 4, TEST_NOW));

		bookings.create(fixture.guest.getId(), fixture.table.getId(), fixture.copy.getId(), START, END, 2, "copy-1");

		assertThatThrownBy(() -> bookings.create(
				other.getId(), otherTable.getId(), fixture.copy.getId(), START, END, 2, "copy-2"))
				.isInstanceOf(BookingConflictException.class);
	}

	@Test
	void guestCountCannotExceedCapacity() {
		CatalogFixture fixture = catalog("capacity");
		assertThatThrownBy(() -> bookings.create(
				fixture.guest.getId(), fixture.table.getId(), fixture.copy.getId(), START, END, 5, "cap-1"))
				.isInstanceOf(DomainException.class)
				.hasMessageContaining("capacity");
	}

	@Test
	void sameIdempotencyKeyReturnsTheSameBooking() {
		CatalogFixture fixture = catalog("idem");
		Booking first = bookings.create(
				fixture.guest.getId(), fixture.table.getId(), fixture.copy.getId(), START, END, 2, "same-key");
		Booking replay = bookings.create(
				fixture.guest.getId(), fixture.table.getId(), fixture.copy.getId(), START, END, 2, "same-key");
		assertThat(replay.getId()).isEqualTo(first.getId());
		assertThat(bookings.listForUser(fixture.guest.getId(), org.springframework.data.domain.PageRequest.of(0, 10)).getTotalElements())
				.isEqualTo(1);
	}

	@Test
	void cancelIsIdempotent() {
		CatalogFixture fixture = catalog("cancel");
		Booking created = bookings.create(
				fixture.guest.getId(), fixture.table.getId(), fixture.copy.getId(), START, END, 2, "cancel-key");
		Booking cancelled = bookings.cancel(created.getId(), fixture.guest.getId(), false);
		Booking again = bookings.cancel(created.getId(), fixture.guest.getId(), false);
		assertThat(cancelled.getStatus()).isEqualTo(BookingStatus.CANCELLED);
		assertThat(again.getStatus()).isEqualTo(BookingStatus.CANCELLED);
	}

	@Test
	void concurrentOverlappingCreatesOnSameTableYieldOneConflict() throws Exception {
		CatalogFixture fixture = catalog("parallel");
		User other = guest("parallel-other");
		GameCopy otherCopy = copies.save(GameCopy.create(fixture.game, TEST_NOW));

		ExecutorService pool = Executors.newFixedThreadPool(2);
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch go = new CountDownLatch(1);
		AtomicInteger successes = new AtomicInteger();
		AtomicInteger conflicts = new AtomicInteger();

		Future<?> first = pool.submit(() -> runCreate(
				ready,
				go,
				successes,
				conflicts,
				fixture.guest.getId(),
				fixture.table.getId(),
				fixture.copy.getId(),
				"par-a"));
		Future<?> second = pool.submit(() -> runCreate(
				ready,
				go,
				successes,
				conflicts,
				other.getId(),
				fixture.table.getId(),
				otherCopy.getId(),
				"par-b"));

		assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
		go.countDown();
		first.get(20, TimeUnit.SECONDS);
		second.get(20, TimeUnit.SECONDS);
		pool.shutdown();

		assertThat(successes.get()).isEqualTo(1);
		assertThat(conflicts.get()).isEqualTo(1);
	}

	private void runCreate(
			CountDownLatch ready,
			CountDownLatch go,
			AtomicInteger successes,
			AtomicInteger conflicts,
			UUID userId,
			UUID tableId,
			UUID copyId,
			String key) {
		ready.countDown();
		try {
			if (!go.await(5, TimeUnit.SECONDS)) {
				throw new IllegalStateException("Timed out waiting to start");
			}
			bookings.create(userId, tableId, copyId, START, END, 2, key);
			successes.incrementAndGet();
		}
		catch (BookingConflictException ex) {
			conflicts.incrementAndGet();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(ex);
		}
	}

	private User guest(String label) {
		return users.save(User.registerGuest(label + "-" + UUID.randomUUID() + "@example.com", "hash-placeholder", TEST_NOW));
	}

	private CatalogFixture catalog(String label) {
		User guest = guest(label);
		ClubTable table = tables.save(ClubTable.create("Table " + label + " " + UUID.randomUUID(), 4, TEST_NOW));
		Game game = games.save(Game.create("Game " + label + " " + UUID.randomUUID(), TEST_NOW));
		GameCopy copy = copies.save(GameCopy.create(game, TEST_NOW));
		return new CatalogFixture(guest, table, game, copy);
	}

	private record CatalogFixture(User guest, ClubTable table, Game game, GameCopy copy) {
	}
}
