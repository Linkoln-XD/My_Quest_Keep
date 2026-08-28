package ru.link.questkeep.catalog;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubTableRepository extends JpaRepository<ClubTable, UUID> {

	List<ClubTable> findByDeletedAtIsNull();
}
