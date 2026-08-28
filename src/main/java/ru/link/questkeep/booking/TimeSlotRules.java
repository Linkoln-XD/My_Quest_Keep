package ru.link.questkeep.booking;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import ru.link.questkeep.shared.exception.DomainException;

public final class TimeSlotRules {

	public static final int SLOT_MINUTES = 30;
	public static final int MIN_DURATION_MINUTES = 60;
	public static final int MAX_DURATION_MINUTES = 240;

	private TimeSlotRules() {
	}

	public static void validateInterval(Instant start, Instant end, Instant now) {
		if (start == null || end == null || now == null) {
			throw new DomainException("start, end and now are required");
		}
		requireAligned(start, "start");
		requireAligned(end, "end");
		if (!start.isAfter(now)) {
			throw new DomainException("Booking must start in the future");
		}
		if (!end.isAfter(start)) {
			throw new DomainException("Booking end must be after start");
		}
		long minutes = Duration.between(start, end).toMinutes();
		if (minutes < MIN_DURATION_MINUTES || minutes > MAX_DURATION_MINUTES) {
			throw new DomainException("Booking duration must be between 1 and 4 hours");
		}
		if (minutes % SLOT_MINUTES != 0) {
			throw new DomainException("Booking duration must be a multiple of 30 minutes");
		}
	}

	public static void requireAligned(Instant instant, String field) {
		ZonedDateTime utc = instant.atZone(ZoneOffset.UTC);
		if (utc.getSecond() != 0 || utc.getNano() != 0) {
			throw new DomainException(field + " must be aligned to a 30-minute UTC slot");
		}
		int minute = utc.getMinute();
		if (minute != 0 && minute != 30) {
			throw new DomainException(field + " must be aligned to a 30-minute UTC slot");
		}
	}
}
