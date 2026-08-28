package ru.link.questkeep.booking.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import ru.link.questkeep.AbstractPostgresTest;

class WaitlistApiTest extends AbstractPostgresTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void guestJoinsWaitlistStaffListsItAndGuestCannotListAll() throws Exception {
		String staffToken = login("staff@questkeep.local", "ChangeMe_Staff_Demo_1");
		String tableId = createTable(staffToken);
		String guestToken = register("wait-" + UUID.randomUUID() + "@example.com");

		mockMvc.perform(get("/api/v1/waitlist")
						.header("Authorization", "Bearer " + guestToken))
				.andExpect(status().isForbidden());

		String joinBody = """
				{"tableId":"%s","startAt":"2026-08-28T12:00:00Z","endAt":"2026-08-28T14:00:00Z"}
				""".formatted(tableId);

		MvcResult created = mockMvc.perform(post("/api/v1/waitlist")
						.header("Authorization", "Bearer " + guestToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(joinBody))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andReturn();
		String entryId = new ObjectMapper().readTree(created.getResponse().getContentAsString()).get("id").asText();

		mockMvc.perform(post("/api/v1/waitlist")
						.header("Authorization", "Bearer " + guestToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(joinBody))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(entryId));

		mockMvc.perform(get("/api/v1/waitlist")
						.header("Authorization", "Bearer " + staffToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1));

		mockMvc.perform(post("/api/v1/waitlist/" + entryId + "/cancel")
						.header("Authorization", "Bearer " + guestToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"));

		mockMvc.perform(post("/api/v1/waitlist/" + entryId + "/cancel")
						.header("Authorization", "Bearer " + guestToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"));
	}

	private String login(String email, String password) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"%s"}
								""".formatted(email, password)))
				.andExpect(status().isOk())
				.andReturn();
		return json(result).get("accessToken").asText();
	}

	private String register(String email) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"password1"}
								""".formatted(email)))
				.andExpect(status().isCreated())
				.andReturn();
		return json(result).get("accessToken").asText();
	}

	private String createTable(String staffToken) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/tables")
						.header("Authorization", "Bearer " + staffToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Wait %s","capacity":4}
								""".formatted(UUID.randomUUID())))
				.andExpect(status().isCreated())
				.andReturn();
		return json(result).get("id").asText();
	}

	private static JsonNode json(MvcResult result) throws Exception {
		return new ObjectMapper().readTree(result.getResponse().getContentAsString());
	}
}
