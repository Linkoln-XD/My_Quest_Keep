package ru.link.questkeep.identity;

import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import ru.link.questkeep.shared.exception.DomainException;

import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

	@Id
	private UUID id;

	@Column(nullable = false, unique = true, length = 320)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private Role role;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected User() {
	}

	private User(UUID id, String email, String passwordHash, Role role, Instant createdAt) {
		this.id = id;
		this.email = email;
		this.passwordHash = passwordHash;
		this.role = role;
		this.createdAt = createdAt;
	}

	public static User registerGuest(String email, String passwordHash, Instant createdAt) {
		return create(email, passwordHash, Role.GUEST, createdAt);
	}

	public static User registerStaff(String email, String passwordHash, Instant createdAt) {
		return create(email, passwordHash, Role.STAFF, createdAt);
	}

	private static User create(String email, String passwordHash, Role role, Instant createdAt) {
		String normalized = normalizeEmail(email);
		if (passwordHash == null || passwordHash.isBlank()) {
			throw new DomainException("Password hash is required");
		}
		if (createdAt == null) {
			throw new DomainException("createdAt is required");
		}
		return new User(UUID.randomUUID(), normalized, passwordHash, role, createdAt);
	}

	public static String normalizeEmail(String email) {
		if (email == null || email.isBlank()) {
			throw new DomainException("Email is required");
		}
		return email.trim().toLowerCase(Locale.ROOT);
	}

	public UUID getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public Role getRole() {
		return role;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
