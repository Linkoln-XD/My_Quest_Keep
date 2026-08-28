package ru.link.questkeep.booking.api;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import ru.link.questkeep.AbstractPostgresTest;
import ru.link.questkeep.HttpSupport;

class BookingHttpContractTest extends AbstractPostgresTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void createRequiresIdempotencyKey() throws Exception {
		HttpSupport http = new HttpSupport(mockMvc);
		String staff = http.staffToken();
		String guest = http.guestToken();
		String tableId = http.createTable(staff);
		String copyId = http.createCopy(staff, http.createGame(staff));

		mockMvc.perform(post("/api/v1/bookings")
						.header("Authorization", "Bearer " + guest)
						.contentType(MediaType.APPLICATION_JSON)
						.content(HttpSupport.bookingBody(tableId, copyId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Missing header: Idempotency-Key"));
	}

	@Test
	void reusedKeyWithDifferentPayloadIsRejected() throws Exception {
		HttpSupport http = new HttpSupport(mockMvc);
		String staff = http.staffToken();
		String guest = http.guestToken();
		String tableId = http.createTable(staff);
		String gameId = http.createGame(staff);
		String copyA = http.createCopy(staff, gameId);
		String copyB = http.createCopy(staff, gameId);

		mockMvc.perform(post("/api/v1/bookings")
						.header("Authorization", "Bearer " + guest)
						.header("Idempotency-Key", "same-key")
						.contentType(MediaType.APPLICATION_JSON)
						.content(HttpSupport.bookingBody(tableId, copyA)))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/bookings")
						.header("Authorization", "Bearer " + guest)
						.header("Idempotency-Key", "same-key")
						.contentType(MediaType.APPLICATION_JSON)
						.content(HttpSupport.bookingBody(tableId, copyB)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Idempotency-Key was reused with a different request"));
	}

	@Test
	void guestCannotSeeOrCancelSomeoneElsessBooking() throws Exception {
		HttpSupport http = new HttpSupport(mockMvc);
		String staff = http.staffToken();
		String owner = http.guestToken();
		String other = http.guestToken();
		String tableId = http.createTable(staff);
		String copyId = http.createCopy(staff, http.createGame(staff));

		MvcResult created = mockMvc.perform(post("/api/v1/bookings")
						.header("Authorization", "Bearer " + owner)
						.header("Idempotency-Key", "owner-key")
						.contentType(MediaType.APPLICATION_JSON)
						.content(HttpSupport.bookingBody(tableId, copyId)))
				.andExpect(status().isCreated())
				.andReturn();
		String bookingId = HttpSupport.json(created).get("id").asText();

		mockMvc.perform(get("/api/v1/bookings/" + bookingId).header("Authorization", "Bearer " + other))
				.andExpect(status().isNotFound());

		mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/cancel").header("Authorization", "Bearer " + other))
				.andExpect(status().isNotFound());
	}

	@Test
	void staffListsAllBookingsGuestCannot() throws Exception {
		HttpSupport http = new HttpSupport(mockMvc);
		String staff = http.staffToken();
		String guest = http.guestToken();
		String tableId = http.createTable(staff);
		String copyId = http.createCopy(staff, http.createGame(staff));

		mockMvc.perform(post("/api/v1/bookings")
						.header("Authorization", "Bearer " + guest)
						.header("Idempotency-Key", "list-key")
						.contentType(MediaType.APPLICATION_JSON)
						.content(HttpSupport.bookingBody(tableId, copyId)))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/bookings").header("Authorization", "Bearer " + guest))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/v1/bookings").header("Authorization", "Bearer " + staff))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)));
	}
}
