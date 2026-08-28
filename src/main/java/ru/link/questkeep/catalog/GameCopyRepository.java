package ru.link.questkeep.catalog;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameCopyRepository extends JpaRepository<GameCopy, UUID> {

	@EntityGraph(attributePaths = "game")
	Page<GameCopy> findByGameIdAndDeletedAtIsNull(UUID gameId, Pageable pageable);

	@EntityGraph(attributePaths = "game")
	Optional<GameCopy> findByIdAndDeletedAtIsNull(UUID id);
}
