package ru.link.questkeep.identity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import ru.link.questkeep.shared.exception.DomainException;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

	@Id
	private UUID id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "token_hash", nullable = false, unique = true, length = 64)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected RefreshToken() {
	}

	private RefreshToken(UUID id, User user, String tokenHash, Instant expiresAt, Instant createdAt) {
		this.id = id;
		this.user = user;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
		this.createdAt = createdAt;
	}

	public static RefreshToken issue(User user, String tokenHash, Instant expiresAt, Instant createdAt) {
		if (user == null) {
			throw new DomainException("User is required");
		}
		if (tokenHash == null || tokenHash.length() != 64) {
			throw new DomainException("token_hash must be a SHA-256 hex string");
		}
		if (expiresAt == null || createdAt == null || !expiresAt.isAfter(createdAt)) {
			throw new DomainException("Refresh token expiry must be after creation");
		}
		return new RefreshToken(UUID.randomUUID(), user, tokenHash, expiresAt, createdAt);
	}

	public void revoke(Instant now) {
		if (revokedAt == null) {
			revokedAt = now;
		}
	}

	public boolean isActive(Instant now) {
		return revokedAt == null && now.isBefore(expiresAt);
	}

	public UUID getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
