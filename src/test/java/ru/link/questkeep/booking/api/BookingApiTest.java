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

class BookingApiTest extends AbstractPostgresTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void overlappingBookingsReturn409AndGuestCannotCreateTables() throws Exception {
		String staffToken = login("staff@questkeep.local", "ChangeMe_Staff_Demo_1");

		String tableId = createTable(staffToken);
		String gameId = createGame(staffToken);
		String copyId = createCopy(staffToken, gameId);
		String otherCopyId = createCopy(staffToken, gameId);

		String guestA = register("guest-a-" + UUID.randomUUID() + "@example.com");
		String guestB = register("guest-b-" + UUID.randomUUID() + "@example.com");

		mockMvc.perform(post("/api/v1/tables")
						.header("Authorization", "Bearer " + guestA)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Nope","capacity":4}
								"""))
				.andExpect(status().isForbidden());

		String body = """
				{"tableId":"%s","gameCopyId":"%s","startAt":"2026-08-28T12:00:00Z","endAt":"2026-08-28T14:00:00Z","guestCount":2}
				""".formatted(tableId, copyId);

		mockMvc.perform(post("/api/v1/bookings")
						.header("Authorization", "Bearer " + guestA)
						.header("Idempotency-Key", "key-a")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("CONFIRMED"));

		String overlap = """
				{"tableId":"%s","gameCopyId":"%s","startAt":"2026-08-28T12:00:00Z","endAt":"2026-08-28T14:00:00Z","guestCount":2}
				""".formatted(tableId, otherCopyId);

		mockMvc.perform(post("/api/v1/bookings")
						.header("Authorization", "Bearer " + guestB)
						.header("Idempotency-Key", "key-b")
						.contentType(MediaType.APPLICATION_JSON)
						.content(overlap))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409));

		mockMvc.perform(get("/api/v1/bookings/me")
						.header("Authorization", "Bearer " + guestA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	private String login(String email, String password) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"%s"}
								""".formatted(email, password)))
				.andExpect(status().isOk())
				.andReturn();
		return token(result);
	}

	private String register(String email) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"password1"}
								""".formatted(email)))
				.andExpect(status().isCreated())
				.andReturn();
		return token(result);
	}

	private String createTable(String staffToken) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/tables")
						.header("Authorization", "Bearer " + staffToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Oak %s","capacity":4}
								""".formatted(UUID.randomUUID())))
				.andExpect(status().isCreated())
				.andReturn();
		return id(result);
	}

	private String createGame(String staffToken) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/games")
						.header("Authorization", "Bearer " + staffToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Catan %s"}
								""".formatted(UUID.randomUUID())))
				.andExpect(status().isCreated())
				.andReturn();
		return id(result);
	}

	private String createCopy(String staffToken, String gameId) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/games/" + gameId + "/copies")
						.header("Authorization", "Bearer " + staffToken))
				.andExpect(status().isCreated())
				.andReturn();
		return id(result);
	}

	private String token(MvcResult result) throws Exception {
		JsonNode json = new ObjectMapper().readTree(result.getResponse().getContentAsString());
		return json.get("accessToken").asText();
	}

	private String id(MvcResult result) throws Exception {
		JsonNode json = new ObjectMapper().readTree(result.getResponse().getContentAsString());
		return json.get("id").asText();
	}
}
