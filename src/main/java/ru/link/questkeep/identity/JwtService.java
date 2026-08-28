package ru.link.questkeep.identity;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Component;

import ru.link.questkeep.shared.exception.UnauthorizedException;

@Component
public class JwtService {

	private final JwtProperties properties;
	private final Clock clock;
	private final MACSigner signer;
	private final MACVerifier verifier;

	public JwtService(JwtProperties properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
		byte[] secret = requireSecret(properties.secret());
		try {
			this.signer = new MACSigner(secret);
			this.verifier = new MACVerifier(secret);
		}
		catch (JOSEException ex) {
			throw new IllegalStateException("Cannot initialize JWT signer", ex);
		}
	}

	public String createAccessToken(User user) {
		Instant now = Instant.now(clock);
		Instant exp = now.plus(properties.accessTtl());
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.subject(user.getId().toString())
				.claim("email", user.getEmail())
				.claim("role", user.getRole().name())
				.issueTime(Date.from(now))
				.expirationTime(Date.from(exp))
				.build();
		try {
			SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
			jwt.sign(signer);
			return jwt.serialize();
		}
		catch (JOSEException ex) {
			throw new IllegalStateException("Cannot sign access token", ex);
		}
	}

	public AccessTokenPayload parseAccessToken(String token) {
		try {
			SignedJWT jwt = SignedJWT.parse(token);
			if (!jwt.verify(verifier)) {
				throw new UnauthorizedException("Invalid access token");
			}
			JWTClaimsSet claims = jwt.getJWTClaimsSet();
			Date expiration = claims.getExpirationTime();
			if (expiration == null || !expiration.toInstant().isAfter(Instant.now(clock))) {
				throw new UnauthorizedException("Access token expired");
			}
			UUID userId = UUID.fromString(claims.getSubject());
			String email = claims.getStringClaim("email");
			Role role = Role.valueOf(claims.getStringClaim("role"));
			return new AccessTokenPayload(userId, email, role);
		}
		catch (UnauthorizedException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new UnauthorizedException("Invalid access token");
		}
	}

	private static byte[] requireSecret(String secret) {
		if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
			throw new IllegalStateException("JWT_SECRET must be at least 32 bytes");
		}
		return secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
	}

	public record AccessTokenPayload(UUID userId, String email, Role role) {
	}
}
