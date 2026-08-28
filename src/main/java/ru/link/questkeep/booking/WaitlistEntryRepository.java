package ru.link.questkeep.booking;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, UUID> {

	@EntityGraph(attributePaths = { "user", "table", "gameCopy" })
	@Query("select w from WaitlistEntry w where w.id = :id")
	Optional<WaitlistEntry> findFetchedById(@Param("id") UUID id);

	@EntityGraph(attributePaths = { "user", "table", "gameCopy" })
	Page<WaitlistEntry> findByUserId(UUID userId, Pageable pageable);

	@EntityGraph(attributePaths = { "user", "table", "gameCopy" })
	Page<WaitlistEntry> findByStatusOrderByCreatedAtAsc(WaitlistStatus status, Pageable pageable);

	@EntityGraph(attributePaths = { "user", "table", "gameCopy" })
	List<WaitlistEntry> findByUserIdAndStatusAndStartAtAndEndAt(
			UUID userId, WaitlistStatus status, Instant startAt, Instant endAt);
}
