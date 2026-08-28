package ru.link.questkeep.catalog;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.link.questkeep.booking.BookingRepository;
import ru.link.questkeep.booking.BookingStatus;
import ru.link.questkeep.shared.exception.DomainException;
import ru.link.questkeep.shared.exception.ResourceNotFoundException;

@Service
public class CatalogService {

	private static final EnumSet<BookingStatus> OCCUPYING = EnumSet.of(
			BookingStatus.PENDING,
			BookingStatus.CONFIRMED);

	private final Clock clock;
	private final ClubTableRepository tables;
	private final GameRepository games;
	private final GameCopyRepository copies;
	private final BookingRepository bookings;

	public CatalogService(
			Clock clock,
			ClubTableRepository tables,
			GameRepository games,
			GameCopyRepository copies,
			BookingRepository bookings) {
		this.clock = clock;
		this.tables = tables;
		this.games = games;
		this.copies = copies;
		this.bookings = bookings;
	}

	@Transactional
	public ClubTable createTable(String name, int capacity) {
		return tables.save(ClubTable.create(name, capacity, Instant.now(clock)));
	}

	@Transactional(readOnly = true)
	public Page<ClubTable> listTables(Pageable pageable) {
		return tables.findByDeletedAtIsNull(pageable);
	}

	@Transactional(readOnly = true)
	public ClubTable getTable(UUID id) {
		return tables.findByIdAndDeletedAtIsNull(id)
				.orElseThrow(() -> new ResourceNotFoundException("Table not found"));
	}

	@Transactional
	public ClubTable updateTable(UUID id, String name, int capacity) {
		ClubTable table = getTable(id);
		table.update(name, capacity, Instant.now(clock));
		return tables.save(table);
	}

	@Transactional
	public ClubTable deleteTable(UUID tableId) {
		ClubTable table = getTable(tableId);
		if (bookings.existsByTable_IdAndStatusInAndEndAtAfter(tableId, OCCUPYING, Instant.now(clock))) {
			throw new DomainException("Cannot delete a table with an active booking");
		}
		table.markDeleted(Instant.now(clock));
		return tables.save(table);
	}

	@Transactional
	public Game createGame(String title) {
		return games.save(Game.create(title, Instant.now(clock)));
	}

	@Transactional(readOnly = true)
	public Page<Game> listGames(Pageable pageable) {
		return games.findByDeletedAtIsNull(pageable);
	}

	@Transactional(readOnly = true)
	public Game getGame(UUID id) {
		return games.findByIdAndDeletedAtIsNull(id)
				.orElseThrow(() -> new ResourceNotFoundException("Game not found"));
	}

	@Transactional
	public Game updateGame(UUID id, String title) {
		Game game = getGame(id);
		game.updateTitle(title, Instant.now(clock));
		return games.save(game);
	}

	@Transactional
	public Game deleteGame(UUID gameId) {
		Game game = getGame(gameId);
		if (bookings.existsByGameCopy_Game_IdAndStatusInAndEndAtAfter(gameId, OCCUPYING, Instant.now(clock))) {
			throw new DomainException("Cannot delete a game with an active booking");
		}
		game.markDeleted(Instant.now(clock));
		return games.save(game);
	}

	@Transactional
	public GameCopy createCopy(UUID gameId) {
		Game game = getGame(gameId);
		return copies.save(GameCopy.create(game, Instant.now(clock)));
	}

	@Transactional(readOnly = true)
	public Page<GameCopy> listCopies(UUID gameId, Pageable pageable) {
		getGame(gameId);
		return copies.findByGameIdAndDeletedAtIsNull(gameId, pageable);
	}

	@Transactional(readOnly = true)
	public GameCopy getCopy(UUID id) {
		return copies.findByIdAndDeletedAtIsNull(id)
				.orElseThrow(() -> new ResourceNotFoundException("Game copy not found"));
	}

	@Transactional
	public GameCopy deleteCopy(UUID copyId) {
		GameCopy copy = getCopy(copyId);
		if (bookings.existsByGameCopy_IdAndStatusInAndEndAtAfter(copyId, OCCUPYING, Instant.now(clock))) {
			throw new DomainException("Cannot delete a game copy with an active booking");
		}
		copy.markDeleted(Instant.now(clock));
		return copies.save(copy);
	}
}
