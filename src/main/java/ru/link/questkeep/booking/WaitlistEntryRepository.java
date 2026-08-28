package ru.link.questkeep.booking;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, UUID> {
}
