package ru.link.questkeep.catalog.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTableRequest(
		@NotBlank @Size(max = 120) String name,
		@Min(2) @Max(8) int capacity) {
}
