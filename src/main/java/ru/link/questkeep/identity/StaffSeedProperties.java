package ru.link.questkeep.identity;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "questkeep.staff")
public record StaffSeedProperties(
		@DefaultValue("staff@questkeep.local") String email,
		@DefaultValue("ChangeMe_Staff_Demo_1") String password) {
}
