package ru.link.questkeep.catalog.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.link.questkeep.AbstractPostgresTest;
import ru.link.questkeep.HttpSupport;

class CatalogApiTest extends AbstractPostgresTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void unauthenticatedCatalogIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/v1/tables")).andExpect(status().isUnauthorized());
	}

	@Test
	void guestCanListAndStaffCanPatchAndSoftDeleteEmptyTable() throws Exception {
		HttpSupport http = new HttpSupport(mockMvc);
		String staff = http.staffToken();
		String guest = http.guestToken();
		String tableId = http.createTable(staff);

		mockMvc.perform(get("/api/v1/tables").header("Authorization", "Bearer " + guest))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").isNumber());

		mockMvc.perform(patch("/api/v1/tables/" + tableId)
						.header("Authorization", "Bearer " + staff)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Renamed Oak","capacity":6}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Renamed Oak"))
				.andExpect(jsonPath("$.capacity").value(6));

		mockMvc.perform(delete("/api/v1/tables/" + tableId).header("Authorization", "Bearer " + staff))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/tables/" + tableId).header("Authorization", "Bearer " + guest))
				.andExpect(status().isNotFound());
	}
}
