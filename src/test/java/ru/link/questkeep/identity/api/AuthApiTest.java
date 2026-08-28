package ru.link.questkeep.identity.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.link.questkeep.AbstractPostgresTest;
import ru.link.questkeep.HttpSupport;

class AuthApiTest extends AbstractPostgresTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void registerRejectsDuplicateEmailAndShortPassword() throws Exception {
		HttpSupport http = new HttpSupport(mockMvc);
		String email = "dup-" + UUID.randomUUID() + "@example.com";
		http.register(email);

		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"password1"}
								""".formatted(email)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Email is already registered"));

		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"short-%s@example.com","password":"123"}
								""".formatted(UUID.randomUUID())))
				.andExpect(status().isBadRequest());
	}

	@Test
	void loginRejectsUnknownPassword() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"staff@questkeep.local","password":"wrong-password"}
								"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void refreshRotatesAndOldTokenStopsWorking() throws Exception {
		HttpSupport http = new HttpSupport(mockMvc);
		JsonNode first = http.registerBody("refresh-" + UUID.randomUUID() + "@example.com");
		String refresh1 = first.get("refreshToken").asText();

		JsonNode second = HttpSupport.json(mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"refreshToken":"%s"}
								""".formatted(refresh1)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isString())
				.andReturn());
		String refresh2 = second.get("refreshToken").asText();

		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"refreshToken":"%s"}
								""".formatted(refresh1)))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"refreshToken":"%s"}
								""".formatted(refresh2)))
				.andExpect(status().isOk());
	}
}
