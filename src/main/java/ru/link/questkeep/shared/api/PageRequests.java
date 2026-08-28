package ru.link.questkeep.shared.api;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import ru.link.questkeep.shared.exception.DomainException;

public final class PageRequests {

	public static final int DEFAULT_SIZE = 20;
	public static final int MAX_SIZE = 100;

	private PageRequests() {
	}

	public static Pageable of(int page, int size) {
		if (page < 0) {
			throw new DomainException("page must be >= 0");
		}
		if (size < 1 || size > MAX_SIZE) {
			throw new DomainException("size must be between 1 and " + MAX_SIZE);
		}
		return PageRequest.of(page, size);
	}
}
