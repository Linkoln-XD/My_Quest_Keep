package ru.link.questkeep.identity;

import java.time.Clock;
import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.link.questkeep.shared.exception.DomainException;
import ru.link.questkeep.shared.exception.UnauthorizedException;

@Service
public class AuthService {

	private final Clock clock;
	private final UserRepository users;
	private final RefreshTokenRepository refreshTokens;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final JwtProperties jwtProperties;

	public AuthService(
			Clock clock,
			UserRepository users,
			RefreshTokenRepository refreshTokens,
			PasswordEncoder passwordEncoder,
			JwtService jwtService,
			JwtProperties jwtProperties) {
		this.clock = clock;
		this.users = users;
		this.refreshTokens = refreshTokens;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.jwtProperties = jwtProperties;
	}

	@Transactional
	public TokenPair register(String email, String rawPassword) {
		String normalized = User.normalizeEmail(email);
		if (users.findByEmail(normalized).isPresent()) {
			throw new DomainException("Email is already registered");
		}
		User user = users.save(User.registerGuest(normalized, passwordEncoder.encode(rawPassword), Instant.now(clock)));
		return issuePair(user);
	}

	@Transactional
	public TokenPair login(String email, String rawPassword) {
		User user = users.findByEmail(User.normalizeEmail(email))
				.filter(found -> passwordEncoder.matches(rawPassword, found.getPasswordHash()))
				.orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
		return issuePair(user);
	}

	@Transactional
	public TokenPair refresh(String rawRefreshToken) {
		if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
			throw new UnauthorizedException("Refresh token is required");
		}
		Instant now = Instant.now(clock);
		RefreshToken stored = refreshTokens.findByTokenHash(TokenHashes.sha256Hex(rawRefreshToken))
				.filter(token -> token.isActive(now))
				.orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
		stored.revoke(now);
		refreshTokens.save(stored);
		return issuePair(stored.getUser());
	}

	private TokenPair issuePair(User user) {
		Instant now = Instant.now(clock);
		String rawRefresh = TokenHashes.newRefreshToken();
		RefreshToken stored = RefreshToken.issue(
				user,
				TokenHashes.sha256Hex(rawRefresh),
				now.plus(jwtProperties.refreshTtl()),
				now);
		refreshTokens.save(stored);
		return new TokenPair(jwtService.createAccessToken(user), rawRefresh);
	}

	public record TokenPair(String accessToken, String refreshToken) {
	}
}
