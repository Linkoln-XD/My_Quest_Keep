package ru.link.questkeep.catalog;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubTableRepository extends JpaRepository<ClubTable, UUID> {

	Page<ClubTable> findByDeletedAtIsNull(Pageable pageable);

	Optional<ClubTable> findByIdAndDeletedAtIsNull(UUID id);
}
