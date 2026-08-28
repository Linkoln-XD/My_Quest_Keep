package ru.link.questkeep.booking;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.link.questkeep.catalog.ClubTable;
import ru.link.questkeep.catalog.ClubTableRepository;
import ru.link.questkeep.catalog.GameCopy;
import ru.link.questkeep.catalog.GameCopyRepository;
import ru.link.questkeep.identity.User;
import ru.link.questkeep.identity.UserRepository;
import ru.link.questkeep.shared.exception.DomainException;
import ru.link.questkeep.shared.exception.ForbiddenException;
import ru.link.questkeep.shared.exception.ResourceNotFoundException;

@Service
public class WaitlistService {

	private final Clock clock;
	private final UserRepository users;
	private final ClubTableRepository tables;
	private final GameCopyRepository copies;
	private final WaitlistEntryRepository waitlist;

	public WaitlistService(
			Clock clock,
			UserRepository users,
			ClubTableRepository tables,
			GameCopyRepository copies,
			WaitlistEntryRepository waitlist) {
		this.clock = clock;
		this.users = users;
		this.tables = tables;
		this.copies = copies;
		this.waitlist = waitlist;
	}

	@Transactional
	public WaitlistEntry join(UUID userId, UUID tableId, UUID copyId, Instant startAt, Instant endAt) {
		if (tableId == null && copyId == null) {
			throw new DomainException("Waitlist entry must target a table and/or a game copy");
		}
		return waitlist.findByUserIdAndStatusAndStartAtAndEndAt(userId, WaitlistStatus.ACTIVE, startAt, endAt)
				.stream()
				.filter(entry -> Objects.equals(tableId, entry.getTable() == null ? null : entry.getTable().getId())
						&& Objects.equals(copyId, entry.getGameCopy() == null ? null : entry.getGameCopy().getId()))
				.findFirst()
				.orElseGet(() -> insert(userId, tableId, copyId, startAt, endAt));
	}

	private WaitlistEntry insert(UUID userId, UUID tableId, UUID copyId, Instant startAt, Instant endAt) {
		Instant now = Instant.now(clock);
		User user = users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
		ClubTable table = null;
		if (tableId != null) {
			table = tables.findByIdAndDeletedAtIsNull(tableId)
					.orElseThrow(() -> new ResourceNotFoundException("Table not found"));
		}
		GameCopy copy = null;
		if (copyId != null) {
			copy = copies.findByIdAndDeletedAtIsNull(copyId)
					.orElseThrow(() -> new ResourceNotFoundException("Game copy not found"));
		}
		return waitlist.save(WaitlistEntry.join(user, table, copy, startAt, endAt, now));
	}

	@Transactional(readOnly = true)
	public Page<WaitlistEntry> listMine(UUID userId, Pageable pageable) {
		return waitlist.findByUserId(userId, pageable);
	}

	@Transactional(readOnly = true)
	public Page<WaitlistEntry> listActive(Pageable pageable) {
		return waitlist.findByStatusOrderByCreatedAtAsc(WaitlistStatus.ACTIVE, pageable);
	}

	@Transactional
	public WaitlistEntry cancel(UUID entryId, UUID actorId, boolean staff) {
		WaitlistEntry entry = waitlist.findFetchedById(entryId)
				.orElseThrow(() -> new ResourceNotFoundException("Waitlist entry not found"));
		if (!staff && !entry.getUser().getId().equals(actorId)) {
			throw new ForbiddenException("Cannot cancel another user's waitlist entry");
		}
		entry.cancel();
		return waitlist.save(entry);
	}
}
