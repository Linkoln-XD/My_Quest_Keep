package ru.link.questkeep.shared.exception;

public class BookingConflictException extends RuntimeException {

	public BookingConflictException() {
		super("The table or game copy is already booked for this interval");
	}
}
