package ru.link.questkeep.booking.api;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ru.link.questkeep.booking.Booking;
import ru.link.questkeep.booking.BookingService;
import ru.link.questkeep.shared.api.PageRequests;
import ru.link.questkeep.shared.api.PageResponse;
import ru.link.questkeep.shared.exception.ForbiddenException;
import ru.link.questkeep.shared.exception.ResourceNotFoundException;
import ru.link.questkeep.shared.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

	private final BookingService bookings;

	public BookingController(BookingService bookings) {
		this.bookings = bookings;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public BookingResponse create(
			@AuthenticationPrincipal AuthenticatedUser user,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody CreateBookingRequest request) {
		return BookingResponse.from(bookings.create(
				user.id(),
				request.tableId(),
				request.gameCopyId(),
				request.startAt(),
				request.endAt(),
				request.guestCount(),
				idempotencyKey));
	}

	@GetMapping("/me")
	public PageResponse<BookingResponse> mine(
			@AuthenticationPrincipal AuthenticatedUser user,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return PageResponse.from(bookings.listForUser(user.id(), PageRequests.of(page, size)).map(BookingResponse::from));
	}

	@GetMapping
	public PageResponse<BookingResponse> all(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return PageResponse.from(bookings.listAll(PageRequests.of(page, size)).map(BookingResponse::from));
	}

	@GetMapping("/{id}")
	public BookingResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
		Booking booking = bookings.get(id);
		if (!user.staff() && !booking.getUser().getId().equals(user.id())) {
			throw new ResourceNotFoundException("Booking not found");
		}
		return BookingResponse.from(booking);
	}

	@PostMapping("/{id}/cancel")
	public BookingResponse cancel(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
		try {
			return BookingResponse.from(bookings.cancel(id, user.id(), user.staff()));
		}
		catch (ForbiddenException ex) {
			throw new ResourceNotFoundException("Booking not found");
		}
	}
}
