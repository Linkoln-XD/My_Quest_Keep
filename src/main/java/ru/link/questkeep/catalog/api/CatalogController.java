package ru.link.questkeep.catalog.api;

import jakarta.validation.Valid;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ru.link.questkeep.catalog.CatalogService;
import ru.link.questkeep.shared.api.PageRequests;
import ru.link.questkeep.shared.api.PageResponse;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {

	private final CatalogService catalog;

	public CatalogController(CatalogService catalog) {
		this.catalog = catalog;
	}

	@PostMapping("/tables")
	@ResponseStatus(HttpStatus.CREATED)
	public TableResponse createTable(@Valid @RequestBody CreateTableRequest request) {
		return TableResponse.from(catalog.createTable(request.name(), request.capacity()));
	}

	@GetMapping("/tables")
	public PageResponse<TableResponse> listTables(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return PageResponse.from(catalog.listTables(PageRequests.of(page, size)).map(TableResponse::from));
	}

	@GetMapping("/tables/{id}")
	public TableResponse getTable(@PathVariable UUID id) {
		return TableResponse.from(catalog.getTable(id));
	}

	@PatchMapping("/tables/{id}")
	public TableResponse updateTable(@PathVariable UUID id, @Valid @RequestBody CreateTableRequest request) {
		return TableResponse.from(catalog.updateTable(id, request.name(), request.capacity()));
	}

	@DeleteMapping("/tables/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteTable(@PathVariable UUID id) {
		catalog.deleteTable(id);
	}

	@PostMapping("/games")
	@ResponseStatus(HttpStatus.CREATED)
	public GameResponse createGame(@Valid @RequestBody CreateGameRequest request) {
		return GameResponse.from(catalog.createGame(request.title()));
	}

	@GetMapping("/games")
	public PageResponse<GameResponse> listGames(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return PageResponse.from(catalog.listGames(PageRequests.of(page, size)).map(GameResponse::from));
	}

	@GetMapping("/games/{id}")
	public GameResponse getGame(@PathVariable UUID id) {
		return GameResponse.from(catalog.getGame(id));
	}

	@PatchMapping("/games/{id}")
	public GameResponse updateGame(@PathVariable UUID id, @Valid @RequestBody CreateGameRequest request) {
		return GameResponse.from(catalog.updateGame(id, request.title()));
	}

	@DeleteMapping("/games/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteGame(@PathVariable UUID id) {
		catalog.deleteGame(id);
	}

	@PostMapping("/games/{gameId}/copies")
	@ResponseStatus(HttpStatus.CREATED)
	public GameCopyResponse createCopy(@PathVariable UUID gameId) {
		return GameCopyResponse.from(catalog.createCopy(gameId));
	}

	@GetMapping("/games/{gameId}/copies")
	public PageResponse<GameCopyResponse> listCopies(
			@PathVariable UUID gameId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return PageResponse.from(catalog.listCopies(gameId, PageRequests.of(page, size)).map(GameCopyResponse::from));
	}

	@GetMapping("/game-copies/{id}")
	public GameCopyResponse getCopy(@PathVariable UUID id) {
		return GameCopyResponse.from(catalog.getCopy(id));
	}

	@DeleteMapping("/game-copies/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteCopy(@PathVariable UUID id) {
		catalog.deleteCopy(id);
	}
}
