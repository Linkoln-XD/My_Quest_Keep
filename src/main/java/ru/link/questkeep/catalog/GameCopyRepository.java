package ru.link.questkeep.catalog;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GameCopyRepository extends JpaRepository<GameCopy, UUID> {

	List<GameCopy> findByGameIdAndDeletedAtIsNull(UUID gameId);

	List<GameCopy> findByDeletedAtIsNull();
}
