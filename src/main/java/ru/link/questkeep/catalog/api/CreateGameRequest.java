package ru.link.questkeep.catalog.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGameRequest(@NotBlank @Size(max = 200) String title) {
}
