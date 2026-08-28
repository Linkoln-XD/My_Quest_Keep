package ru.link.questkeep;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractPostgresTest.FixedClockConfig.class)
public abstract class AbstractPostgresTest {

	public static final Instant TEST_NOW = Instant.parse("2026-08-28T10:00:00Z");

	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

	static {
		POSTGRES.start();
	}

	@DynamicPropertySource
	static void registerDatasource(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("questkeep.jwt.secret", () -> "test-jwt-secret-which-is-at-least-32b");
		registry.add("questkeep.staff.email", () -> "staff@questkeep.local");
		registry.add("questkeep.staff.password", () -> "ChangeMe_Staff_Demo_1");
	}

	@TestConfiguration
	public static class FixedClockConfig {

		@Bean
		@Primary
		Clock clock() {
			return Clock.fixed(TEST_NOW, ZoneOffset.UTC);
		}
	}
}
