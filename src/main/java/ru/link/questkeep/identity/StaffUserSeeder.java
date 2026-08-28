package ru.link.questkeep.identity;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class StaffUserSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(StaffUserSeeder.class);

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final StaffSeedProperties properties;
	private final Clock clock;

	public StaffUserSeeder(
			UserRepository users,
			PasswordEncoder passwordEncoder,
			StaffSeedProperties properties,
			Clock clock) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.properties = properties;
		this.clock = clock;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (properties.email() == null || properties.email().isBlank()
				|| properties.password() == null || properties.password().isBlank()) {
			log.warn("STAFF seed skipped: questkeep.staff.email/password not set");
			return;
		}
		String email = User.normalizeEmail(properties.email());
		if (users.findByEmail(email).isPresent()) {
			return;
		}
		users.save(User.registerStaff(email, passwordEncoder.encode(properties.password()), Instant.now(clock)));
		log.info("Seeded STAFF user {}", email);
	}
}
