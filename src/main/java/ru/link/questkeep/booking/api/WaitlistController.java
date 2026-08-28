package ru.link.questkeep.booking.api;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ru.link.questkeep.booking.WaitlistService;
import ru.link.questkeep.shared.api.PageRequests;
import ru.link.questkeep.shared.api.PageResponse;
import ru.link.questkeep.shared.exception.ForbiddenException;
import ru.link.questkeep.shared.exception.ResourceNotFoundException;
import ru.link.questkeep.shared.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/v1/waitlist")
public class WaitlistController {

	private final WaitlistService waitlist;

	public WaitlistController(WaitlistService waitlist) {
		this.waitlist = waitlist;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public WaitlistResponse join(
			@AuthenticationPrincipal AuthenticatedUser user,
			@Valid @RequestBody JoinWaitlistRequest request) {
		return WaitlistResponse.from(waitlist.join(
				user.id(),
				request.tableId(),
				request.gameCopyId(),
				request.startAt(),
				request.endAt()));
	}

	@GetMapping("/me")
	public PageResponse<WaitlistResponse> mine(
			@AuthenticationPrincipal AuthenticatedUser user,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return PageResponse.from(waitlist.listMine(user.id(), PageRequests.of(page, size)).map(WaitlistResponse::from));
	}

	@GetMapping
	public PageResponse<WaitlistResponse> active(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return PageResponse.from(waitlist.listActive(PageRequests.of(page, size)).map(WaitlistResponse::from));
	}

	@PostMapping("/{id}/cancel")
	public WaitlistResponse cancel(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
		try {
			return WaitlistResponse.from(waitlist.cancel(id, user.id(), user.staff()));
		}
		catch (ForbiddenException ex) {
			throw new ResourceNotFoundException("Waitlist entry not found");
		}
	}
}
