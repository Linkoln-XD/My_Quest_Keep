package ru.link.questkeep.booking;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

	@EntityGraph(attributePaths = { "table", "gameCopy", "user" })
	Optional<Booking> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);

	@EntityGraph(attributePaths = { "table", "gameCopy", "user" })
	@Query("select b from Booking b where b.id = :id")
	Optional<Booking> findFetchedById(@Param("id") UUID id);

	@EntityGraph(attributePaths = { "table", "gameCopy", "user" })
	Page<Booking> findByUserId(UUID userId, Pageable pageable);

	@EntityGraph(attributePaths = { "table", "gameCopy", "user" })
	@Query("select b from Booking b")
	Page<Booking> findAllFetched(Pageable pageable);

	boolean existsByTable_IdAndStatusInAndEndAtAfter(
			UUID tableId, Collection<BookingStatus> statuses, Instant now);

	boolean existsByGameCopy_IdAndStatusInAndEndAtAfter(
			UUID gameCopyId, Collection<BookingStatus> statuses, Instant now);

	boolean existsByGameCopy_Game_IdAndStatusInAndEndAtAfter(
			UUID gameId, Collection<BookingStatus> statuses, Instant now);
}
