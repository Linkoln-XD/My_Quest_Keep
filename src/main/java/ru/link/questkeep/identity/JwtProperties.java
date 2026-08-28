package ru.link.questkeep.identity;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "questkeep.jwt")
public record JwtProperties(
		@DefaultValue("change-me-to-a-long-random-string-at-least-32-chars") String secret,
		@DefaultValue("15m") Duration accessTtl,
		@DefaultValue("7d") Duration refreshTtl) {
}
