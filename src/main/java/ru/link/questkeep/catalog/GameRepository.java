package ru.link.questkeep.catalog;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, UUID> {

	Page<Game> findByDeletedAtIsNull(Pageable pageable);

	Optional<Game> findByIdAndDeletedAtIsNull(UUID id);
}
