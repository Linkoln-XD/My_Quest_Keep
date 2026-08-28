package ru.link.questkeep.booking;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

	Optional<Booking> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);

	Page<Booking> findByUserId(UUID userId, Pageable pageable);
}
