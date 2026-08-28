package ru.link.questkeep.shared.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "questkeep.cors")
public record CorsProperties(
		@DefaultValue({"http://localhost:*", "http://127.0.0.1:*"}) List<String> allowedOriginPatterns) {
}
