package ru.link.questkeep.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import ru.link.questkeep.catalog.ClubTable;
import ru.link.questkeep.catalog.Game;
import ru.link.questkeep.catalog.GameCopy;
import ru.link.questkeep.identity.User;
import ru.link.questkeep.shared.exception.DomainException;

class BookingDomainTest {

	@Test
	void expireIfEndedWhenNowReachesEnd() {
		Instant now = Instant.parse("2026-08-28T10:00:00Z");
		Booking booking = sample(now);
		assertThat(booking.expireIfEnded(Instant.parse("2026-08-28T13:59:59Z"))).isFalse();
		assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
		assertThat(booking.expireIfEnded(Instant.parse("2026-08-28T14:00:00Z"))).isTrue();
		assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
	}

	@Test
	void rejectsUnalignedStart() {
		Instant now = Instant.parse("2026-08-28T10:00:00Z");
		assertThatThrownBy(() -> Booking.confirmNew(
				ClubTable.create("Oak", 4, now),
				GameCopy.create(Game.create("Catan", now), now),
				User.registerGuest("slot@example.com", "hash-placeholder", now),
				Instant.parse("2026-08-28T12:10:00Z"),
				Instant.parse("2026-08-28T14:00:00Z"),
				2,
				null,
				now))
				.isInstanceOf(DomainException.class)
				.hasMessageContaining("aligned");
	}

	private static Booking sample(Instant now) {
		return Booking.confirmNew(
				ClubTable.create("Oak", 4, now),
				GameCopy.create(Game.create("Catan", now), now),
				User.registerGuest("slot2@example.com", "hash-placeholder", now),
				Instant.parse("2026-08-28T12:00:00Z"),
				Instant.parse("2026-08-28T14:00:00Z"),
				2,
				null,
				now);
	}
}
