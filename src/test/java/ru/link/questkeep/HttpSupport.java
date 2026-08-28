package ru.link.questkeep;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class HttpSupport {

	private static final ObjectMapper JSON = new ObjectMapper();

	private final MockMvc mockMvc;

	public HttpSupport(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	public String login(String email, String password) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"%s"}
								""".formatted(email, password)))
				.andExpect(status().isOk())
				.andReturn();
		return json(result).get("accessToken").asText();
	}

	public String staffToken() throws Exception {
		return login("staff@questkeep.local", "ChangeMe_Staff_Demo_1");
	}

	public JsonNode registerBody(String email) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"password1"}
								""".formatted(email)))
				.andExpect(status().isCreated())
				.andReturn();
		return json(result);
	}

	public String register(String email) throws Exception {
		return registerBody(email).get("accessToken").asText();
	}

	public String guestToken() throws Exception {
		return register("guest-" + UUID.randomUUID() + "@example.com");
	}

	public String createTable(String staffToken) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/tables")
						.header("Authorization", "Bearer " + staffToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Oak %s","capacity":4}
								""".formatted(UUID.randomUUID())))
				.andExpect(status().isCreated())
				.andReturn();
		return json(result).get("id").asText();
	}

	public String createGame(String staffToken) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/games")
						.header("Authorization", "Bearer " + staffToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Catan %s"}
								""".formatted(UUID.randomUUID())))
				.andExpect(status().isCreated())
				.andReturn();
		return json(result).get("id").asText();
	}

	public String createCopy(String staffToken, String gameId) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/games/" + gameId + "/copies")
						.header("Authorization", "Bearer " + staffToken))
				.andExpect(status().isCreated())
				.andReturn();
		return json(result).get("id").asText();
	}

	public static JsonNode json(MvcResult result) throws Exception {
		return JSON.readTree(result.getResponse().getContentAsString());
	}

	public static String bookingBody(String tableId, String copyId) {
		return """
				{"tableId":"%s","gameCopyId":"%s","startAt":"2026-08-28T12:00:00Z","endAt":"2026-08-28T14:00:00Z","guestCount":2}
				""".formatted(tableId, copyId);
	}
}
